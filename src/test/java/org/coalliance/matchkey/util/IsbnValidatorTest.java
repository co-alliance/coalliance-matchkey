/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.coalliance.matchkey.util.IsbnValidator.returnValidISBNs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsbnValidatorTest {

    private static Set<String> set(String... items) {
        Set<String> s = new LinkedHashSet<>();
        for (String it : items) s.add(it);
        return s;
    }

    @Test
    @DisplayName("10-digit ISBN passes through (trailing text trimmed)")
    void tenDigit() {
        assertEquals(set("0123456789"), returnValidISBNs(set("0123456789 (pbk.)")));
    }

    @Test
    @DisplayName("10-digit ISBN ending in X is accepted")
    void tenDigitWithX() {
        assertEquals(set("123456789X"), returnValidISBNs(set("123456789X")));
    }

    @Test
    @DisplayName("13-digit ISBN starting with 978 is accepted")
    void thirteenDigit978() {
        assertEquals(set("9780123456789"), returnValidISBNs(set("9780123456789 (hbk.)")));
    }

    @Test
    @DisplayName("13-digit string not starting with 978/979 is rejected")
    void thirteenDigitNot978Or979Rejected() {
        assertTrue(returnValidISBNs(set("1234567890123")).isEmpty());
    }

    @Test
    @DisplayName("non-ISBN strings are filtered out")
    void nonIsbnFiltered() {
        assertTrue(returnValidISBNs(set("not an ISBN", "abc")).isEmpty());
    }

    @Test
    @DisplayName("multiple candidates: only valid ones are returned")
    void mixedCandidates() {
        Set<String> result = returnValidISBNs(set("0123456789", "abc", "9780123456789"));
        assertEquals(set("0123456789", "9780123456789"), result);
    }
}
