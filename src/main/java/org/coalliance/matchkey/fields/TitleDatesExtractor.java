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
 * Extracts the 15-character title-inclusive-dates component of the matchKey
 * from MARC 245$f.
 *
 * <p>The 245$f subfield carries the date range of the resource described
 * (e.g. "2005-2007"). The value is passed through {@code stripPuncuation()} and
 * padded to 15 characters. Used historically by Daily Camera Focus Magazine
 * and similar serial-with-date-range cataloging.
 *
 * <p>Trailing whitespace is removed before processing (matching the indexer's
 * historical {@code .trim()} behaviour on the raw subfield toString form);
 * leading whitespace is intentionally preserved and converted to a leading
 * underscore by {@code stripPuncuation()}.
 *
 * <p>Stateless and thread-safe.
 */
public final class TitleDatesExtractor {

    private static final int OUTPUT_WIDTH = 15;

    public String extract(Record record) {
        String value = "";
        DataField titleField = (DataField) record.getVariableField("245");
        if (titleField != null && titleField.getSubfield('f') != null) {
            String raw = titleField.getSubfield('f').getData().stripTrailing();
            value = stripPuncuation(raw);
        }
        return padWithUnderscores(value, OUTPUT_WIDTH);
    }
}
