package com.tpi.paseadores.user.service;

import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.domain.WalkerZone;
import com.tpi.paseadores.user.dto.WalkerCoverageRequest;
import com.tpi.paseadores.user.dto.WalkerCoverageResponse;
import com.tpi.paseadores.user.dto.WalkerPublicProfileResponse;
import com.tpi.paseadores.user.repository.UserRepository;
import com.tpi.paseadores.user.repository.WalkerZoneRepository;
import com.tpi.paseadores.zone.dto.ZoneResponse;
import com.tpi.paseadores.zone.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// public API of the walker coverage feature (RN-04/RN-08): default price/capacity and service zones
@Service
@RequiredArgsConstructor
public class WalkerCoverageService {
    private final UserRepository userRepository;
    private final WalkerZoneRepository walkerZoneRepository;
    private final ZoneService zoneService;

    @Transactional(readOnly = true)
    public WalkerCoverageResponse getOwnCoverage(UUID walkerId) {
        User walker = activeWalker(walkerId);
        return new WalkerCoverageResponse(walker.getDefaultPrice(), walker.getDefaultCapacity(), zonesOf(walkerId));
    }

    // RN-08: solo un paseador edita su propia cobertura; reemplaza el conjunto completo de zonas
    @Transactional
    public WalkerCoverageResponse updateOwnCoverage(UUID walkerId, WalkerCoverageRequest request) {
        User walker = activeWalker(walkerId);
        for (UUID zoneId : request.zoneIds()) {
            if (!zoneService.existsById(zoneId)) {
                throw new ResourceNotFoundException("Zone not found");
            }
        }

        walker.setDefaultPrice(request.defaultPrice());
        walker.setDefaultCapacity(request.defaultCapacity());

        walkerZoneRepository.deleteAllByWalkerId(walkerId);
        walkerZoneRepository.saveAll(request.zoneIds().stream().map(zoneId -> new WalkerZone(walkerId, zoneId)).toList());

        return new WalkerCoverageResponse(walker.getDefaultPrice(), walker.getDefaultCapacity(), zonesOf(walkerId));
    }

    // usado por walk al publicar (RN-13): la zona del paseo debe estar entre las zonas de servicio
    @Transactional(readOnly = true)
    public boolean coversZone(UUID walkerId, UUID zoneId) {
        activeWalker(walkerId);
        return walkerZoneRepository.findAllByWalkerId(walkerId).stream()
                .anyMatch(wz -> wz.getZoneId().equals(zoneId));
    }

    // consulta pública, sin datos de contacto: cualquiera puede ver el perfil de un paseador activo
    @Transactional(readOnly = true)
    public WalkerPublicProfileResponse getPublicProfile(UUID walkerId) {
        User walker = userRepository.findById(walkerId)
                .filter(u -> u.isActive() && u.getRole() == UserRole.WALKER)
                .orElseThrow(() -> new ResourceNotFoundException("Walker not found"));
        return new WalkerPublicProfileResponse(walker.getId(), walker.getFullName(), walker.getDefaultPrice(),
                walker.getDefaultCapacity(), walker.getRatingAvg(), walker.getReviewCount(), zonesOf(walkerId));
    }

    private User activeWalker(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isActive() || user.getRole() != UserRole.WALKER) {
            throw new ForbiddenActionException("Only walkers can manage service coverage");
        }
        return user;
    }

    private List<ZoneResponse> zonesOf(UUID walkerId) {
        List<UUID> zoneIds = walkerZoneRepository.findAllByWalkerId(walkerId).stream().map(WalkerZone::getZoneId).toList();
        return zoneService.findAllById(zoneIds);
    }
}
