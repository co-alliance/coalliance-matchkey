/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import static org.coalliance.matchkey.util.AccentNormalizer.removeAccents;
import static org.coalliance.matchkey.util.Padding.padWithUnderscores;
import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuation;

/**
 * Extracts the 5-character author component of the matchKey from MARC 1XX/130 $a.
 *
 * <p>Fields scanned in order:
 * <ul>
 *   <li>100$a — personal author</li>
 *   <li>110$a — corporate author</li>
 *   <li>111$a — meeting/conference</li>
 *   <li>130$a — uniform title</li>
 * </ul>
 *
 * <p>For each field present, $a is passed through {@code stripPuncuation()} and
 * {@code removeAccents()}, and the cleaned values are concatenated in the order
 * above. The combined string is padded or truncated to 5 characters.
 *
 * <p>Real MARC records normally carry at most one of these fields; concatenation
 * is defensive behaviour from the indexer for unusual records.
 *
 * <p>The indexer applies {@code stripPuncuationAndRemoveAccents} twice to each
 * value — once on the raw subfield and once on the result — and this library
 * matches that. The second pass is NOT always a no-op: when the $a begins with
 * punctuation (e.g. {@code "[Faidit, Hugues]"} or a leading quote), the first
 * pass converts the leading bracket/quote to {@code "_"} and the second pass
 * strips that leading {@code "_"}. Cleaning only once would leave the underscore
 * and shift the author window, diverging from the production index.
 *
 * <p>Field 113 was used historically; the indexer changed to 130 on 2023-02-14
 * and this library follows.
 *
 * <p>Stateless and thread-safe.
 */
public final class AuthorExtractor {

    private static final int OUTPUT_WIDTH = 5;

    private static final String[] AUTHOR_FIELDS = { "100", "110", "111", "130" };

    public String extract(Record record) {
        StringBuilder result = new StringBuilder();
        for (String tag : AUTHOR_FIELDS) {
            DataField field = (DataField) record.getVariableField(tag);
            if (field != null && field.getSubfield('a') != null) {
                // Match the indexer's extraction exactly: it reads the subfield via
                // toString() (which yields "$a<data>") and strips the "$a" prefix,
                // then applies the clean step TWICE.
                String raw = field.getSubfield('a').toString().replace("$a", "");
                result.append(clean(clean(raw)));
            }
        }
        return padWithUnderscores(result.toString(), OUTPUT_WIDTH);
    }

    // stripPuncuation + removeAccents, the composition the indexer calls
    // stripPuncuationAndRemoveAccents. Applied TWICE per field above: this is NOT
    // a no-op when the $a starts with punctuation (e.g. "[Faidit, Hugues]" or a
    // leading quote). The first pass turns the leading "[" into "_"; the second
    // pass strips that now-leading "_". Applying it once would leave the "_" and
    // shift the 5-char author window, diverging from the production index.
    private static String clean(String raw) {
        return removeAccents(stripPuncuation(raw));
    }
}
