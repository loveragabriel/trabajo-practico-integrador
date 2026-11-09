package com.tpi.paseadores.walk.dto;

import com.tpi.paseadores.walk.domain.WalkStatus;
import jakarta.validation.constraints.NotNull;

public record WalkStatusUpdateRequest(@NotNull WalkStatus status) {
}
