/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the 4-character pagination component of the matchKey from MARC field 300.
 *
 * <p>Finds the first run of four consecutive digits anywhere in the 300 field
 * (gated on subfield $a being present), or returns {@code "____"} if no such
 * run is found or the field is absent. Output width: 4 characters.
 *
 * <p>The four-digit search runs against the field's serialized form (all subfields)
 * rather than $a alone, matching the indexer's behaviour: in practice the extent
 * digits live in $a, but accidentally matching digits from $b/$c is a rare
 * occurrence that the indexer accepts.
 *
 * <p>Stateless and thread-safe.
 */
public final class PaginationExtractor {

    private static final Pattern FOUR_DIGITS = Pattern.compile("\\d{4}");
    private static final String  EMPTY       = "____";

    public String extract(Record record) {
        DataField field300 = (DataField) record.getVariableField("300");
        if (field300 == null || field300.getSubfield('a') == null) {
            return EMPTY;
        }
        Matcher m = FOUR_DIGITS.matcher(field300.toString());
        return m.find() ? m.group() : EMPTY;
    }
}
