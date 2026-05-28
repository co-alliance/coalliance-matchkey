/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuation;
import static org.coalliance.matchkey.util.PuncuationStripper.stripPuncuationSpace;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PuncuationStripperTest {

    @Test
    @DisplayName("null input returns empty string")
    void nullInput() {
        assertEquals("", stripPuncuation(null));
    }

    @Test
    @DisplayName("empty input returns empty string")
    void emptyInput() {
        assertEquals("", stripPuncuation(""));
    }

    @Test
    @DisplayName("punctuation replaced with underscore by default (trailing punct becomes trailing _)")
    void replacesPunctuation() {
        assertEquals("EP_1_1_5_", stripPuncuation("EP 1.1/5:"));
    }

    @Test
    @DisplayName("leading 'The ' is stripped")
    void stripsLeadingThe() {
        assertEquals("law_of_primitive_man", stripPuncuation("The law of primitive man"));
    }

    @Test
    @DisplayName("leading 'A ' is stripped")
    void stripsLeadingA() {
        assertEquals("history_of_Rome", stripPuncuation("A history of Rome"));
    }

    @Test
    @DisplayName("leading 'An ' is stripped")
    void stripsLeadingAn() {
        assertEquals("apple", stripPuncuation("An apple"));
    }

    @Test
    @DisplayName("ampersand becomes 'and'")
    void ampersandBecomesAnd() {
        assertEquals("blackandwhite", stripPuncuation("black&white"));
    }

    @Test
    @DisplayName("apostrophes are removed entirely (not replaced)")
    void apostropheRemoved() {
        assertEquals("dont", stripPuncuation("don't"));
    }

    @Test
    @DisplayName("runs of the replacement character collapse to one")
    void collapseRuns() {
        assertEquals("x_y", stripPuncuation("x   y"));
    }

    @Test
    @DisplayName("leading 'a ' is greedy — eats all following whitespace too")
    void leadingArticleIsGreedy() {
        assertEquals("b", stripPuncuation("a   b"));
    }

    @Test
    @DisplayName("stripPuncuationSpace replaces with spaces and lets trim() finish trailing punctuation")
    void spaceVariantTrailingPunctuation() {
        assertEquals("law of primitive man",
                stripPuncuationSpace("law of primitive man /").trim());
    }
}
