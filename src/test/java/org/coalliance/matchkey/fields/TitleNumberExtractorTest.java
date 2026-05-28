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

class TitleNumberExtractorTest {

    private final TitleNumberExtractor extractor = new TitleNumberExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private Record recordWith245n(String value) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Something"));
        f245.addSubfield(marcFactory.newSubfield('n', value));
        r.addVariableField(f245);
        return r;
    }

    @Test
    @DisplayName("245$n 'v. 1' produces a 10-char padded result")
    void shortNumber() {
        assertEquals("v_1_______", extractor.extract(recordWith245n("v. 1")));
    }

    @Test
    @DisplayName("missing 245 returns 10 underscores")
    void noTitle() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals("__________", extractor.extract(r));
    }

    @Test
    @DisplayName("245 present but no $n returns 10 underscores")
    void noSubfieldN() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Just a title"));
        r.addVariableField(f245);
        assertEquals("__________", extractor.extract(r));
    }

    @Test
    @DisplayName("longer 245$n is truncated to 10 characters")
    void longNumber() {
        assertEquals("v_123456_v",
                extractor.extract(recordWith245n("v. 123456, vol. 7")));
    }
}
