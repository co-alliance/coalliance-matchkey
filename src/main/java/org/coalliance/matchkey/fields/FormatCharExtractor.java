/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.util.List;

/**
 * Extracts the 1-character format component of the matchKey: {@code "e"} for an
 * electronic resource, {@code "p"} otherwise.
 *
 * <p>The decision combines MARC field evidence with an optional filename hint:
 * <ul>
 *   <li>MARC-derived electronic markers: 245$h "electronic resource",
 *       590$a / 533$a "electronic reproduction", 300$a "online resource",
 *       337$a starting with 'c' (RDA), 007 starting with 'C',
 *       or the combination of 086 (gov doc) plus 856 (online access).</li>
 *   <li>Filename hint containing "electronic" or "ebook" adds electronic evidence.</li>
 *   <li>Filename hint containing "physical" or "print" <em>overrides</em> any
 *       MARC-derived electronic marker and forces the result to {@code "p"}.</li>
 * </ul>
 *
 * <p>The filename hint is optional. Pass {@code null} (or use the no-arg behaviour
 * of {@link #fromSystemProperty()} when the property is unset) for pure-MARC
 * detection — appropriate for any consumer that does not follow the CoAlliance
 * convention of encoding format hints in MARC filenames such as
 * {@code cu-electronic-2026-04.marc}.
 *
 * <p>Stateless apart from the immutable filename hint; safe to reuse across threads.
 */
public final class FormatCharExtractor {

    /** System property key matching the CoAlliance indexer's convention. */
    public static final String SYSTEM_PROPERTY_KEY = "org.coalliance.indexing.fileName";

    private static final String ELECTRONIC = "e";
    private static final String PRINT      = "p";

    private final String marcFilenameHint;

    /**
     * @param marcFilenameHint optional MARC filename used for format detection,
     *                         or {@code null} for pure-MARC behaviour.
     */
    public FormatCharExtractor(String marcFilenameHint) {
        this.marcFilenameHint = marcFilenameHint;
    }

    /**
     * Convenience factory that reads the filename hint from the system property
     * {@value #SYSTEM_PROPERTY_KEY}, matching the CoAlliance indexer's CLI invocation
     * convention. Returns an extractor with a {@code null} hint if the property is unset.
     */
    public static FormatCharExtractor fromSystemProperty() {
        return new FormatCharExtractor(System.getProperty(SYSTEM_PROPERTY_KEY));
    }

    /**
     * Returns {@code "e"} if the record is electronic, {@code "p"} otherwise.
     *
     * @param record the MARC record (must not be null)
     */
    public String extract(Record record) {
        if (filenameSuggestsPrint()) {
            return PRINT;
        }
        if (filenameSuggestsElectronic()
                || hasPreRDAElectronicMarker(record)
                || hasRDAElectronicMarker(record)
                || isElectronicGovDoc(record)) {
            return ELECTRONIC;
        }
        return PRINT;
    }

    private boolean hasPreRDAElectronicMarker(Record record) {
        if (subfieldContains(record, "245", 'h', "electronic resource")) return true;
        if (subfieldContains(record, "590", 'a', "electronic reproduction")) return true;
        if (subfieldContains(record, "533", 'a', "electronic reproduction")) return true;
        if (subfieldContains(record, "300", 'a', "online resource")) return true;

        for (ControlField cf : (List<ControlField>) record.getControlFields()) {
            if ("007".equals(cf.getTag())
                    && cf.getData() != null
                    && cf.getData().toUpperCase().startsWith("C")) {
                return true;
            }
        }

        // Leader/06 (type of record) == 'm' => computer file / electronic resource.
        // Matches getFormat_pre_RDA: leader chars 6-7 uppercased, startsWith "M".
        // Guarded on length >= 10 exactly as the indexer is. Without this, online
        // audio/video records (Leader/06='m', no 245$h, 007 not starting 'C') are
        // mis-tagged 'p'.
        String leader = record.getLeader() == null ? null : record.getLeader().toString();
        if (leader != null && leader.length() >= 10
                && leader.substring(6, 8).toUpperCase().startsWith("M")) {
            return true;
        }
        return false;
    }

    private boolean hasRDAElectronicMarker(Record record) {
        DataField f337 = (DataField) record.getVariableField("337");
        if (f337 == null || f337.getSubfield('a') == null) return false;
        String a = f337.getSubfield('a').getData();
        return a != null && a.trim().toLowerCase().startsWith("c");
    }

    private boolean isElectronicGovDoc(Record record) {
        return record.getVariableField("086") != null
            && record.getVariableField("856") != null;
    }

    private boolean filenameSuggestsElectronic() {
        if (marcFilenameHint == null) return false;
        String lower = marcFilenameHint.toLowerCase();
        return lower.contains("electronic") || lower.contains("ebook");
    }

    private boolean filenameSuggestsPrint() {
        if (marcFilenameHint == null) return false;
        String lower = marcFilenameHint.toLowerCase();
        return lower.contains("physical") || lower.contains("print");
    }

    private static boolean subfieldContains(Record record, String tag, char code, String needle) {
        DataField field = (DataField) record.getVariableField(tag);
        if (field == null || field.getSubfield(code) == null) return false;
        String value = field.getSubfield(code).getData();
        return value != null && value.toLowerCase().contains(needle);
    }
}
