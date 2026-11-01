package com.tpi.paseadores.shared.validation;

public class StringUtils {
    private StringUtils() {
        // utility class
    }

    /**
     * Converts blank strings (null or whitespace-only) to null, otherwise trims and returns.
     */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
