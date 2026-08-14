/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.coalliance.matchkey.util.Padding.padWithUnderscores;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaddingTest {

    @Test
    @DisplayName("shorter input is right-padded with underscores")
    void padShort() {
        assertEquals("abc_______", padWithUnderscores("abc", 10));
    }

    @Test
    @DisplayName("longer input is truncated to the target length")
    void truncateLong() {
        assertEquals("abcde", padWithUnderscores("abcdefghij", 5));
    }

    @Test
    @DisplayName("exact-length input is returned unchanged")
    void exactLength() {
        assertEquals("abcde", padWithUnderscores("abcde", 5));
    }

    @Test
    @DisplayName("internal runs of spaces collapse to a single underscore")
    void collapseSpaces() {
        assertEquals("a_b_c_____", padWithUnderscores("a   b c", 10));
    }

    @Test
    @DisplayName("empty input becomes pure underscores")
    void emptyInput() {
        assertEquals("____", padWithUnderscores("", 4));
    }

    // 08-14-26 (_v08142026) Lowercasing moved into the padder so widths are
    // measured on the final characters. See the class javadoc for why İ is the
    // only character this changes.

    @Test
    @DisplayName("input is lowercased before it is measured")
    void lowercasesInput() {
        assertEquals("abc_______", padWithUnderscores("ABC", 10));
    }

    @Test
    @DisplayName("Turkish İ expands to two characters inside the width, not past it")
    void turkishCapitalIStaysInsideWidth() {
        // "İ".toLowerCase() is "i" + U+0307. Padding to 5 used to yield a 6-char
        // result once the key-wide lowercase ran.
        assertEquals(5, padWithUnderscores("İstanbul", 5).length());
        assertEquals("i̇sta", padWithUnderscores("İstanbul", 5));
        assertEquals(5, padWithUnderscores("İ", 5).length());
        assertEquals("i̇___", padWithUnderscores("İ", 5));
    }

    @Test
    @DisplayName("padded output is already lowercase, so a later lowercase cannot resize it")
    void outputIsStableUnderFurtherLowercasing() {
        for (String input : new String[] { "İstanbul", "İİ", "ABC", "Straße", "İ Press" }) {
            String padded = padWithUnderscores(input, 5);
            assertEquals(padded, padded.toLowerCase(java.util.Locale.ROOT));
            assertEquals(5, padded.length());
        }
    }
}
