/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

/**
 * Replaces punctuation characters in a string with a chosen replacement character
 * and strips leading articles ("a", "an", "the") and a small set of other
 * normalisations used by the matchKey algorithm.
 *
 * <p>The misspelling "puncuation" is preserved from the indexer source for
 * grep-parity across the consortium codebase.
 *
 * <p>See {@code docs/CoAlliance_Match_Key.md} for the full specification.
 */
public final class PuncuationStripper {

    private PuncuationStripper() {}

    private static final char UNDERSCORE = '_';
    private static final char SPACE      = ' ';
    private static final char ZERO_WIDTH_SPACE = '​';

    /** Characters replaced one-for-one with the chosen replacement character. */
    private static final String PUNCTUATION_CHARS =
            " !\"#$()*+,-./:;<=>?@[\\]^_`|~©";

    /** Convenience for the canonical "underscore-replaced" form. */
    public static String stripPuncuation(String input) {
        return stripPuncuation(input, UNDERSCORE);
    }

    /**
     * Variant that replaces punctuation with spaces instead of underscores.
     * Used by the title section so trailing punctuation can be trimmed cleanly.
     */
    public static String stripPuncuationSpace(String input) {
        return stripPuncuation(input, SPACE);
    }

    /**
     * Replaces punctuation in {@code input} with {@code replacement}. Returns ""
     * for null input. Also strips leading articles ("a", "an", "the"), normalises
     * "&amp;" to "and", removes apostrophes and braces, treats {@code %22} and
     * {@code %} as underscores, and collapses runs of the replacement character.
     */
    public static String stripPuncuation(String input, char replacement) {
        if (input == null) return "";
        if (input.isEmpty()) return input;

        input = input.replace("%22", "_");
        input = input.replace("%",   "_");

        input = input.replaceFirst("^[" + replacement + "]+", "");

        input = input.replaceFirst("^[aA] +",  "");
        input = input.replaceFirst("^[aA]n +", "");
        input = input.replaceFirst("^[tT]he +", "");

        input = input.replace("'", "");
        input = input.replace("{", "");
        input = input.replace("}", "");
        input = input.replace("&", "and");

        for (char c : PUNCTUATION_CHARS.toCharArray()) {
            input = input.replace(c, replacement);
        }
        input = input.replace(ZERO_WIDTH_SPACE, replacement);

        input = input.replaceAll(replacement + "+", String.valueOf(replacement));
        return input;
    }
}
