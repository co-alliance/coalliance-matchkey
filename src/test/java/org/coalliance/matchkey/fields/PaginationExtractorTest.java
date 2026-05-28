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

class PaginationExtractorTest {

    private final PaginationExtractor extractor = new PaginationExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private Record recordWith300a(String value) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f300 = marcFactory.newDataField("300", ' ', ' ');
        f300.addSubfield(marcFactory.newSubfield('a', value));
        r.addVariableField(f300);
        return r;
    }

    @Test
    @DisplayName("300$a with a 4-digit page count returns those 4 digits")
    void fourDigitPages() {
        assertEquals("1234", extractor.extract(recordWith300a("1234 pages")));
    }

    @Test
    @DisplayName("300$a with fewer than 4 contiguous digits returns underscores")
    void twoDigitPages() {
        assertEquals("____", extractor.extract(recordWith300a("66 p.")));
    }

    @Test
    @DisplayName("300$a with more than 4 contiguous digits returns the first 4")
    void sixDigitPages() {
        assertEquals("9876", extractor.extract(recordWith300a("987654 pages")));
    }

    @Test
    @DisplayName("missing 300 field returns underscores")
    void noField300() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals("____", extractor.extract(r));
    }

    @Test
    @DisplayName("300 field with no $a returns underscores")
    void no300a() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f300 = marcFactory.newDataField("300", ' ', ' ');
        f300.addSubfield(marcFactory.newSubfield('b', "ill."));
        r.addVariableField(f300);
        assertEquals("____", extractor.extract(r));
    }

    @Test
    @DisplayName("non-numeric extent (online resource) returns underscores")
    void onlineResource() {
        assertEquals("____", extractor.extract(recordWith300a("1 online resource")));
    }
}
