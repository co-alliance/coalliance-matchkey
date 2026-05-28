/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the 4-character publication-year component of the matchKey.
 *
 * <p>Cascading source preference:
 * <ol>
 *   <li>MARC control field 008 — see {@link #dateFrom008(Record)} for the
 *       reissue / government-document / date1-vs-date2 logic.</li>
 *   <li>MARC 264$c (RDA), with bracket and "?" punctuation stripped and
 *       precedence given to years preceded by a {@code "c"} (copyright).</li>
 *   <li>MARC 260$c (pre-RDA), with the rightmost 4-digit run winning.</li>
 *   <li>If nothing valid is found, the literal string {@code "0000"}.</li>
 * </ol>
 *
 * <p>Output width: always 4 characters.
 *
 * <p>This extractor vendors the algorithm body from {@code MarcUtil.getPublicationYear}
 * in the consortium's grReports30 codebase. {@code MarcUtil} as a whole carries
 * grReports plumbing irrelevant to matchKey generation; only the publication-year
 * routines come along.
 *
 * <p>Stateless and thread-safe.
 */
public final class PublicationYearExtractor {

    private static final int    MIN_YEAR = 1200;
    private static final String FALLBACK = "0000";

    private static final Pattern FOUR_DIGITS = Pattern.compile("\\d{4}");

    public String extract(Record record) {
        String result = dateFrom008(record);
        if (isValidYear(result)) return result.trim();

        result = field264c(record);
        if (isValidYear(result)) return result.trim();

        result = field260c(record);
        if (isValidYear(result)) return result.trim();

        return FALLBACK;
    }

    private static boolean isValidYear(String s) {
        if (s == null) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Resolves the publication year from control field 008 (positions 7-10 =
     * date1, 11-14 = date2). Reissue records (position 6 == 'r') always use
     * date1. Government documents (086$a present) always use date1. Otherwise
     * date2 wins when valid (4 digits, not 9999, >= 1200); falls back to date1.
     * Returns null when no valid year can be produced.
     */
    private static String dateFrom008(Record record) {
        for (ControlField cf : record.getControlFields()) {
            if (!"008".equals(cf.getTag().trim())) continue;
            String data = cf.getData();
            if (data == null || data.length() < 15) continue;

            boolean reissue = "r".equals(data.substring(6, 7));
            String dateOne  = data.substring(7, 11);
            String dateTwo  = data.substring(11, 15);

            if (!reissue) {
                DataField govDoc = (DataField) record.getVariableField("086");
                if (govDoc != null && govDoc.getSubfield('a') != null) {
                    return dateOne;
                }
                try {
                    int v = Integer.parseInt(dateTwo);
                    if (v != 9999 && dateTwo.trim().length() == 4 && v >= MIN_YEAR) {
                        return dateTwo;
                    }
                } catch (NumberFormatException e) {
                    // fall through
                }
            }
            if (dateOne.trim().length() == 4) {
                try {
                    Integer.parseInt(dateOne);
                    return dateOne;
                } catch (NumberFormatException e) {
                    // fall through to next 008 (or end of loop)
                }
            }
        }
        return null;
    }

    private static String field264c(Record record) {
        DataField field = (DataField) record.getVariableField("264");
        if (field == null || field.getSubfield('c') == null) return null;
        return process264c(field.getSubfield('c').getData());
    }

    /**
     * Strips {@code [}, {@code ]}, {@code ?} from the input, then chooses among
     * 4-digit substrings: any year that appears immediately after a {@code "c"}
     * (copyright) wins; otherwise the rightmost run wins.
     */
    static String process264c(String input) {
        if (input == null || input.isEmpty()) return null;
        String cleaned = input.replace("[", "").replace("]", "").replace("?", "");

        List<String> years = findYears(cleaned);
        if (years.isEmpty()) return null;

        String fallback = "";
        for (int i = years.size() - 1; i >= 0; i--) {
            String year = years.get(i);
            if (cleaned.toLowerCase().contains("c" + year)) {
                return year;
            }
            if (fallback.isEmpty()) {
                fallback = year;
            }
        }
        return fallback;
    }

    /**
     * Returns the rightmost 4-digit run anywhere in the serialized 260 field,
     * or "" when the field or subfield is absent. The indexer's source contains
     * a {@code startsWith("c")} branch on the matched digits that is dead code
     * (the regex only matches digits); preserved here as a comment-only note.
     */
    private static String field260c(Record record) {
        DataField field = (DataField) record.getVariableField("260");
        if (field == null || field.getSubfield('c') == null) return "";
        List<String> years = findYears(field.toString());
        if (years.isEmpty()) return "";
        return years.get(years.size() - 1);
    }

    private static List<String> findYears(String input) {
        List<String> years = new ArrayList<>();
        Matcher m = FOUR_DIGITS.matcher(input);
        while (m.find()) years.add(m.group());
        return years;
    }
}
