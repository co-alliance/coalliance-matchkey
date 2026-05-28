/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey;

import org.coalliance.matchkey.fields.AuthorExtractor;
import org.coalliance.matchkey.fields.EditionExtractor;
import org.coalliance.matchkey.fields.FormatCharExtractor;
import org.coalliance.matchkey.fields.LeaderTypeExtractor;
import org.coalliance.matchkey.fields.PaginationExtractor;
import org.coalliance.matchkey.fields.PublicationYearExtractor;
import org.coalliance.matchkey.fields.PublisherExtractor;
import org.coalliance.matchkey.fields.TitleDatesExtractor;
import org.coalliance.matchkey.fields.TitleExtractor;
import org.coalliance.matchkey.fields.TitleNumberExtractor;
import org.coalliance.matchkey.fields.TitlePartExtractor;
import org.marc4j.marc.Record;

/**
 * Generates a Gold Rush matchKey from a MARC bibliographic record.
 *
 * <p>The matchKey is a fixed-width string assembled from 11 record-derived
 * sections, a disabled GMD placeholder, a version string, and a 1-character
 * format marker. Section order, widths, and post-processing all match the
 * production indexer ({@code CoAllianceIndexUtil.iiiMatchKey}).
 *
 * <p>Section layout (total width: 188 characters):
 * <pre>
 *   95  Title              (245 $a $b $p, or linked 880, with non-Roman fallbacks)
 *    5  GMD (disabled)     constant "_____"
 *    4  Publication Year   (008 / 264$c / 260$c)
 *    4  Pagination         (300$a)
 *    3  Edition            (250$a with 1st-edition Book default)
 *    5  Publisher          (264$b / 260$b)
 *    1  Leader Type        (Leader position 6)
 *   30  Title Part         (multi-$p, 9 chars each)
 *   10  Title Number       (245$n)
 *    5  Author             (100$a / 110$a / 111$a / 130$a)
 *   15  Title Dates        (245$f)
 *   10  Version            ({@link MatchKeyVersion#VERSION})
 *    1  Format Character   ('e' or 'p', filename-hint aware)
 * </pre>
 *
 * <p>Post-processing: lowercase, then replace each space with {@code '_'}.
 * Stateless apart from the immutable filename hint inherited by the underlying
 * {@link FormatCharExtractor}; safe to reuse across threads.
 */
public final class MatchKeyGenerator {

    /** Disabled GMD section. Always 5 underscores since 11-15-22. */
    private static final String GMD_DISABLED = "_____";

    private final TitleExtractor           title           = new TitleExtractor();
    private final PublicationYearExtractor publicationYear = new PublicationYearExtractor();
    private final PaginationExtractor      pagination      = new PaginationExtractor();
    private final EditionExtractor         edition         = new EditionExtractor();
    private final PublisherExtractor       publisher       = new PublisherExtractor();
    private final LeaderTypeExtractor      leaderType      = new LeaderTypeExtractor();
    private final TitlePartExtractor       titlePart       = new TitlePartExtractor();
    private final TitleNumberExtractor     titleNumber     = new TitleNumberExtractor();
    private final AuthorExtractor          author          = new AuthorExtractor();
    private final TitleDatesExtractor      titleDates      = new TitleDatesExtractor();
    private final FormatCharExtractor      formatChar;

    /**
     * @param marcFilenameHint optional MARC filename used for format detection,
     *                         or {@code null} for pure-MARC behaviour. See
     *                         {@link FormatCharExtractor} for the override rules.
     */
    public MatchKeyGenerator(String marcFilenameHint) {
        this.formatChar = new FormatCharExtractor(marcFilenameHint);
    }

    /**
     * Convenience factory matching the CoAlliance indexer's CLI convention:
     * reads the filename hint from the {@code org.coalliance.indexing.fileName}
     * system property (or null if unset).
     */
    public static MatchKeyGenerator fromSystemProperty() {
        return new MatchKeyGenerator(System.getProperty(FormatCharExtractor.SYSTEM_PROPERTY_KEY));
    }

    /** Generates the 188-character matchKey for {@code record}. */
    public String generate(Record record) {
        StringBuilder mk = new StringBuilder(188);
        mk.append(title.extract(record));
        mk.append(GMD_DISABLED);
        mk.append(publicationYear.extract(record));
        mk.append(pagination.extract(record));
        mk.append(edition.extract(record));
        mk.append(publisher.extract(record));
        mk.append(leaderType.extract(record));
        mk.append(titlePart.extract(record));
        mk.append(titleNumber.extract(record));
        mk.append(author.extract(record));
        mk.append(titleDates.extract(record));
        mk.append(MatchKeyVersion.VERSION);
        mk.append(formatChar.extract(record));

        return mk.toString().toLowerCase().replace(' ', '_');
    }
}
