/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.coalliance.matchkey.util.AccentNormalizer.normalize;
import static org.coalliance.matchkey.util.IsbnValidator.returnValidISBNs;
import static org.coalliance.matchkey.util.Languages.getLanguage;
import static org.coalliance.matchkey.util.Languages.isNonRoman;
import static org.coalliance.matchkey.util.Padding.padWithUnderscores;
import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuationSpace;

/**
 * Extracts the 95-character title component of the matchKey from MARC 245
 * (or its linked 880 vernacular-script field), with fallbacks for non-Roman
 * languages without an 880.
 *
 * <p>Decision order:
 * <ol>
 *   <li>If the record language is non-Roman (see {@code util.Languages}) AND
 *       no 880$a is present, return the first available identifier as the
 *       title: LCC (010$a, spaces removed) → first valid ISBN (020$a) →
 *       ISSN (022$a, must be 9 chars and contain '-').</li>
 *   <li>If 245$6 indicates a linked 880, replace the title field with the 880
 *       whose $6 points back at 245.</li>
 *   <li>Concatenate stripPuncuationSpace-cleaned 245$a, 245$b, 245$p (or the
 *       880 equivalents), with all whitespace removed, NFD-normalised, padded
 *       to 95 characters, and lowercased.</li>
 * </ol>
 *
 * <p>Stateless and thread-safe.
 */
public final class TitleExtractor {

    private static final int OUTPUT_WIDTH = 95;

    public String extract(Record record) {
        DataField titleField = (DataField) record.getVariableField("245");
        if (titleField == null) {
            // 07-31-26 No 245 field at all => 95 underscores, like every other
            // empty section. Through _v03182026 this returned "" (zero width),
            // which made 245-less records (MARC holdings, Leader/06='x') produce
            // a SHORT key and contradicted the spec's rule that every section is
            // padded to its fixed character count. It was also inconsistent: a
            // 245 that was present but whose $a$b$p cleaned to nothing already
            // padded to 95 here. Reported by Ed Summers (Stanford/POD) after
            // 7% of 39M records came out off-length.
            // https://github.com/co-alliance/coalliance-matchkey/issues/1
            return padWithUnderscores("", OUTPUT_WIDTH);
        }

        String fallback = nonRomanFallback(record);
        if (fallback != null) {
            return padWithUnderscores(fallback.trim(), OUTPUT_WIDTH);
        }

        titleField = resolveLinkedTitleField(record, titleField);

        String combined = subfieldClean(titleField, 'a')
                        + subfieldClean(titleField, 'b')
                        + subfieldClean(titleField, 'p');

        if (!Normalizer.isNormalized(combined, Normalizer.Form.NFD)) {
            combined = normalize(combined);
        }
        // 08-14-26 padWithUnderscores lowercases before measuring; the
        // trailing toLowerCase() it used to carry is redundant.
        return padWithUnderscores(combined, OUTPUT_WIDTH);
    }

    /**
     * If the record is in a non-Roman language and has no 880$a vernacular
     * field, returns a fallback identifier (LCC, ISBN, or ISSN) to use as the
     * title section. Returns null when no fallback is needed.
     */
    private static String nonRomanFallback(Record record) {
        String language = getLanguage(record);
        if (!isNonRoman(language)) return null;
        if (subfieldValue(record, "880", 'a') != null) return null;

        String lcc = lccNumber(record);
        if (lcc != null) return lcc;

        String isbn = firstValidIsbn(record);
        if (isbn != null) return isbn;

        return issn(record);
    }

    /** Returns 010$a with spaces removed, or null. */
    private static String lccNumber(Record record) {
        String raw = subfieldValue(record, "010", 'a');
        return raw == null ? null : raw.replace(" ", "");
    }

    /** Returns the first ISBN from any 020$a that passes the validator, or null. */
    private static String firstValidIsbn(Record record) {
        if (record.getVariableField("020") == null) return null;
        Set<DataField> all020 = allFields(record, "020");
        Set<String> candidates = new LinkedHashSet<>();
        for (DataField field : all020) {
            Subfield a = field.getSubfield('a');
            if (a == null || a.getData() == null) return null;
            candidates.add(a.getData().trim());
        }
        Set<String> valid = returnValidISBNs(candidates);
        return valid.isEmpty() ? null : valid.iterator().next();
    }

    /** Returns 022$a only if it is exactly 9 characters and contains '-'. */
    private static String issn(Record record) {
        String raw = subfieldValue(record, "022", 'a');
        if (raw == null || !raw.contains("-") || raw.length() != 9) return null;
        return raw.trim();
    }

    /**
     * If 245$6 marks a linked 880 field, finds the 880 whose own $6 points back
     * at 245 and returns it; otherwise returns {@code titleField} unchanged.
     */
    private static DataField resolveLinkedTitleField(Record record, DataField titleField) {
        Subfield link = titleField.getSubfield('6');
        if (link == null || link.getData() == null || !link.getData().contains("880")) {
            return titleField;
        }
        for (DataField field880 : allFields(record, "880")) {
            Subfield back = field880.getSubfield('6');
            if (back != null && back.getData() != null && back.getData().contains("245")) {
                return field880;
            }
        }
        return titleField;
    }

    /**
     * Returns {@code stripPuncuationSpace(field$code)} with all whitespace
     * removed and trimmed — the matchKey-mode subfield contribution. Empty
     * string when the subfield is absent.
     */
    private static String subfieldClean(DataField field, char code) {
        if (field == null) return "";
        Subfield sf = field.getSubfield(code);
        if (sf == null || sf.getData() == null) return "";
        return stripPuncuationSpace(sf.getData()).replace(" ", "").trim();
    }

    /** Plain subfield-data accessor: first occurrence only, null if absent. */
    private static String subfieldValue(Record record, String tag, char code) {
        VariableField vf = record.getVariableField(tag);
        if (!(vf instanceof DataField)) return null;
        Subfield sf = ((DataField) vf).getSubfield(code);
        return (sf == null) ? null : sf.getData();
    }

    /** Returns every {@code tag} field on the record (preserving order). */
    private static LinkedHashSet<DataField> allFields(Record record, String tag) {
        LinkedHashSet<DataField> out = new LinkedHashSet<>();
        for (Object f : record.getDataFields()) {
            DataField df = (DataField) f;
            if (tag.equals(df.getTag())) out.add(df);
        }
        return out;
    }
}
