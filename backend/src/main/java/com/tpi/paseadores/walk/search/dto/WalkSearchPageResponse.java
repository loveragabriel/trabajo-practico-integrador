package com.tpi.paseadores.walk.search.dto;

import java.util.List;

public record WalkSearchPageResponse(List<WalkSearchResponse> items, int page, int size, boolean hasNext) {}
