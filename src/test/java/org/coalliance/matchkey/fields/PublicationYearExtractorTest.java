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

class PublicationYearExtractorTest {

    private final PublicationYearExtractor extractor = new PublicationYearExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private Record withControlField(String tag, String data) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        r.addVariableField(marcFactory.newControlField(tag, data));
        return r;
    }

    private void addSubfield(Record r, String tag, char code, String value) {
        DataField f = marcFactory.newDataField(tag, ' ', ' ');
        f.addSubfield(marcFactory.newSubfield(code, value));
        r.addVariableField(f);
    }

    @Test
    @DisplayName("008 with type 's' (single date): date1 wins when date2 is invalid (ACLS doc example)")
    void singleDate() {
        // From the algorithm-doc ACLS example: 008 = "080101s2008 xxu|||| st ||| ||eng d"
        Record r = withControlField("008", "080101s2008 xxu|||| st ||| ||eng d");
        assertEquals("2008", extractor.extract(r));
    }

    @Test
    @DisplayName("008 with type 'm' (multiple dates): date2 wins when valid")
    void multipleDatesDate2Wins() {
        // chars 7-10 = "2014", chars 11-14 = "2016"
        Record r = withControlField("008", "150101m20142016xxu           eng d");
        assertEquals("2016", extractor.extract(r));
    }

    @Test
    @DisplayName("008 with type 'r' (reissue): always date1 even if date2 looks valid")
    void reissueUsesDate1() {
        // chars 7-10 = "2010" (reissue date), chars 11-14 = "1985" (original)
        Record r = withControlField("008", "150101r20101985xxu           eng d");
        assertEquals("2010", extractor.extract(r));
    }

    @Test
    @DisplayName("government document (086$a present) uses date1 regardless of date2")
    void govDocUsesDate1() {
        Record r = withControlField("008", "150101m20142016xxu           eng d");
        addSubfield(r, "086", 'a', "ED 1.310/2:516193");
        assertEquals("2014", extractor.extract(r));
    }

    @Test
    @DisplayName("date2 == 9999 falls through to date1")
    void date2NineNine() {
        Record r = withControlField("008", "150101m20149999xxu           eng d");
        assertEquals("2014", extractor.extract(r));
    }

    @Test
    @DisplayName("date2 below MIN_YEAR (1200) falls through to date1")
    void date2BelowMinYear() {
        Record r = withControlField("008", "150101m20140500xxu           eng d");
        assertEquals("2014", extractor.extract(r));
    }

    @Test
    @DisplayName("no 008: 264$c '[1988]' yields 1988")
    void field264cWithBrackets() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        addSubfield(r, "264", 'c', "[1988]");
        assertEquals("1988", extractor.extract(r));
    }

    @Test
    @DisplayName("no 008: 264$c 'c1955 [1956]' — c-prefixed year wins over rightmost")
    void field264cCopyrightWins() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        addSubfield(r, "264", 'c', "c1955 [1956]");
        assertEquals("1955", extractor.extract(r));
    }

    @Test
    @DisplayName("no 008 or 264: 260$c '1972' is found")
    void field260cFallback() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        addSubfield(r, "260", 'c', "1972");
        assertEquals("1972", extractor.extract(r));
    }

    @Test
    @DisplayName("no date anywhere returns '0000'")
    void nothingValidReturns0000() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals("0000", extractor.extract(r));
    }

    @Test
    @DisplayName("008 shorter than 15 chars is skipped without throwing")
    void short008Skipped() {
        Record r = withControlField("008", "tooshort");
        addSubfield(r, "260", 'c', "1999");
        assertEquals("1999", extractor.extract(r));
    }
}
