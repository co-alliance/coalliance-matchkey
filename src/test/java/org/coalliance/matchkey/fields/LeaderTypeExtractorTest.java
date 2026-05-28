/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderTypeExtractorTest {

    private final LeaderTypeExtractor extractor = new LeaderTypeExtractor();
    private final MarcFactory marcFactory = MarcFactory.newInstance();

    @Test
    @DisplayName("returns Leader position 6 for a book record (type 'a')")
    void bookRecord() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        assertEquals("a", extractor.extract(r));
    }

    @Test
    @DisplayName("returns Leader position 6 for a projected medium (type 'g')")
    void projectedMedium() {
        Record r = marcFactory.newRecord("01234ngm a22005052u 4500");
        assertEquals("g", extractor.extract(r));
    }

    @Test
    @DisplayName("returns Leader position 6 for a music recording (type 'j')")
    void musicRecording() {
        Record r = marcFactory.newRecord("01234njm a22005052u 4500");
        assertEquals("j", extractor.extract(r));
    }
}
