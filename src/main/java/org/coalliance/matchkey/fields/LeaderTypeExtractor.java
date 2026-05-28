/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.fields;

import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;

import static org.coalliance.matchkey.util.Constants.UNDERSCORE;

/**
 * Extracts the 1-character "Type of" component of the matchKey from the MARC Leader.
 *
 * <p>The character at Leader position 6 is returned when the leader is at least
 * 10 characters long; otherwise an underscore is returned. Output width: 1 character.
 *
 * <p>Specification name: "Type of" — see {@code docs/CoAlliance_Match_Key.md}.
 * Stateless and thread-safe.
 */
public final class LeaderTypeExtractor {

    /**
     * Returns the 1-character matchKey component derived from the MARC Leader.
     *
     * @param record the MARC record (must not be null)
     * @return the character at Leader position 6, or "_" if the leader is too short
     */
    public String extract(Record record) {
        Leader leader = record.getLeader();
        String leaderStr = leader.toString();
        if (leaderStr != null && leaderStr.length() >= 10) {
            return leaderStr.substring(6, 7);
        }
        return String.valueOf(UNDERSCORE);
    }
}
