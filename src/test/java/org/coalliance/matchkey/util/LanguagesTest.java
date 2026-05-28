/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import static org.coalliance.matchkey.util.Languages.getLanguage;
import static org.coalliance.matchkey.util.Languages.isNonRoman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguagesTest {

    // 008 positions 35-37 hold the language; total length must be 40 chars.
    private static final String ENG_008 = "150101s2014    xxu                 eng d";
    private static final String CHI_008 = "150101s2014    xxu                 chi d";
    private static final String BLANK_008 = "150101s2014    xxu                     d";

    private final MarcFactory marcFactory = MarcFactory.newInstance();

    @Test
    @DisplayName("getLanguage reads 008 positions 35-37")
    void from008() {
        Record r = marcFactory.newRecord();
        r.addVariableField(marcFactory.newControlField("008", ENG_008));
        assertEquals("eng", getLanguage(r));
    }

    @Test
    @DisplayName("falls back to 041$a when 008 language is blank")
    void fallbackTo041a() {
        Record r = marcFactory.newRecord();
        r.addVariableField(marcFactory.newControlField("008", BLANK_008));
        DataField f041 = marcFactory.newDataField("041", ' ', ' ');
        f041.addSubfield(marcFactory.newSubfield('a', "fre"));
        r.addVariableField(f041);
        assertEquals("fre", getLanguage(r));
    }

    @Test
    @DisplayName("returns null when neither source provides a language")
    void nullWhenNoneAvailable() {
        Record r = marcFactory.newRecord();
        assertNull(getLanguage(r));
    }

    @Test
    @DisplayName("isNonRoman recognises the consortium's set")
    void nonRomanMembership() {
        assertTrue(isNonRoman("chi"));
        assertTrue(isNonRoman("rus"));
        assertTrue(isNonRoman("ara"));
        assertFalse(isNonRoman("eng"));
        assertFalse(isNonRoman("fre"));
        assertFalse(isNonRoman(null));
    }
}
