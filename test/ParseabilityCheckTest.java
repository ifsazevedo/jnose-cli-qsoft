/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * See README.md for credits and usage.
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the detection of files that jnose-core cannot parse. */
class ParseabilityCheckTest {

    @TempDir
    Path tempDir;

    private Path project;

    @BeforeEach
    void createSampleProject() throws IOException {
        project = SampleProject.create(tempDir);
    }

    @Test
    void java8ProjectHasNoUnparseableFiles() throws IOException {
        assertTrue(ParseabilityCheck.findUnparseableFiles(project).isEmpty(), "the Java 8 sample is fully parseable");
    }

    @Test
    void reportsFileWithTextBlock() throws IOException {
        Path modern = SampleProject.addModernTest(project);
        assertEquals(List.of(modern), ParseabilityCheck.findUnparseableFiles(project), "the text block file is reported");
    }

    @Test
    void ignoresFilesInBuildFolders() throws IOException {
        Path generated = Files.createDirectories(project.resolve("target/generated"));
        Path modern = generated.resolve(SampleProject.MODERN_TEST_FILE);
        Files.writeString(modern, SampleProject.MODERN_TEST);
        assertFalse(ParseabilityCheck.findUnparseableFiles(project).contains(modern),
                "files under target/ are not analysed by jnose-core, so they are not reported");
    }

    @Test
    void recognisesSkippedFolderAnywhereInPath() {
        assertTrue(ParseabilityCheck.isInSkippedFolder(Path.of("module/build/Generated.java")),
                "a build folder inside a module is skipped");
    }

    @Test
    void doesNotSkipSourceFolders() {
        assertFalse(ParseabilityCheck.isInSkippedFolder(Path.of("src/test/java/demo/CounterTest.java")),
                "source folders are analysed");
    }
}
