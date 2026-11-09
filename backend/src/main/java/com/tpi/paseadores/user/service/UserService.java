package com.tpi.paseadores.user.service;

import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.validation.StringUtils;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.dto.UserResponse;
import com.tpi.paseadores.user.dto.UserUpdateRequest;
import com.tpi.paseadores.user.repository.UserRepository;
import com.tpi.paseadores.zone.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

// public API of the user feature: other features go through here, never through UserRepository
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ZoneService zoneService;

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User create(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // RN-02: el rol y el email nunca se editan desde el perfil; la zona es obligatoria para ambos roles
    @Transactional
    public UserResponse updateProfile(UUID userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!zoneService.existsById(request.zoneId())) {
            throw new ResourceNotFoundException("Zone not found");
        }

        user.setFullName(request.fullName().trim());
        user.setPhone(StringUtils.blankToNull(request.phone()));
        user.setZoneId(request.zoneId());

        return UserResponse.from(user);
    }

    // usado por review al calificar un paseo: suma una calificación al promedio del paseador,
    // bajo lock de fila para serializar reseñas concurrentes del mismo paseador
    @Transactional
    public void applyReviewRating(UUID walkerId, int rating) {
        User walker = userRepository.findByIdForUpdate(walkerId)
                .orElseThrow(() -> new ResourceNotFoundException("Walker not found"));
        int newCount = walker.getReviewCount() + 1;
        BigDecimal total = walker.getRatingAvg().multiply(BigDecimal.valueOf(walker.getReviewCount()))
                .add(BigDecimal.valueOf(rating));
        walker.setRatingAvg(total.divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP));
        walker.setReviewCount(newCount);
    }
}
