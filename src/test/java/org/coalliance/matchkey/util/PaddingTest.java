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
}
