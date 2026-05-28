/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.cli;

import org.coalliance.matchkey.MatchKeyGenerator;
import org.coalliance.matchkey.fields.FormatCharExtractor;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a binary MARC file and prints one {@code recordId\tmatchKey} line per
 * record on stdout. Intended for verifying that the library produces the same
 * matchKey as the embedded MarcImporter code: index a MARC file via the legacy
 * indexer, run this CLI on the same file, then diff the two columns.
 *
 * <p>Usage:
 * <pre>
 *   java -cp matchkey.jar:marc4j-2.9.6.jar \
 *        org.coalliance.matchkey.cli.MatchKeyCli &lt;file.marc&gt;
 * </pre>
 *
 * <p>The MARC filename is passed to {@link FormatCharExtractor} as a hint by
 * default (matching the indexer's convention of using the filename to override
 * format detection). Override with {@code -Dorg.coalliance.indexing.fileName=...}.
 *
 * <p>Record ID is taken from MARC control field 001; records with no 001 are
 * emitted with an empty id column.
 */
public final class MatchKeyCli {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: MatchKeyCli <file.marc>");
            System.exit(1);
        }

        File marcFile = new File(args[0]);
        if (!marcFile.isFile()) {
            System.err.println("Not a file: " + marcFile);
            System.exit(1);
        }

        String hint = System.getProperty(
                FormatCharExtractor.SYSTEM_PROPERTY_KEY, marcFile.getName());
        MatchKeyGenerator generator = new MatchKeyGenerator(hint);

        try (InputStream in = new FileInputStream(marcFile)) {
            MarcReader reader = new MarcStreamReader(in);
            while (reader.hasNext()) {
                Record record = reader.next();
                String id = record.getControlNumber();
                String mk = generator.generate(record);
                System.out.println((id == null ? "" : id) + "\t" + mk);
            }
        }
    }
}
