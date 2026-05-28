/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.coalliance.matchkey.util.AccentNormalizer.removeAccents;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccentNormalizerTest {

    @Test
    @DisplayName("null input returns empty string")
    void nullInput() {
        assertEquals("", removeAccents(null));
    }

    @Test
    @DisplayName("plain ASCII passes through unchanged")
    void plainAscii() {
        assertEquals("Smith", removeAccents("Smith"));
    }

    @Test
    @DisplayName("acute accents are removed")
    void acuteAccent() {
        assertEquals("Perez", removeAccents("Pérez"));
    }

    @Test
    @DisplayName("umlauts are removed")
    void umlaut() {
        assertEquals("Muller", removeAccents("Müller"));
    }

    @Test
    @DisplayName("mixed diacritics across a longer string")
    void mixedDiacritics() {
        assertEquals("Cafe au lait, naive jalapeno",
                removeAccents("Café au lait, naïve jalapeño"));
    }
}
