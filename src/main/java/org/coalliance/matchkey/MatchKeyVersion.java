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
    public static final String VERSION = "_v07312026";

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
