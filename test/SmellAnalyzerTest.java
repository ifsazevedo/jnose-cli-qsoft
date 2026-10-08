/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * See README.md for credits and usage.
 */

import io.github.arieslab.dto.TestClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the bridge to jnose-core. */
class SmellAnalyzerTest {

    /** jnose-core 0.9.4 detects 21 test smells. */
    private static final int SMELLS_IN_JNOSE_CORE = 21;

    private static final int NOTHING_PRINTED = 0;

    @TempDir
    Path tempDir;

    @Test
    void allSmellsOfJnoseCoreAreKnown() {
        assertEquals(SMELLS_IN_JNOSE_CORE, SmellAnalyzer.SMELL_NAMES.size(), "jnose-core 0.9.4 defines 21 smells");
    }

    @Test
    void analyseFindsTheSampleTestClass() throws Exception {
        List<TestClass> classes = SmellAnalyzer.analyse(SampleProject.create(tempDir));
        assertEquals(List.of(SampleProject.SMELLY_TEST_CLASS), classes.stream().map(TestClass::getName).toList(),
                "the only test class of the sample project is found");
    }

    @Test
    void analyseDetectsPlantedSleepyTest() throws Exception {
        TestClass counterTest = SmellAnalyzer.analyse(SampleProject.create(tempDir)).getFirst();
        assertTrue(counterTest.getListTestSmell().stream().anyMatch(s -> "Sleepy Test".equals(s.getName())),
                "Thread.sleep in a test is reported as Sleepy Test");
    }

    @Test
    void analyseReturnsNoClassesForEmptyFolder() throws Exception {
        assertTrue(SmellAnalyzer.analyse(tempDir).isEmpty(), "an empty folder has no test classes");
    }

    @Test
    void libraryWarningsArePrintedOnOneLine() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        LogRecord warning = new LogRecord(Level.WARNING, "isTestFile: error parsing file");
        warning.setThrown(new IllegalStateException("details"));
        new SmellAnalyzer.OneLineHandler(new PrintStream(err)).publish(warning);
        assertEquals("jnose-core: isTestFile: error parsing file (IllegalStateException)" + System.lineSeparator(),
                new String(err.toByteArray(), StandardCharsets.UTF_8), "message and exception type, no stack trace");
    }

    @Test
    void libraryInfoMessagesAreHidden() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        new SmellAnalyzer.OneLineHandler(new PrintStream(err)).publish(new LogRecord(Level.INFO, "getFilesTest"));
        assertEquals(NOTHING_PRINTED, err.size(), "INFO messages are not printed");
    }
}
