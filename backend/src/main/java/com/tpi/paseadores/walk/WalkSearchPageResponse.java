package com.tpi.paseadores.walk;

import java.util.List;

public record WalkSearchPageResponse(List<WalkSearchResponse> items, int page, int size, boolean hasNext) {}
