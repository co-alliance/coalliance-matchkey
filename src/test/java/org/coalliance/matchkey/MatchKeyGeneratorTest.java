/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchKeyGeneratorTest {

    private final MatchKeyGenerator generator = new MatchKeyGenerator(null);
    private final MarcFactory marcFactory = MarcFactory.newInstance();

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

    @Test
    @DisplayName("matchKey is always exactly 188 characters")
    void matchkeyWidth() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(188, generator.generate(r).length());
    }

    @Test
    @DisplayName("matchKey ends with the algorithm version + format char")
    void versionAndFormatAtEnd() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        String mk = generator.generate(r);
        // last 11 chars = version (10) + format (1)
        String tail = mk.substring(mk.length() - 11);
        assertTrue(tail.startsWith(MatchKeyVersion.VERSION), "Got tail: " + tail);
        assertTrue(tail.endsWith("p") || tail.endsWith("e"), "Got tail: " + tail);
    }

    @Test
    @DisplayName("matchKey is always lowercased")
    void alwaysLowercase() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "MIXED Case TITLE"));
        r.addVariableField(f245);
        String mk = generator.generate(r);
        assertEquals(mk.toLowerCase(), mk);
    }

    @Test
    @DisplayName("matchKey contains no spaces (post-processing replaces them with underscores)")
    void noSpaces() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(-1, generator.generate(r).indexOf(' '));
    }

    @Test
    @DisplayName("ACLS algorithm-doc anchor: full end-to-end matchKey is reproduced section by section")
    void aclsEndToEnd() {
        // Build the ACLS record from CoAlliance_Match_Key.md (section "Match Key Example")
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        r.addVariableField(marcFactory.newControlField("001", "991034738289702766"));
        r.addVariableField(marcFactory.newControlField("008", "080101s2008    xxu|||| st ||| ||eng d"));

        DataField f110 = marcFactory.newDataField("110", '2', ' ');
        f110.addSubfield(marcFactory.newSubfield('a', "American Council of Learned Societies."));
        r.addVariableField(f110);

        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a',
                "American Council of Learned Societies Annual Report for the Years 2006-2007 and 2005-200"));
        f245.addSubfield(marcFactory.newSubfield('b', "6"));
        f245.addSubfield(marcFactory.newSubfield('h', "[electronic resource]."));
        r.addVariableField(f245);

        DataField f260 = marcFactory.newDataField("260", ' ', ' ');
        f260.addSubfield(marcFactory.newSubfield('a', "[S.l.] :"));
        f260.addSubfield(marcFactory.newSubfield('b', "Distributed by ERIC Clearinghouse,"));
        f260.addSubfield(marcFactory.newSubfield('c', "2008."));
        r.addVariableField(f260);

        DataField f300 = marcFactory.newDataField("300", ' ', ' ');
        f300.addSubfield(marcFactory.newSubfield('a', "1 online resource (66 p.)"));
        r.addVariableField(f300);

        String mk = generator.generate(r);

        // Section-by-section expected values; built up rather than transcribed
        // so the test is robust against fence-post errors in the algorithm doc.
        String titleContent = "americancounciloflearnedsocietiesannualreportfortheyears20062007and20052006"; // 75
        String expected =
                  titleContent + "_".repeat(95 - titleContent.length()) // 95 title
                + "_____"                                                 // 5  GMD (disabled)
                + "2008"                                                  // 4  pub year (008)
                + "____"                                                  // 4  pagination (no 4-digit run in "1 online resource (66 p.)")
                + "1__"                                                   // 3  edition (Book default — empty 250 + leader "am")
                + "distr"                                                 // 5  publisher (264$b absent; 260$b "Distributed by ERIC Clearinghouse")
                + "a"                                                     // 1  leader type (position 6)
                + "_".repeat(30)                                          // 30 title part (no multi-$p)
                + "_".repeat(10)                                          // 10 title number (no $n)
                + "ameri"                                                 // 5  author (110$a "American Council...")
                + "_".repeat(15)                                          // 15 title dates (no $f)
                + MatchKeyVersion.VERSION                                 // 10 version
                + "e";                                                    // 1  format char ('e' from 245$h "electronic resource")

        assertEquals(188, expected.length(), "expected length");
        assertEquals(expected, mk);
    }
}
