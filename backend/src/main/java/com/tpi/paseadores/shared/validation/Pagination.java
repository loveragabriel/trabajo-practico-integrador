package com.tpi.paseadores.shared.validation;

import com.tpi.paseadores.shared.exception.InvalidFieldException;

public final class Pagination {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "12";
    public static final int MAX_SIZE = 50;

    private Pagination() {
        // utility class
    }

    public static void validate(int page, int size) {
        if (page < 0) {
            throw new InvalidFieldException("page", "La página no puede ser negativa.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidFieldException("size", "El tamaño de página debe estar entre 1 y " + MAX_SIZE + ".");
        }
    }
}
