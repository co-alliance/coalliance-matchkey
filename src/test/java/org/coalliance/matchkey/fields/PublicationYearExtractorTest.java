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

    // 08-14-26 (_v08142026) The gov-doc short-circuit is conditional on date1
    // being usable. Reported by Ed Summers (Stanford/POD): the raw return skipped
    // the checks both sibling paths apply, so a gov doc could key on a 9999
    // placeholder or on a 2- or 3-character year. The four tests below pin both
    // halves of the fix — that a usable date1 still wins, and that an unusable
    // one now falls through instead of being emitted.

    @Test
    @DisplayName("gov doc with date1 == 9999 falls through to date2 (was: emitted 9999)")
    void govDocNineNineFallsThroughToDate2() {
        // The bug's signature case: identical records, one with an 086, used to
        // disagree. 9999 is the unknown-year placeholder both sibling paths reject.
        Record withGovDoc = withControlField("008", "150101m99992016xxu           eng d");
        addSubfield(withGovDoc, "086", 'a', "ED 1.310/2:516193");
        assertEquals("2016", extractor.extract(withGovDoc));

        Record withoutGovDoc = withControlField("008", "150101m99992016xxu           eng d");
        assertEquals(extractor.extract(withoutGovDoc), extractor.extract(withGovDoc));
    }

    @Test
    @DisplayName("gov doc with a 3-character date1 falls through instead of shortening the key")
    void govDocShortDate1FallsThrough() {
        Record r = withControlField("008", "150101m987 2016xxu           eng d");
        addSubfield(r, "086", 'a', "ED 1.310/2:516193");
        assertEquals("2016", extractor.extract(r));
    }

    @Test
    @DisplayName("gov doc with date1 below MIN_YEAR (1200) falls through to date2")
    void govDocBelowMinYearFallsThrough() {
        Record r = withControlField("008", "150101m05002016xxu           eng d");
        addSubfield(r, "086", 'a', "ED 1.310/2:516193");
        assertEquals("2016", extractor.extract(r));
    }

    @Test
    @DisplayName("gov doc with no usable 008 date at all still reaches 264$c")
    void govDocUnusable008ReachesField264c() {
        Record r = withControlField("008", "150101m987 9999xxu           eng d");
        addSubfield(r, "086", 'a', "ED 1.310/2:516193");
        addSubfield(r, "264", 'c', "[1988]");
        assertEquals("1988", extractor.extract(r));
    }

    @Test
    @DisplayName("every path returns exactly 4 characters")
    void alwaysFourCharacters() {
        Record govDocShort = withControlField("008", "150101m987 987 xxu           eng d");
        addSubfield(govDocShort, "086", 'a', "ED 1.310/2:516193");
        assertEquals(4, extractor.extract(govDocShort).length());
        assertEquals("0000", extractor.extract(govDocShort));

        assertEquals(4, extractor.extract(withControlField("008", "tooshort")).length());
        assertEquals(4, extractor.extract(
                withControlField("008", "150101m20142016xxu           eng d")).length());
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
