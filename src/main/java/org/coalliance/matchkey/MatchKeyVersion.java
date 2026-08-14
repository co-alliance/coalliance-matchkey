/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey;

/**
 * The version string embedded in every generated matchKey, identifying which
 * algorithm version produced it.
 *
 * <p>This file is intentionally a single-constant class so that a future
 * algorithm change is a one-line, maximum-reviewability commit. Bumping the
 * version implies — and should always accompany — a real change in matchKey
 * output: behaviour changes belong together with their version bump.
 */
public final class MatchKeyVersion {

    private MatchKeyVersion() {}

    /** Format: {@code _vMMDDYYYY}. Width: 10 characters. */
    public static final String VERSION = "_v08142026";

    /**
     * 08-14-26 (_v08142026): two fixes, both reported by Ed Summers
     * (Stanford/POD) from the same 39.4M-record corpus that produced issue #1.
     *
     * <ol>
     *   <li>Turkish capital İ (U+0130) no longer inflates the key past 188.
     *       Sections are lowercased before their width is measured
     *       ({@link org.coalliance.matchkey.util.Padding}), because İ lowercases
     *       to two code points and the whole key is lowercased at the end.
     *       Affected 10,575 records of 39.4M, all Turkish or Ottoman, via the
     *       publisher section.</li>
     *   <li>The government-document publication year is validated before use
     *       ({@link org.coalliance.matchkey.fields.PublicationYearExtractor}).
     *       An 086$a record used to emit control field 008 date1 raw, so a
     *       malformed date1 shifted the key short and a 9999 placeholder was
     *       emitted verbatim even when date2 held a real year — a normal-length,
     *       silently unmatchable key.</li>
     * </ol>
     *
     * <p>Keys carrying _v07312026 remain comparable to _v08142026 keys for every
     * record except those two populations.
     */
    public static final String VERSION_V07312026 = "_v07312026";

    /**
     * 07-31-26 (_v07312026): an absent title (no 245) and an absent publisher
     * (no 264$b/260$b) now pad to their full section widths (95 and 5) instead
     * of emitting a zero-width section. Every matchKey is now exactly 188
     * characters, as the specification has always described. Through
     * _v03182026 those two cases produced short keys of 183, 93, or 88
     * characters. Keys carrying _v03182026 are NOT comparable to _v07312026
     * keys for records lacking a 245 or a publisher.
     *
     * @see <a href="https://github.com/co-alliance/coalliance-matchkey/issues/1">issue #1</a>
     */
    public static final String VERSION_V03182026 = "_v03182026";
}
