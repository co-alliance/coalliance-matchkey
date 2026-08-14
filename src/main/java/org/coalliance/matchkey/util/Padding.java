/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import java.util.Locale;

/**
 * Pads or truncates a string to a fixed length with trailing underscores,
 * collapsing any runs of internal spaces to a single underscore first and
 * lowercasing before the width is measured.
 *
 * <p>Spec name: {@code padWithUnderscores}. See {@code docs/CoAlliance_Match_Key.md}.
 */
public final class Padding {

    private Padding() {}

    /**
     * Returns {@code input} lowercased, then padded with trailing underscores or
     * truncated so the result has exactly {@code desiredLength} characters. Runs
     * of spaces inside {@code input} are collapsed to a single underscore before
     * measuring.
     *
     * <p>08-14-26 (_v08142026) The lowercasing moved here, ahead of the measure.
     * The finished matchKey is lowercased as a whole
     * ({@code MatchKeyGenerator.generate}), and Turkish capital İ (U+0130)
     * lowercases to TWO code points — {@code i} (U+0069) plus a combining dot
     * above (U+0307). Measuring the width before that expansion let one İ count
     * as a single character while padding and then occupy two afterwards, so the
     * section — in practice the 5-character publisher, which neither
     * NFD-normalises nor strips accents — overran its fixed width and pushed the
     * whole key past 188. İ is the only character in Unicode whose lowercase
     * mapping lengthens the string, so nothing else is affected: for every other
     * input, lowercasing before or after the pad gives the same result. Reported
     * by Ed Summers (Stanford/POD) — 10,575 records of 39.4M, all Turkish or
     * Ottoman.
     *
     * <p>{@link Locale#ROOT} is explicit because the default-locale overload does
     * the opposite damage on a Turkish-locale JVM, where {@code "I"} lowercases
     * to dotless {@code "ı"} — same width, different key, varying by machine.
     */
    public static String padWithUnderscores(String input, int desiredLength) {
        String squeezed = input.replaceAll(" +", "_").toLowerCase(Locale.ROOT);
        if (squeezed.length() >= desiredLength) {
            return squeezed.substring(0, desiredLength);
        }
        return squeezed + "_".repeat(desiredLength - squeezed.length());
    }
}
