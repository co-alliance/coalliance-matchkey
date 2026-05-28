/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

/**
 * Shared character and string constants used across the matchKey algorithm.
 *
 * <p>Broken out as named constants so extractors can statically import them and
 * avoid scattering magic characters through the code. New constants should be
 * added here only when they are referenced from more than one extractor.
 */
public final class Constants {

    private Constants() {}

    public static final char SPACE      = ' ';
    public static final char UNDERSCORE = '_';
}
