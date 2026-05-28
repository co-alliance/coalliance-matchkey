/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import java.text.Normalizer;

/**
 * Unicode normalisation helpers used by the matchKey algorithm.
 *
 * <p>{@link #normalize(String)} produces the NFD-decomposed form so accented
 * characters become base-letter + combining-mark pairs.
 *
 * <p>{@link #removeAccents(String)} additionally strips the combining marks,
 * collapsing e.g. {@code "Müller"} to {@code "Muller"} and {@code "Pérez"} to
 * {@code "Perez"}.
 *
 * <p>See {@code docs/CoAlliance_Match_Key.md} for the full specification
 * (spec names: {@code normalizeString} and {@code normalizeStringAndRemoveAccents}).
 */
public final class AccentNormalizer {

    private AccentNormalizer() {}

    /** Decomposes accented characters into base letter + combining mark (NFD). */
    public static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFD);
    }

    /** NFD-normalises and removes all combining diacritical marks. */
    public static String removeAccents(String text) {
        return text == null
                ? ""
                : Normalizer.normalize(text, Normalizer.Form.NFD)
                            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
