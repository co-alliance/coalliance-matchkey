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
 *   <li>Lowercase, then pad or truncate to 5 characters.</li>
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
            // 07-31-26 An absent 264$b/260$b => five underscores, like every
            // other empty section. Through _v03182026 this returned "" (zero
            // width), producing a SHORT key that contradicted the spec's rule
            // that every section is padded to its fixed character count. It was
            // also inconsistent: a publisher of "&" already cleaned to nothing
            // and padded to 5 here. Reported by Ed Summers (Stanford/POD) after
            // 7% of 39M records came out off-length.
            // https://github.com/co-alliance/coalliance-matchkey/issues/1
            return padWithUnderscores("", OUTPUT_WIDTH);
        }
        String work = input.replace("&", "");
        work = stripPuncuation(work.trim()).replace("_", "");
        // 08-14-26 The trailing toLowerCase() is gone: padWithUnderscores now
        // lowercases before it measures, so the width is that of the final
        // characters. See MatchKeyVersion._v08142026 — this section is where
        // Turkish İ used to push the key past 188.
        return padWithUnderscores(work, OUTPUT_WIDTH);
    }
}
