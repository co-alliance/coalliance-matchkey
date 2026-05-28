/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

import static org.coalliance.matchkey.util.Padding.padWithUnderscores;
import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuation;

/**
 * Extracts the 5-character publisher name component of the matchKey, prefering
 * MARC 264$b (RDA) and falling back to 260$b (pre-RDA).
 *
 * <p>The cleaning sequence:
 * <ol>
 *   <li>Read 264$b; if empty, fall back to 260$b. Trailing commas on the
 *       subfield value are stripped.</li>
 *   <li>Remove all ampersands (so {@code stripPuncuation()} does not expand
 *       them to the literal word "and").</li>
 *   <li>Apply {@code stripPuncuation()} with underscore replacement.</li>
 *   <li>Delete all resulting underscores so the remaining characters are run
 *       together with no separators (yields e.g. {@code "DistributedbyERIC..."}
 *       for {@code "Distributed by ERIC Clearinghouse"}).</li>
 *   <li>Pad or truncate to 5 characters and lowercase.</li>
 * </ol>
 *
 * <p>Note: the indexer's source intends an NFD accent normalisation here (added
 * 2025-09-17) but a guard variable bug makes it a no-op in practice. This
 * library preserves the as-shipped behaviour; accented characters in the
 * publisher name pass through unchanged.
 *
 * <p>Stateless and thread-safe.
 */
public final class PublisherExtractor {

    private static final int OUTPUT_WIDTH = 5;

    public String extract(Record record) {
        String publisher = publisherSubfield(record).trim();
        return cleanup(publisher);
    }

    private static String publisherSubfield(Record record) {
        String pub = subfieldValue(record, "264", 'b');
        if (pub.trim().isEmpty()) {
            pub = subfieldValue(record, "260", 'b');
        }
        return pub;
    }

    private static String subfieldValue(Record record, String tag, char code) {
        VariableField vf = record.getVariableField(tag);
        if (!(vf instanceof DataField)) return "";
        DataField field = (DataField) vf;
        if (field.getSubfield(code) == null || field.getSubfield(code).getData() == null) {
            return "";
        }
        String value = field.getSubfield(code).getData().trim();
        if (value.endsWith(",")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String cleanup(String input) {
        if (input == null || input.trim().isEmpty()) {
            return padWithUnderscores("", OUTPUT_WIDTH);
        }
        String work = input.replace("&", "");
        work = stripPuncuation(work.trim()).replace("_", "");
        return padWithUnderscores(work, OUTPUT_WIDTH).toLowerCase();
    }
}
