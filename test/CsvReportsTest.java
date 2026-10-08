/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * See README.md for credits and usage.
 */

import io.github.arieslab.dto.TestClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the CSV reports and of the CSV formatting. */
class CsvReportsTest {

    private static final PrintStream SILENT = new PrintStream(OutputStream.nullOutputStream());
    private static final String ONE_OCCURRENCE = "1";
    /** Sleepy Test, Eager Test, Empty Test and Ignored Test (see SampleProject). */
    private static final int SMELLS_IN_SAMPLE = 4;

    /** 8 October 2026, 18:40:12. */
    private static final LocalDateTime RUN_TIME = LocalDateTime.of(2026, 10, 8, 18, 40, 12);

    @TempDir
    Path tempDir;

    private Path project;
    private List<TestClass> classes;

    @BeforeEach
    void analyseSampleProject() throws Exception {
        project = SampleProject.create(tempDir);
        classes = SmellAnalyzer.analyse(project);
    }

    // --- CSV formatting ---------------------------------------------------

    @Test
    void csvLeavesPlainCellsUnquoted() {
        assertEquals("a,b,c", CsvReports.csv(List.of("a", "b", "c")), "plain cells are joined with commas");
    }

    @Test
    void csvQuotesCellsContainingCommas() {
        assertEquals("x,\"20, 23\"", CsvReports.csv(List.of("x", "20, 23")), "a cell with a comma is quoted");
    }

    @Test
    void csvDoublesQuotesInsideCells() {
        assertEquals("\"say \"\"hi\"\"\"", CsvReports.csv(List.of("say \"hi\"")), "inner quotes are doubled");
    }

    @Test
    void csvWritesNullAsEmptyCell() {
        assertEquals("a,", CsvReports.csv(Arrays.asList("a", null)), "null becomes an empty cell");
    }

    // --- File names ---------------------------------------------------------

    @Test
    void fileNameHasProjectDateTimeAndSuffix() {
        assertEquals("sample_20261008-184012_result_byclasstest.csv",
                CsvReports.fileName(SampleProject.NAME, RUN_TIME, CsvReports.BY_CLASS_SUFFIX),
                "project, yyyyMMdd-HHmmss and the report suffix");
    }

    // --- Paths --------------------------------------------------------------

    @Test
    void relReturnsPathRelativeToProject() {
        Path relative = Path.of("src/test/java/demo/CounterTest.java");
        String shown = CsvReports.rel(project, String.valueOf(project.resolve(relative)));
        assertEquals(relative, Path.of(shown), "paths are shown relative to the project folder");
    }

    @Test
    void relReturnsEmptyWhenThereIsNoProductionFile() {
        assertEquals("", CsvReports.rel(project, ""), "a missing production file gives an empty cell");
    }

    // --- Report by test class -------------------------------------------------

    @Test
    void byClassHeaderEndsWithOneColumnPerSmell() {
        List<String> header = CsvReports.BY_CLASS_HEADER;
        assertEquals(SmellAnalyzer.SMELL_NAMES, header.subList(header.size() - SmellAnalyzer.SMELL_NAMES.size(),
                header.size()), "the last columns are the smells defined by jnose-core");
    }

    @Test
    void byClassReportCountsSleepyTestOnce() throws IOException {
        assertEquals(ONE_OCCURRENCE, byClassCount("Sleepy Test"), "Sleepy Test should be counted once");
    }

    @Test
    void byClassReportCountsEmptyTestOnce() throws IOException {
        assertEquals(ONE_OCCURRENCE, byClassCount("EmptyTest"), "EmptyTest should be counted once");
    }

    @Test
    void byClassReportCountsIgnoredTestOnce() throws IOException {
        assertEquals(ONE_OCCURRENCE, byClassCount("IgnoredTest"), "IgnoredTest should be counted once");
    }

    // --- Report by test smell -------------------------------------------------

    @Test
    void bySmellReportReturnsNumberOfOccurrences() throws IOException {
        Path file = tempDir.resolve("bysmell.csv");
        assertEquals(SMELLS_IN_SAMPLE, CsvReports.writeBySmell(classes, project, file), "one row per smell in the sample");
    }

    @Test
    void bySmellReportListsSleepyTest() throws IOException {
        assertTrue(bySmellReport().contains("Sleepy Test,incrementsAfterWaiting"), "Sleepy Test should be listed");
    }

    @Test
    void bySmellReportListsEmptyTest() throws IOException {
        assertTrue(bySmellReport().contains("EmptyTest,notWrittenYet"), "EmptyTest should be listed");
    }

    @Test
    void bySmellReportListsIgnoredTest() throws IOException {
        assertTrue(bySmellReport().contains("IgnoredTest,disabled"), "IgnoredTest should be listed");
    }

    // --- Helpers ----------------------------------------------------------------

    /** Writes the by-class report and returns the count of a smell for the sample test class. */
    private String byClassCount(String smell) throws IOException {
        Path file = tempDir.resolve("byclass.csv");
        CsvReports.writeByClass(classes, project, file, SILENT);
        List<String> lines = Files.readAllLines(file);
        List<String> header = List.of(lines.getFirst().split(","));
        List<String> row = lines.stream().skip(1)
                .map(line -> List.of(line.split(",")))
                .filter(cells -> cells.get(header.indexOf("TestFileName")).equals(SampleProject.SMELLY_TEST_CLASS))
                .findFirst().orElseThrow();
        return row.get(header.indexOf(smell));
    }

    /** Writes the by-smell report and returns its content. */
    private String bySmellReport() throws IOException {
        Path file = tempDir.resolve("bysmell.csv");
        CsvReports.writeBySmell(classes, project, file);
        return Files.readString(file);
    }
}
