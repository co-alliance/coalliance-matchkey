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

class TitlePartExtractorTest {

    private final TitlePartExtractor extractor = new TitlePartExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private static final String EMPTY = "______________________________"; // 30 underscores

    private Record withTitleParts(String... pSubfields) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Something"));
        for (String p : pSubfields) {
            f245.addSubfield(marcFactory.newSubfield('p', p));
        }
        r.addVariableField(f245);
        return r;
    }

    @Test
    @DisplayName("missing 245 returns 30 underscores")
    void noTitle() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals(EMPTY, extractor.extract(r));
    }

    @Test
    @DisplayName("245 with no $p returns 30 underscores")
    void noSubfieldP() {
        assertEquals(EMPTY, extractor.extract(withTitleParts()));
    }

    @Test
    @DisplayName("exactly one $p is intentionally skipped (handled by Title section)")
    void singleSubfieldP() {
        assertEquals(EMPTY, extractor.extract(withTitleParts("Volume 1.")));
    }

    @Test
    @DisplayName("two short $p subfields concatenate their first 8 chars each")
    void twoShortParts() {
        // "Part one." -> "Part_one_" (9 chars) -> take first 8 ("Part_one")
        // "Part two." -> "Part_two_" (9 chars) -> take first 8 ("Part_two")
        // concat = "Part_onePart_two" (16); pad to 30
        assertEquals("part_onepart_two______________",
                extractor.extract(withTitleParts("Part one.", "Part two.")));
    }

    @Test
    @DisplayName("three long $p subfields each contribute their first 9 chars (indexer Javadoc example)")
    void threeLongParts() {
        // From CoAllianceIndexUtil javadoc on field245p; spaces become underscores
        // because stripPuncuation runs with UNDERSCORE replacement.
        assertEquals("manufactuindustry_frozen_fr___",
                extractor.extract(withTitleParts(
                        "Manufacturing.",
                        "Industry series.",
                        "Frozen fruit, juice, and vegetable manufacturing.")));
    }
}
