/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

/**
 * Pads or truncates a string to a fixed length with trailing underscores,
 * collapsing any runs of internal spaces to a single underscore first.
 *
 * <p>Spec name: {@code padWithUnderscores}. See {@code docs/CoAlliance_Match_Key.md}.
 */
public final class Padding {

    private Padding() {}

    /**
     * Returns {@code input} padded with trailing underscores or truncated so the
     * result has exactly {@code desiredLength} characters. Runs of spaces inside
     * {@code input} are collapsed to a single underscore before measuring.
     */
    public static String padWithUnderscores(String input, int desiredLength) {
        String squeezed = input.replaceAll(" +", "_");
        if (squeezed.length() >= desiredLength) {
            return squeezed.substring(0, desiredLength);
        }
        return squeezed + "_".repeat(desiredLength - squeezed.length());
    }
}
