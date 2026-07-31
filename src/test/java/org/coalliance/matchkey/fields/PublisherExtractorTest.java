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

class PublisherExtractorTest {

    private final PublisherExtractor extractor = new PublisherExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    // 07-31-26 (_v07312026) An empty publisher pads to 5 underscores like every
    // other empty section. Through _v03182026 it emitted a zero-width section.
    // See PublisherExtractor.cleanup().
    private static final String EMPTY = "_____";

    private Record withSubfield(String tag, char code, String value) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f = marcFactory.newDataField(tag, ' ', ' ');
        f.addSubfield(marcFactory.newSubfield(code, value));
        r.addVariableField(f);
        return r;
    }

    @Test
    @DisplayName("no 264 or 260 pads to 5 underscores (_v07312026; was zero-width through _v03182026)")
    void noPublisherField() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(EMPTY, extractor.extract(r));
        assertEquals(5, extractor.extract(r).length());
    }

    @Test
    @DisplayName("a publisher of '&' cleans to nothing and pads to 5, same as an absent publisher")
    void ampersandOnlyPublisherMatchesAbsentPublisher() {
        // Regression for the inconsistency that motivated the _v07312026 change:
        // "&" was stripped to "" and then padded, while an ABSENT publisher was
        // not padded at all. Both now produce the same 5-underscore section.
        assertEquals(EMPTY, extractor.extract(withSubfield("264", 'b', "&")));
    }

    @Test
    @DisplayName("264$b 'Distributed by ERIC Clearinghouse' matches the algorithm-doc example 'distr'")
    void distributedByEric() {
        // matches the ACLS example in docs/CoAlliance_Match_Key.md
        assertEquals("distr",
                extractor.extract(withSubfield("264", 'b', "Distributed by ERIC Clearinghouse")));
    }

    @Test
    @DisplayName("falls back to 260$b when 264$b is absent")
    void fallsBackToField260() {
        assertEquals("rando",
                extractor.extract(withSubfield("260", 'b', "Random House")));
    }

    @Test
    @DisplayName("trailing comma on subfield is stripped before processing")
    void trailingCommaStripped() {
        assertEquals("rando",
                extractor.extract(withSubfield("264", 'b', "Random House,")));
    }

    @Test
    @DisplayName("ampersand is removed (not expanded to 'and') so it does not pollute the truncation")
    void ampersandRemoved() {
        // "ACME & Co." -> remove & -> "ACME  Co." -> strip puncuation/spaces ->
        // "ACME_Co_" -> remove underscores -> "ACMECo" -> truncate to 5 -> "ACMEC" -> lower
        assertEquals("acmec",
                extractor.extract(withSubfield("264", 'b', "ACME & Co.")));
    }

    @Test
    @DisplayName("short publisher is padded with underscores")
    void shortName() {
        // "BBC" -> "BBC" -> pad to 5 -> "BBC__" -> lower -> "bbc__"
        assertEquals("bbc__", extractor.extract(withSubfield("264", 'b', "BBC")));
    }

    @Test
    @DisplayName("output is always lowercased")
    void alwaysLowercase() {
        assertEquals("oxfor",
                extractor.extract(withSubfield("264", 'b', "OXFORD UNIVERSITY PRESS")));
    }

    @Test
    @DisplayName("264$b takes precedence over 260$b when both are present")
    void rdaPrecedence() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f264 = marcFactory.newDataField("264", ' ', ' ');
        f264.addSubfield(marcFactory.newSubfield('b', "Random House"));
        DataField f260 = marcFactory.newDataField("260", ' ', ' ');
        f260.addSubfield(marcFactory.newSubfield('b', "Penguin Books"));
        r.addVariableField(f264);
        r.addVariableField(f260);
        assertEquals("rando", extractor.extract(r));
    }
}
