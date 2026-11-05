package com.tpi.paseadores.walk.query;

import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.user.UserService;
import com.tpi.paseadores.walk.WalkStatus;
import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WalkReadService {
    private final WalkReadRepository repository;
    private final UserService users;
    private final Clock clock;

    public WalkReadService(WalkReadRepository repository, UserService users, Clock clock) {
        this.repository = repository;
        this.users = users;
        this.clock = clock;
    }

    public WalkAgendaResponse agenda(UUID actorId, WalkAgendaPeriod period, int page, int size) {
        User actor = activeUser(actorId);
        if (actor.getRole() != UserRole.WALKER) {
            throw new ForbiddenActionException("La agenda está disponible para paseadores activos.");
        }
        if (page < 0) throw new InvalidFieldException("page", "La página no puede ser negativa.");
        if (size < 1 || size > 50) throw new InvalidFieldException("size", "El tamaño de página debe estar entre 1 y 50.");
        var rows = repository.findAgenda(actorId, period, LocalDateTime.now(clock), page, size);
        return new WalkAgendaResponse(List.copyOf(rows.subList(0, Math.min(rows.size(), size))),
                page, size, rows.size() > size);
    }

    public WalkReadResponse detail(UUID actorId, UUID walkId) {
        User actor = activeUser(actorId);
        var walk = repository.findVisibleDetail(walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
        boolean offered = (walk.status() == WalkStatus.PUBLISHED || walk.status() == WalkStatus.FULL)
                && walk.startTime().isAfter(LocalDateTime.now(clock));
        boolean own = actor.getRole() == UserRole.WALKER && walk.walker().id().equals(actorId);
        if (offered || own) return walk;
        if (actor.getRole() == UserRole.CLIENT && repository.hasJoinedBooking(walkId, actorId)) return walk;
        throw new ResourceNotFoundException("Paseo no encontrado.");
    }

    private User activeUser(UUID id) {
        return users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ForbiddenActionException("Necesitás una cuenta activa para consultar los paseos."));
    }
}
