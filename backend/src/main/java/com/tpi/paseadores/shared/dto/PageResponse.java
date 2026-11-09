package com.tpi.paseadores.shared.dto;

import java.util.List;

// forma compartida de toda respuesta paginada de la API: búsqueda, agenda, mis reservas, etc.
public record PageResponse<T>(List<T> items, int page, int size, boolean hasNext) {
}
