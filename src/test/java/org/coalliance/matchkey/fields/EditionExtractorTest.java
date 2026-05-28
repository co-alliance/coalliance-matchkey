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

class EditionExtractorTest {

    private final EditionExtractor extractor = new EditionExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    // Leader position 6-7 == "am" means Book; "as" means continuing resource
    // (journal/serial); "cm" means notated music.
    private static final String BOOK_LEADER    = "02801nam a22005052u 4500";
    private static final String JOURNAL_LEADER = "02801nas a22005052u 4500";
    private static final String MUSIC_LEADER   = "02801ncm a22005052u 4500";

    private Record withEdition(String leader, String editionStatement) {
        Record r = marcFactory.newRecord(leader);
        DataField f250 = marcFactory.newDataField("250", ' ', ' ');
        f250.addSubfield(marcFactory.newSubfield('a', editionStatement));
        r.addVariableField(f250);
        return r;
    }

    @Test
    @DisplayName("missing 250 on a Book defaults to '1__' (1st edition)")
    void emptyOnBookDefaultsToFirst() {
        Record r = marcFactory.newRecord(BOOK_LEADER);
        assertEquals("1__", extractor.extract(r));
    }

    @Test
    @DisplayName("missing 250 on a journal returns '___'")
    void emptyOnJournalReturnsUnderscores() {
        Record r = marcFactory.newRecord(JOURNAL_LEADER);
        assertEquals("___", extractor.extract(r));
    }

    @Test
    @DisplayName("missing 250 on music returns '___' (not a Book)")
    void emptyOnMusicReturnsUnderscores() {
        Record r = marcFactory.newRecord(MUSIC_LEADER);
        assertEquals("___", extractor.extract(r));
    }

    @Test
    @DisplayName("'1st ed.' returns '1__'")
    void firstEdAbbreviation() {
        assertEquals("1__", extractor.extract(withEdition(BOOK_LEADER, "1st ed.")));
    }

    @Test
    @DisplayName("'First edition' returns '1__' via the 'fir' word match")
    void firstEditionSpelledOut() {
        assertEquals("1__", extractor.extract(withEdition(BOOK_LEADER, "First edition")));
    }

    @Test
    @DisplayName("'Second edition' returns '2__'")
    void secondEdition() {
        assertEquals("2__", extractor.extract(withEdition(BOOK_LEADER, "Second edition")));
    }

    @Test
    @DisplayName("'3rd revised' returns '3__'")
    void thirdRevised() {
        assertEquals("3__", extractor.extract(withEdition(BOOK_LEADER, "3rd revised")));
    }

    @Test
    @DisplayName("'10th anniversary' returns '10_'")
    void tenthAnniversary() {
        assertEquals("10_", extractor.extract(withEdition(BOOK_LEADER, "10th anniversary")));
    }

    @Test
    @DisplayName("numeric-only '42 ed.' returns '42_'")
    void numericTwoDigit() {
        assertEquals("42_", extractor.extract(withEdition(BOOK_LEADER, "42 ed.")));
    }

    @Test
    @DisplayName("alpha-only 'Reprint.' returns 'rep' (no ordinal word match)")
    void alphaReprint() {
        assertEquals("rep", extractor.extract(withEdition(BOOK_LEADER, "Reprint.")));
    }

    @Test
    @DisplayName("ordinal word recognition is case-insensitive ('FIFTH' -> '5__')")
    void caseInsensitiveWordMatch() {
        assertEquals("5__", extractor.extract(withEdition(BOOK_LEADER, "FIFTH ANNIVERSARY")));
    }
}
