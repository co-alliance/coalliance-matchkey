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
    public static final String VERSION = "_v03182026";
}
