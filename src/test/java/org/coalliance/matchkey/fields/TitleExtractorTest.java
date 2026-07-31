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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleExtractorTest {

    private final TitleExtractor extractor = new TitleExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    // 07-31-26 (_v07312026) A missing 245 pads to 95 underscores like every other
    // empty section. Through _v03182026 it emitted a zero-width section.
    private static final String EMPTY = "_".repeat(95);
    private static final String CHI_008 = "150101s2014    xxu                 chi d";
    private static final String ENG_008 = "150101s2014    xxu                 eng d";

    private static String repeatUnderscores(int n) { return "_".repeat(n); }

    private static String padded(String s) {
        return s + repeatUnderscores(95 - s.length());
    }

    private Record withTitle(String... subfieldPairs) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        for (int i = 0; i < subfieldPairs.length; i += 2) {
            f245.addSubfield(marcFactory.newSubfield(
                    subfieldPairs[i].charAt(0), subfieldPairs[i + 1]));
        }
        r.addVariableField(f245);
        return r;
    }

    private void addSubfield(Record r, String tag, char code, String value) {
        DataField existing = (DataField) r.getVariableField(tag);
        if (existing != null) {
            existing.addSubfield(marcFactory.newSubfield(code, value));
        } else {
            DataField f = marcFactory.newDataField(tag, ' ', ' ');
            f.addSubfield(marcFactory.newSubfield(code, value));
            r.addVariableField(f);
        }
    }

    private void addField(Record r, String tag, String... subfieldPairs) {
        DataField f = marcFactory.newDataField(tag, ' ', ' ');
        for (int i = 0; i < subfieldPairs.length; i += 2) {
            f.addSubfield(marcFactory.newSubfield(
                    subfieldPairs[i].charAt(0), subfieldPairs[i + 1]));
        }
        r.addVariableField(f);
    }

    @Test
    @DisplayName("missing 245 pads to 95 underscores (_v07312026; was zero-width through _v03182026)")
    void noTitle() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(EMPTY, extractor.extract(r));
        assertEquals(95, extractor.extract(r).length());
    }

    @Test
    @DisplayName("a 245 present but with no $a$b$p pads to 95, same as an absent 245")
    void emptySubfieldsMatchAbsent245() {
        // Regression for the inconsistency that motivated the _v07312026 change:
        // a present-but-empty 245 was already padded, while an ABSENT 245 was not.
        assertEquals(EMPTY, extractor.extract(withTitle("a", "")));
    }

    @Test
    @DisplayName("simple 245$a is space-stripped, lowercased, and padded")
    void simpleTitle() {
        assertEquals(padded("simpletitle"),
                extractor.extract(withTitle("a", "Simple Title")));
    }

    @Test
    @DisplayName("245$a and $b are concatenated with no separator")
    void concatenateAandB() {
        assertEquals(padded("helloworld"),
                extractor.extract(withTitle("a", "Hello ", "b", "World")));
    }

    @Test
    @DisplayName("leading 'The ' article is stripped")
    void leadingArticleStripped() {
        assertEquals(padded("historyofrome"),
                extractor.extract(withTitle("a", "The History of Rome")));
    }

    @Test
    @DisplayName("non-Roman language (chi) with no 880 falls back to LCC (010$a)")
    void nonRomanFallbackToLcc() {
        Record r = withTitle("a", "Some Chinese-looking title");
        r.addVariableField(marcFactory.newControlField("008", CHI_008));
        addField(r, "010", "a", "LCC123 ");
        // LCC has space removed, padded, lowercased
        assertEquals(padded("lcc123"), extractor.extract(r));
    }

    @Test
    @DisplayName("non-Roman language with no 880, no LCC falls back to first valid ISBN")
    void nonRomanFallbackToIsbn() {
        Record r = withTitle("a", "Title");
        r.addVariableField(marcFactory.newControlField("008", CHI_008));
        addField(r, "020", "a", "9780123456789 (pbk.)");
        assertEquals(padded("9780123456789"), extractor.extract(r));
    }

    @Test
    @DisplayName("non-Roman language with no 880/LCC/ISBN falls back to ISSN (022$a)")
    void nonRomanFallbackToIssn() {
        Record r = withTitle("a", "Title");
        r.addVariableField(marcFactory.newControlField("008", CHI_008));
        addField(r, "022", "a", "1234-5678");
        assertEquals(padded("1234-5678"), extractor.extract(r));
    }

    @Test
    @DisplayName("non-Roman language WITH 880$a present skips the identifier fallback")
    void nonRomanWith880UsesNormalPath() {
        // chi + 880$a present + 245$6 linking → use 880 title
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        r.addVariableField(marcFactory.newControlField("008", CHI_008));
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('6', "880-01"));
        f245.addSubfield(marcFactory.newSubfield('a', "Romanized Title"));
        r.addVariableField(f245);
        DataField f880 = marcFactory.newDataField("880", '1', '0');
        f880.addSubfield(marcFactory.newSubfield('6', "245-01"));
        f880.addSubfield(marcFactory.newSubfield('a', "VernacularTitle"));
        r.addVariableField(f880);
        assertEquals(padded("vernaculartitle"), extractor.extract(r));
    }

    @Test
    @DisplayName("245$6 880 link is followed even for Roman-language records")
    void romanLinkedTo880() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        r.addVariableField(marcFactory.newControlField("008", ENG_008));
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('6', "880-01"));
        f245.addSubfield(marcFactory.newSubfield('a', "Romanized"));
        r.addVariableField(f245);
        DataField f880 = marcFactory.newDataField("880", '1', '0');
        f880.addSubfield(marcFactory.newSubfield('6', "245-01"));
        f880.addSubfield(marcFactory.newSubfield('a', "FromLinked880"));
        r.addVariableField(f880);
        assertEquals(padded("fromlinked880"), extractor.extract(r));
    }

    @Test
    @DisplayName("output is always exactly 95 characters")
    void alwaysExactWidth() {
        String result = extractor.extract(withTitle("a",
                "A very long title that should be truncated to ninety-five characters at the end of processing"));
        assertEquals(95, result.length());
    }

    @Test
    @DisplayName("output is always lowercased")
    void alwaysLowercase() {
        String result = extractor.extract(withTitle("a", "MIXED Case TITLE"));
        assertEquals(result.toLowerCase(), result);
    }

    @Test
    @DisplayName("punctuation in title becomes a word break and then disappears")
    void punctuationCollapses() {
        // "Hello, World!" -> stripPuncuationSpace -> "Hello  World " -> remove
        // spaces -> "HelloWorld" -> lowercase -> "helloworld"
        assertEquals(padded("helloworld"),
                extractor.extract(withTitle("a", "Hello, World!")));
    }

    @Test
    @DisplayName("ACLS algorithm-doc example: full title section is reproduced")
    void aclsDocAnchorExample() {
        Record r = withTitle("a",
                "American Council of Learned Societies Annual Report for the Years 2006-2007 and 2005-2006");
        String result = extractor.extract(r);
        assertTrue(result.startsWith("americancounciloflearnedsocietiesannualreportfortheyears20062007and20052006"),
                "Got: " + result);
        assertEquals(95, result.length());
    }
}
