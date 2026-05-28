/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TitleDatesExtractorTest {

    private final TitleDatesExtractor extractor = new TitleDatesExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private static final String EMPTY = "_______________"; // 15 underscores

    private Record withTitleAndF(String fValue) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Some title"));
        f245.addSubfield(marcFactory.newSubfield('f', fValue));
        r.addVariableField(f245);
        return r;
    }

    @Test
    @DisplayName("missing 245 returns 15 underscores")
    void noTitle() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(EMPTY, extractor.extract(r));
    }

    @Test
    @DisplayName("245 without $f returns 15 underscores")
    void noSubfieldF() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Title"));
        r.addVariableField(f245);
        assertEquals(EMPTY, extractor.extract(r));
    }

    @Test
    @DisplayName("standard date range '2005-2007' is padded to 15")
    void dateRange() {
        assertEquals("2005_2007______", extractor.extract(withTitleAndF("2005-2007")));
    }

    @Test
    @DisplayName("longer date string is truncated to 15")
    void longDateString() {
        assertEquals("1900_1910_1920_",
                extractor.extract(withTitleAndF("1900-1910-1920-1930")));
    }

    @Test
    @DisplayName("trailing whitespace is stripped (no trailing underscore from it)")
    void trailingWhitespace() {
        assertEquals("2005_2007______", extractor.extract(withTitleAndF("2005-2007   ")));
    }

    @Test
    @DisplayName("leading whitespace becomes a leading underscore (indexer quirk preserved)")
    void leadingWhitespace() {
        assertEquals("_2005_2007_____", extractor.extract(withTitleAndF(" 2005-2007")));
    }
}
