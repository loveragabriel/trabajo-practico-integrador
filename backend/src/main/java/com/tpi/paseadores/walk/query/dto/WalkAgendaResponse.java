package com.tpi.paseadores.walk.query.dto;

import java.util.List;

public record WalkAgendaResponse(List<WalkReadResponse> items, int page, int size, boolean hasNext) {}
