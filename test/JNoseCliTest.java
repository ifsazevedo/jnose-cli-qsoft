/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * See README.md for credits and usage.
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the command-line interface: arguments, exit codes and messages. */
class JNoseCliTest {

    private static final PrintStream SILENT = new PrintStream(OutputStream.nullOutputStream());

    @TempDir
    Path tempDir;

    private Path project;
    private Path reports;

    @BeforeEach
    void createSampleProject() throws IOException {
        project = SampleProject.create(tempDir);
        reports = tempDir.resolve("reports");
    }

    @Test
    void parseArgsRejectsMissingProject() {
        JNoseCli.CliException e = assertThrows(JNoseCli.CliException.class, () -> JNoseCli.parseArgs(new String[0]));
        assertEquals(JNoseCli.EXIT_USAGE, e.exitCode, "no arguments is a usage error");
    }

    @Test
    void parseArgsRejectsTooManyArguments() {
        String[] args = {"a", "b", "c"};
        JNoseCli.CliException e = assertThrows(JNoseCli.CliException.class, () -> JNoseCli.parseArgs(args));
        assertEquals(JNoseCli.EXIT_USAGE, e.exitCode, "three arguments is a usage error");
    }

    @Test
    void parseArgsRejectsFolderThatDoesNotExist() {
        String[] args = {String.valueOf(tempDir.resolve("missing"))};
        JNoseCli.CliException e = assertThrows(JNoseCli.CliException.class, () -> JNoseCli.parseArgs(args));
        assertEquals(JNoseCli.EXIT_USAGE, e.exitCode, "a missing folder is a usage error");
    }

    @Test
    void runSucceedsOnSampleProject() {
        int exitCode = JNoseCli.run(args(project), SILENT, SILENT);
        assertEquals(JNoseCli.EXIT_OK, exitCode, "the sample project is analysed successfully");
    }

    @Test
    void runFailsWhenProjectHasNoTests() throws IOException {
        Path empty = Files.createDirectories(tempDir.resolve("empty"));
        int exitCode = JNoseCli.run(args(empty), SILENT, SILENT);
        assertEquals(JNoseCli.EXIT_NO_TESTS, exitCode, "a project without tests ends with EXIT_NO_TESTS");
    }

    @Test
    void runWritesBothReports() throws IOException {
        JNoseCli.run(args(project), SILENT, SILENT);
        assertEquals(List.of(CsvReports.BY_CLASS_SUFFIX, CsvReports.BY_SMELL_SUFFIX), reportSuffixes(),
                "both CSV reports are written to the output folder");
    }

    @Test
    void reportNamesStartWithProjectNameAndTimestamp() throws IOException {
        JNoseCli.run(args(project), SILENT, SILENT);
        assertTrue(reportNames().stream().allMatch(n -> n.matches(SampleProject.NAME + "_\\d{8}-\\d{6}_result_.*")),
                "report names are <project>_<yyyyMMdd-HHmmss>_result_...: " + reportNames());
    }

    @Test
    void runWarnsAboutSkippedFiles() throws IOException {
        SampleProject.addModernTest(project);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        JNoseCli.run(args(project), SILENT, new PrintStream(err));
        String warning = new String(err.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(warning.contains(SampleProject.MODERN_TEST_FILE), "the skipped file is named in the warning");
    }

    private List<String> reportNames() throws IOException {
        try (Stream<Path> files = Files.list(reports)) {
            return files.map(f -> String.valueOf(f.getFileName())).sorted().toList();
        }
    }

    /** Suffixes of the reports written, in the order byclasstest, bytestsmells. */
    private List<String> reportSuffixes() throws IOException {
        return reportNames().stream().map(n -> n.substring(n.indexOf("_result_"))).toList();
    }

    private String[] args(Path projectFolder) {
        return new String[] {String.valueOf(projectFolder), String.valueOf(reports)};
    }
}
