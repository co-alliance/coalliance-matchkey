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

class FormatCharExtractorTest {

    private final MarcFactory marcFactory = MarcFactory.newInstance();

    private Record plainBook() {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', "Plain book"));
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

    private void addControlField(Record r, String tag, String data) {
        r.addVariableField(marcFactory.newControlField(tag, data));
    }

    private FormatCharExtractor noHint() { return new FormatCharExtractor(null); }

    @Test
    @DisplayName("plain print book with no electronic markers returns 'p'")
    void plainBookIsPrint() {
        assertEquals("p", noHint().extract(plainBook()));
    }

    @Test
    @DisplayName("245$h 'electronic resource' returns 'e'")
    void electronicVia245h() {
        Record r = plainBook();
        addSubfield(r, "245", 'h', "[electronic resource]");
        assertEquals("e", noHint().extract(r));
    }

    @Test
    @DisplayName("337$a starting with 'c' (RDA carrier 'computer') returns 'e'")
    void electronicViaRDA337a() {
        Record r = plainBook();
        addSubfield(r, "337", 'a', "computer");
        assertEquals("e", noHint().extract(r));
    }

    @Test
    @DisplayName("007 starting with 'C' returns 'e'")
    void electronicVia007() {
        Record r = plainBook();
        addControlField(r, "007", "cr |||||||||||");
        assertEquals("e", noHint().extract(r));
    }

    @Test
    @DisplayName("gov doc (086) with online access (856) returns 'e'")
    void electronicGovDoc() {
        Record r = plainBook();
        addSubfield(r, "086", 'a', "ED 1.310/2:516193");
        addSubfield(r, "856", 'u', "http://eric.ed.gov/...");
        assertEquals("e", noHint().extract(r));
    }

    @Test
    @DisplayName("086 alone (no 856) does not mark electronic")
    void govDocAloneIsPrint() {
        Record r = plainBook();
        addSubfield(r, "086", 'a', "ED 1.310/2:516193");
        assertEquals("p", noHint().extract(r));
    }

    @Test
    @DisplayName("filename hint containing 'electronic' returns 'e'")
    void filenameElectronic() {
        FormatCharExtractor x = new FormatCharExtractor("cu-electronic-2026-04.marc");
        assertEquals("e", x.extract(plainBook()));
    }

    @Test
    @DisplayName("filename hint containing 'physical' overrides MARC electronic markers")
    void filenamePhysicalOverridesMarc() {
        Record r = plainBook();
        addSubfield(r, "245", 'h', "[electronic resource]");
        addControlField(r, "007", "cr |||||||||||");

        FormatCharExtractor x = new FormatCharExtractor("cu-physical-2026-04.marc");
        assertEquals("p", x.extract(r));
    }

    @Test
    @DisplayName("fromSystemProperty() uses the documented property key")
    void fromSystemPropertyReadsKey() {
        String previous = System.getProperty(FormatCharExtractor.SYSTEM_PROPERTY_KEY);
        try {
            System.setProperty(FormatCharExtractor.SYSTEM_PROPERTY_KEY, "ebook-collection.marc");
            FormatCharExtractor x = FormatCharExtractor.fromSystemProperty();
            assertEquals("e", x.extract(plainBook()));
        } finally {
            if (previous == null) {
                System.clearProperty(FormatCharExtractor.SYSTEM_PROPERTY_KEY);
            } else {
                System.setProperty(FormatCharExtractor.SYSTEM_PROPERTY_KEY, previous);
            }
        }
    }
}
