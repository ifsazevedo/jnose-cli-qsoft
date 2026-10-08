/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * Test smell detection by JNose (Virgínio et al., SBES 2020). See README.md for credits and usage.
 */

import io.github.arieslab.dto.TestClass;
import io.github.arieslab.dto.TestSmell;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The two CSV reports, equivalent to the JNose "By TestClass" and "By TestSmells" exports.
 */
final class CsvReports {

    static final String BY_CLASS_SUFFIX = "_result_byclasstest.csv";
    static final String BY_SMELL_SUFFIX = "_result_bytestsmells.csv";

    static final List<String> BY_CLASS_HEADER = concat(
            List.of("App", "TestFileName", "PathFile", "ProductionFileName", "LOC", "numberMethods"),
            SmellAnalyzer.SMELL_NAMES);
    static final List<String> BY_SMELL_HEADER =
            List.of("App", "TestClass", "PathFile", "ProductionFileName", "TestSmell", "Method", "Lines");

    private CsvReports() {
    }

    /** Writes one row per test class, with the number of occurrences of each smell. */
    static void writeByClass(List<TestClass> classes, Path project, Path file, PrintStream err) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (TestClass tc : classes) {
            Map<String, Integer> counts = tc.getLineSumTestSmells();
            warnAboutUnknownSmells(counts, tc, err);
            List<String> row = new ArrayList<>(classColumns(tc, project));
            row.add(String.valueOf(tc.getNumberLine()));
            row.add(String.valueOf(tc.getNumberMethods()));
            SmellAnalyzer.SMELL_NAMES.forEach(smell -> row.add(String.valueOf(counts.getOrDefault(smell, 0))));
            rows.add(row);
        }
        writeCsv(file, BY_CLASS_HEADER, rows);
    }

    /** Writes one row per smell occurrence; returns the number of occurrences written. */
    static int writeBySmell(List<TestClass> classes, Path project, Path file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (TestClass tc : classes) {
            for (TestSmell ts : tc.getListTestSmell()) {
                List<String> row = new ArrayList<>(classColumns(tc, project));
                row.addAll(List.of(ts.getName(), nz(ts.getMethod()), nz(ts.getRange())));
                rows.add(row);
            }
        }
        writeCsv(file, BY_SMELL_HEADER, rows);
        return rows.size();
    }

    /** Columns shared by both reports: project, test class, test file, production file. */
    static List<String> classColumns(TestClass tc, Path project) {
        return List.of(nz(tc.getProjectName()), nz(tc.getName()),
                rel(project, tc.getPathFile()), rel(project, tc.getProductionFile()));
    }

    /** Reports smells returned by jnose-core that have no column (e.g. after a library update). */
    static void warnAboutUnknownSmells(Map<String, Integer> counts, TestClass tc, PrintStream err) {
        for (String smell : counts.keySet()) {
            if (!SmellAnalyzer.SMELL_NAMES.contains(smell)) {
                err.println("Warning: unknown smell '" + smell + "' in " + tc.getName() + " (not in the report columns)");
            }
        }
    }

    static void writeCsv(Path file, List<String> header, List<List<String>> rows) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            out.println(csv(header));
            rows.forEach(row -> out.println(csv(row)));
        }
    }

    /** One CSV line (RFC 4180): cells with commas, quotes or line breaks are quoted. */
    static String csv(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            String c = nz(cells.get(i));
            if (c.contains(",") || c.contains("\"") || c.contains("\n") || c.contains("\r")) {
                c = "\"" + c.replace("\"", "\"\"") + "\"";
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Path relative to the project folder, for readability. */
    static String rel(Path project, String file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            return project.relativize(Path.of(file).toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) {
            return file;
        }
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }

    static List<String> concat(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }
}
