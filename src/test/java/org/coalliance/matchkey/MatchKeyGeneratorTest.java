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
    @DisplayName("matchKey is 188 characters when title and publisher are present")
    void matchkeyWidthFullRecord() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Some Title"));
        r.addVariableField(f245);
        DataField f264 = marcFactory.newDataField("264", ' ', '1');
        f264.addSubfield(marcFactory.newSubfield('b', "Some Publisher"));
        r.addVariableField(f264);
        assertEquals(188, generator.generate(r).length());
    }

    @Test
    @DisplayName("Turkish İ in the publisher keeps the key at 188 (_v08142026)")
    void matchkeyWidthTurkishPublisher() {
        // 08-14-26 İ (U+0130) lowercases to two code points, so measuring the
        // 5-char publisher section before the key-wide lowercase let it overrun.
        // Ed Summers (Stanford/POD) found 10,575 such records in 39.4M: 189 for
        // one İ, 190 for two, 191 for three. İ is the only character in Unicode
        // whose lowercase mapping lengthens the string.
        assertEquals(188, generator.generate(withPublisher("İstanbul Press")).length());
        assertEquals(188, generator.generate(withPublisher("İİstanbul")).length());
        assertEquals(188, generator.generate(withPublisher("İİİstanbul")).length());
        // The ASCII spelling was never affected; it stays the control.
        assertEquals(188, generator.generate(withPublisher("Istanbul Press")).length());
    }

    @Test
    @DisplayName("İ still distinguishes two publishers, it just no longer widens the key")
    void turkishPublisherStillDistinct() {
        // The fix is about width, not matching: an İ publisher and its ASCII
        // spelling did not match before and still do not. What matters is that
        // two copies of the same İ record produce the same 188-char key.
        String turkish = generator.generate(withPublisher("İstanbul Press"));
        String ascii   = generator.generate(withPublisher("Istanbul Press"));
        assertEquals(turkish, generator.generate(withPublisher("İstanbul Press")));
        assertTrue(!turkish.equals(ascii), "İ and I publishers should still differ");
    }

    private Record withPublisher(String publisher) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Some Title"));
        r.addVariableField(f245);
        DataField f264 = marcFactory.newDataField("264", ' ', '1');
        f264.addSubfield(marcFactory.newSubfield('b', publisher));
        r.addVariableField(f264);
        return r;
    }

    @Test
    @DisplayName("title-less, publisher-less record is still 188 characters (_v07312026)")
    void matchkeyWidthBareRecord() {
        // 07-31-26 A record with no 245 and no 264/260 (e.g. a MARC holdings record,
        // Leader/06='x') now pads both sections to full width, so the key is 188 like
        // every other key. Through _v03182026 those sections were zero-width and this
        // record produced a short 88-char key (188 - 95 - 5). Keys are now fixed-width
        // as the specification has always described.
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(188, generator.generate(r).length());
    }

    @Test
    @DisplayName("every key is 188 characters regardless of which sections are absent")
    void matchkeyWidthIsAlwaysFixed() {
        // The four record shapes that produced 188/183/93/88 through _v03182026.
        Record bare = marcFactory.newRecord("02801nam a22005052u 4500");

        Record titleOnly = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField t245 = marcFactory.newDataField("245", '1', '0');
        t245.addSubfield(marcFactory.newSubfield('a', "Some Title"));
        titleOnly.addVariableField(t245);

        Record publisherOnly = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField p264 = marcFactory.newDataField("264", ' ', '1');
        p264.addSubfield(marcFactory.newSubfield('b', "Some Publisher"));
        publisherOnly.addVariableField(p264);

        Record both = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField b245 = marcFactory.newDataField("245", '1', '0');
        b245.addSubfield(marcFactory.newSubfield('a', "Some Title"));
        both.addVariableField(b245);
        DataField b264 = marcFactory.newDataField("264", ' ', '1');
        b264.addSubfield(marcFactory.newSubfield('b', "Some Publisher"));
        both.addVariableField(b264);

        for (Record r : new Record[] { bare, titleOnly, publisherOnly, both }) {
            assertEquals(188, generator.generate(r).length());
        }
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
