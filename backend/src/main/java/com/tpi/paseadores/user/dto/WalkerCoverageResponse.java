package com.tpi.paseadores.user.dto;

import com.tpi.paseadores.zone.dto.ZoneResponse;

import java.math.BigDecimal;
import java.util.List;

public record WalkerCoverageResponse(BigDecimal defaultPrice, Integer defaultCapacity, List<ZoneResponse> zones) {
}
