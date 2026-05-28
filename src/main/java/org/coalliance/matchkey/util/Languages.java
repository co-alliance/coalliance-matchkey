/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MARC language detection used by the matchKey algorithm's non-Roman title
 * fallback path.
 *
 * <p>The primary source is control field 008 positions 35-37 (3 characters,
 * after trimming). If the 008 is unavailable or its language slot is blank,
 * falls back to 041$a.
 *
 * <p>{@link #isNonRoman(String)} returns true for the set of MARC language
 * codes that typically require an 880 vernacular-script field — i.e. the
 * languages whose 245$a values are usually not in the Roman alphabet.
 */
public final class Languages {

    private Languages() {}

    private static final Set<String> NON_ROMAN_CODES;
    static {
        Set<String> s = new HashSet<>();
        s.add("chi"); // Chinese
        s.add("jpn"); // Japanese
        s.add("rus"); // Russian
        s.add("ara"); // Arabic
        s.add("heb"); // Hebrew
        s.add("kor"); // Korean
        s.add("tha"); // Thai
        s.add("ind"); // Indonesian
        s.add("ukr"); // Ukrainian
        s.add("gre"); // Greek (modern)
        s.add("may"); // Malay
        s.add("srp"); // Serbian
        s.add("per"); // Persian
        s.add("hin"); // Hindi
        s.add("bul"); // Bulgarian
        s.add("yid"); // Yiddish
        s.add("grc"); // Greek (ancient)
        s.add("tam"); // Tamil
        s.add("urd"); // Urdu
        s.add("ben"); // Bengali
        s.add("cze"); // Czech
        s.add("pol"); // Polish
        s.add("tur"); // Turkish
        s.add("vie"); // Vietnamese
        NON_ROMAN_CODES = s;
    }

    /**
     * Returns the MARC language code (3 chars) for the record, or null if none
     * can be determined. Tries 008 positions 35-37 first, then 041$a.
     */
    public static String getLanguage(Record record) {
        String control008 = controlField(record, "008");
        if (control008 != null && control008.length() >= 38) {
            String code = control008.substring(35, 38).trim();
            if (code.length() == 3) {
                return code;
            }
        }
        DataField f041 = (DataField) record.getVariableField("041");
        if (f041 != null && f041.getSubfield('a') != null) {
            return f041.getSubfield('a').getData();
        }
        return null;
    }

    /**
     * Returns true if {@code marcLanguageCode} is in the consortium's set of
     * MARC codes whose 245 typically contains non-Roman script (and which
     * therefore tend to have an 880 transliteration).
     */
    public static boolean isNonRoman(String marcLanguageCode) {
        return marcLanguageCode != null && NON_ROMAN_CODES.contains(marcLanguageCode);
    }

    private static String controlField(Record record, String tag) {
        for (ControlField cf : (List<ControlField>) record.getControlFields()) {
            if (tag.equals(cf.getTag())) {
                return cf.getData();
            }
        }
        return null;
    }
}
