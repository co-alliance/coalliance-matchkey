/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.coalliance.matchkey.util.AccentNormalizer.removeAccents;

/**
 * Extracts the 3-character edition component of the matchKey from MARC 250$a.
 *
 * <p>Two-phase: a raw parser reduces 250$a to a 3-character code, then an
 * interpretation phase maps recognised English ordinal words ("first", "1st",
 * "second"/"sec", …, "10th") to their digit form ({@code "1__"}, {@code "2__"},
 * …, {@code "10_"}). An empty 250 on a Book record (Leader positions 6-7 == "AM")
 * defaults to {@code "1__"}; an empty 250 on any other record returns
 * {@code "___"}.
 *
 * <p>The raw parser prefers numeric over alphabetic: it tries 3 / 2 / 1 leading
 * digits first, then 3 / 2 / 1 leading letters (with accent removal). Output of
 * the raw parser is always exactly 3 characters: digits/letters padded on the
 * right with {@code "_"}.
 *
 * <p>The Book-default check uses the MARC Leader directly rather than the
 * indexer's full {@code getFormat()} machinery; "Book" is the indexer's name
 * for Leader type/bibliographic-level "AM" (textual / monographic), which is
 * the only path inside {@code getFormat()} that produces the "Book" facet.
 *
 * <p>Stateless and thread-safe.
 */
public final class EditionExtractor {

    private static final String FIRST_EDITION = "1__";
    private static final String EMPTY         = "___";

    private static final Pattern THREE_DIGITS = Pattern.compile("[0-9]{3}");
    private static final Pattern TWO_DIGITS   = Pattern.compile("[0-9]{2}");
    private static final Pattern ONE_DIGIT    = Pattern.compile("[0-9]");
    private static final Pattern THREE_ALPHA  = Pattern.compile("[a-zA-Z]{3}");
    private static final Pattern TWO_ALPHA    = Pattern.compile("[a-zA-Z]{2}");
    private static final Pattern ONE_ALPHA    = Pattern.compile("[a-zA-Z]");

    public String extract(Record record) {
        String raw = field250a(record).toLowerCase(Locale.ROOT);

        // Word-form ordinals. Each branch matches the indexer's contains() check
        // against the 3-char raw code (the first 3 letters of the edition statement).
        if (raw.contains("1st") || raw.contains("fir")) return FIRST_EDITION;
        if (raw.contains("2nd") || raw.contains("sec")) return "2__";
        if (raw.contains("3rd") || raw.contains("thi")) return "3__";
        if (raw.contains("4th") || raw.contains("for")) return "4__";
        if (raw.contains("5th") || raw.contains("fif")) return "5__";
        if (raw.contains("6th") || raw.contains("six")) return "6__";
        if (raw.contains("7th") || raw.contains("sev")) return "7__";
        if (raw.contains("8th") || raw.contains("eig")) return "8__";
        if (raw.contains("9th") || raw.contains("nin")) return "9__";
        if (raw.contains("10t")) return "10_";

        // Empty after stripping underscores → default to 1st edition for Books only.
        if (raw.trim().replace("_", "").isEmpty() && isBook(record)) {
            return FIRST_EDITION;
        }
        return raw;
    }

    private static String field250a(Record record) {
        DataField editionField = (DataField) record.getVariableField("250");
        if (editionField == null || editionField.getSubfield('a') == null) {
            return EMPTY;
        }
        String stmt = editionField.getSubfield('a').getData().trim();

        Matcher m;
        if ((m = THREE_DIGITS.matcher(stmt)).find()) return m.group();
        if ((m = TWO_DIGITS  .matcher(stmt)).find()) return m.group() + "_";
        if ((m = ONE_DIGIT   .matcher(stmt)).find()) return m.group() + "__";

        if ((m = THREE_ALPHA.matcher(stmt)).find()) return removeAccents(m.group());
        if ((m = TWO_ALPHA  .matcher(stmt)).find()) return removeAccents(m.group()) + "_";
        if ((m = ONE_ALPHA  .matcher(stmt)).find()) return removeAccents(m.group()) + "__";

        return EMPTY;
    }

    private static boolean isBook(Record record) {
        Leader leader = record.getLeader();
        if (leader == null) return false;
        String s = leader.toString();
        return s != null && s.length() >= 8 && s.substring(6, 8).equalsIgnoreCase("AM");
    }
}
