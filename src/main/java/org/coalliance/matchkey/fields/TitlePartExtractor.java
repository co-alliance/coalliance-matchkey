/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import java.util.List;

import static org.coalliance.matchkey.util.Padding.padWithUnderscores;
import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuation;

/**
 * Extracts the 30-character title part component of the matchKey from MARC 245$p.
 *
 * <p>The 245$p subfield carries the "name of part" (e.g. "Section A. Mathematics").
 * Only runs of more than one $p contribute; a single $p produces an empty section
 * (the indexer relies on the first $p having already been included in the title
 * field's contribution to the matchKey).
 *
 * <p>When more than one $p is present, every $p — including the first — is
 * passed through {@code stripPuncuation()}, and the first 9 characters of each
 * are concatenated. The "9" is an artifact of indexer behaviour: the source
 * uses {@code subSequence(0, min(10, length) - 1)}, dropping the last character
 * of the 10-character slice (or the last character of a short subfield). The
 * result is padded to 30 characters with trailing underscores.
 *
 * <p>Stateless and thread-safe.
 */
public final class TitlePartExtractor {

    private static final int OUTPUT_WIDTH      = 30;
    private static final int PER_SUBFIELD_MAX  = 10;

    public String extract(Record record) {
        StringBuilder result = new StringBuilder();

        DataField titleField = (DataField) record.getVariableField("245");
        if (titleField != null) {
            List<Subfield> subfieldsP = titleField.getSubfields('p');
            if (subfieldsP.size() > 1) {
                for (Subfield p : subfieldsP) {
                    String cleaned = stripPuncuation(p.getData().trim());
                    int end = Math.min(PER_SUBFIELD_MAX, cleaned.length());
                    if (end > 0) {
                        result.append(cleaned, 0, end - 1);
                    }
                }
            }
        }
        return padWithUnderscores(result.toString(), OUTPUT_WIDTH);
    }
}
