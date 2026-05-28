/*
 * Copyright 2026 Colorado Alliance of Research Libraries
 * SPDX-License-Identifier: Apache-2.0
 */
package org.coalliance.matchkey.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchKeyCliTest {

    private final MarcFactory marcFactory = MarcFactory.newInstance();

    @Test
    @DisplayName("CLI emits one tab-separated 'id\\tmatchkey' line per record")
    void writesOneLinePerRecord(@TempDir Path tmp) throws Exception {
        Path marc = tmp.resolve("sample.marc");
        try (FileOutputStream fos = new FileOutputStream(marc.toFile())) {
            MarcStreamWriter writer = new MarcStreamWriter(fos);
            writer.write(makeRecord("rec1", "First Title"));
            writer.write(makeRecord("rec2", "Second Title"));
            writer.close();
        }

        String output = runCli(marc.toString());
        String[] lines = output.trim().split("\n");

        assertEquals(2, lines.length, "Expected 2 output lines, got: " + output);
        assertTrue(lines[0].startsWith("rec1\t"), "Line 0: " + lines[0]);
        assertTrue(lines[1].startsWith("rec2\t"), "Line 1: " + lines[1]);
        assertEquals(188, lines[0].split("\t")[1].length(),
                "matchkey width on line 0: " + lines[0]);
    }

    private Record makeRecord(String controlNumber, String title) {
        Record r = marcFactory.newRecord("02801nam a22005052u 4500");
        r.addVariableField(marcFactory.newControlField("001", controlNumber));
        DataField f245 = marcFactory.newDataField("245", '1', '0');
        f245.addSubfield(marcFactory.newSubfield('a', title));
        r.addVariableField(f245);
        return r;
    }

    private String runCli(String path) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(buf));
            MatchKeyCli.main(new String[]{path});
        } finally {
            System.setOut(originalOut);
        }
        return buf.toString();
    }
}
