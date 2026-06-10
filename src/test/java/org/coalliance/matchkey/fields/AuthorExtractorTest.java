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

class AuthorExtractorTest {

    private final AuthorExtractor extractor = new AuthorExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private static final String EMPTY = "_____";

    private Record withField(String tag, String aValue) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f = marcFactory.newDataField(tag, ' ', ' ');
        f.addSubfield(marcFactory.newSubfield('a', aValue));
        r.addVariableField(f);
        return r;
    }

    private void addField(Record r, String tag, String aValue) {
        DataField f = marcFactory.newDataField(tag, ' ', ' ');
        f.addSubfield(marcFactory.newSubfield('a', aValue));
        r.addVariableField(f);
    }

    @Test
    @DisplayName("no 100/110/111/130 returns 5 underscores")
    void noAuthorField() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(EMPTY, extractor.extract(r));
    }

    @Test
    @DisplayName("100$a short name is padded to 5")
    void shortPersonalAuthor() {
        assertEquals("Smith", extractor.extract(withField("100", "Smith")));
    }

    @Test
    @DisplayName("100$a single character is padded to 5")
    void singleChar() {
        assertEquals("S____", extractor.extract(withField("100", "S")));
    }

    @Test
    @DisplayName("100$a long name is truncated to 5")
    void longName() {
        assertEquals("Smith", extractor.extract(withField("100", "Smithsonian Institution")));
    }

    @Test
    @DisplayName("110$a corporate name (with punctuation) cleans and pads")
    void corporateAuthor() {
        // "ACME Corp." -> stripPuncuation "ACME_Corp_" -> truncate to 5 -> "ACME_"
        assertEquals("ACME_", extractor.extract(withField("110", "ACME Corp.")));
    }

    @Test
    @DisplayName("diacritics in author name are removed")
    void accentedAuthor() {
        assertEquals("Perez", extractor.extract(withField("100", "Pérez")));
    }

    @Test
    @DisplayName("multiple author fields concatenate in 100/110/111/130 order")
    void multipleAuthorFields() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        addField(r, "100", "S");
        addField(r, "130", "Doe");
        // concat = "SDoe" -> pad to 5
        assertEquals("SDoe_", extractor.extract(r));
    }

    @Test
    @DisplayName("130 is read in place of legacy 113 (changed 2023-02-14)")
    void usesUniformTitle130NotLegacy113() {
        assertEquals("_____", extractor.extract(withField("113", "Some uniform")));
        assertEquals("Unifo", extractor.extract(withField("130", "Uniform title")));
    }

    @Test
    @DisplayName("leading bracket/punctuation is removed by the double clean (no leading underscore)")
    void leadingPunctuationStrippedByDoubleClean() {
        // "[Faidit, Hugues]," — the indexer cleans each value TWICE. Pass 1 turns the
        // leading "[" into "_"; pass 2 strips that now-leading "_". A single clean
        // would leave "_faid" and shift the 5-char author window, diverging from the
        // production index. Result here is "Faidi" (the extractor does not lowercase;
        // iiiMatchKey lowercases the whole key downstream).
        assertEquals("Faidi", extractor.extract(withField("100", "[Faidit, Hugues],")));
    }
}
