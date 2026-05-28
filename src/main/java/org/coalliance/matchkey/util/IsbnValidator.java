/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 *
 * Portions vendored from solrmarc-marc4j (org.solrmarc.tools.Utils.returnValidISBNs).
 * See LICENSE-solrmarc at the repo root. Original notice preserved below:
 *
 *     Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 */
package org.coalliance.matchkey.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filters a set of ISBN-candidate strings, returning only the syntactically
 * valid ISBNs (with trailing text trimmed away).
 *
 * <p>Recognises 10-digit ISBNs (with check digit 0-9 or X) and 13-digit ISBNs
 * starting with 978 or 979. Adapted directly from
 * {@code org.solrmarc.tools.Utils.returnValidISBNs}.
 */
public final class IsbnValidator {

    private IsbnValidator() {}

    private static final Pattern P10    = Pattern.compile("^\\d{9}[\\dX].*");
    private static final Pattern P13    = Pattern.compile("^(978|979)\\d{9}[X\\d].*");
    private static final Pattern P13ANY = Pattern.compile("^\\d{12}[X\\d].*");

    public static Set<String> returnValidISBNs(Set<String> candidates) {
        Set<String> isbns = new LinkedHashSet<>();
        for (String raw : candidates) {
            String value = raw.trim();
            if (P13.matcher(value).matches()) {
                isbns.add(value.substring(0, 13));
            } else if (P10.matcher(value).matches() && !P13ANY.matcher(value).matches()) {
                isbns.add(value.substring(0, 10));
            }
        }
        return isbns;
    }
}
