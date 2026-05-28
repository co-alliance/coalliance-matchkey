/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import static org.coalliance.matchkey.util.Padding.padWithUnderscores;
import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuation;

/**
 * Extracts the 10-character title number component of the matchKey from MARC 245$n.
 *
 * <p>The 245$n subfield carries the "number of part" (e.g. "v. 1", "no. 3").
 * Its value is passed through {@code stripPuncuation()} and padded to 10 characters
 * with trailing underscores, or returned as 10 underscores if the subfield is absent.
 *
 * <p>Stateless and thread-safe.
 */
public final class TitleNumberExtractor {

    private static final int OUTPUT_WIDTH = 10;

    public String extract(Record record) {
        String value = "";
        DataField titleField = (DataField) record.getVariableField("245");
        if (titleField != null && titleField.getSubfield('n') != null) {
            value = stripPuncuation(titleField.getSubfield('n').getData()).trim();
        }
        return padWithUnderscores(value, OUTPUT_WIDTH);
    }
}
