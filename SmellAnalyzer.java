/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * Test smell detection by JNose (Virgínio et al., SBES 2020). See README.md for credits and usage.
 */

import io.github.arieslab.core.Config;
import io.github.arieslab.core.JNoseCore;
import io.github.arieslab.core.testsmelldetector.testsmell.TestSmellDetector;
import io.github.arieslab.dto.TestClass;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Bridge to jnose-core: which smells to detect, how the library logs, and
 * running the detection on a project folder.
 */
final class SmellAnalyzer {

    /** JNose default: a test with more statements than this is a Verbose Test. */
    static final int VERBOSE_TEST_MAX_STATEMENTS = 30;

    /** Names of the smells jnose-core detects, in its (alphabetical) order. */
    static final List<String> SMELL_NAMES = TestSmellDetector.getAllTestSmellNames();

    /** Package of jnose-core, used to configure its loggers. */
    static final String LIBRARY_LOGGER = "io.github.arieslab";

    /** All 21 smells enabled, with the JNose default threshold for Verbose Test. */
    static final Config ALL_SMELLS = new Config() {
        public boolean assertionRoulette() { return true; }
        public boolean conditionalTestLogic() { return true; }
        public boolean constructorInitialization() { return true; }
        public boolean defaultTest() { return true; }
        public boolean dependentTest() { return true; }
        public boolean duplicateAssert() { return true; }
        public boolean eagerTest() { return true; }
        public boolean emptyTest() { return true; }
        public boolean exceptionCatchingThrowing() { return true; }
        public boolean generalFixture() { return true; }
        public boolean mysteryGuest() { return true; }
        public boolean printStatement() { return true; }
        public boolean redundantAssertion() { return true; }
        public boolean sensitiveEquality() { return true; }
        public boolean verboseTest() { return true; }
        public boolean sleepyTest() { return true; }
        public boolean lazyTest() { return true; }
        public boolean unknownTest() { return true; }
        public boolean ignoredTest() { return true; }
        public boolean resourceOptimism() { return true; }
        public boolean magicNumberTest() { return true; }
        public int maxStatements() { return VERBOSE_TEST_MAX_STATEMENTS; }
    };

    /** The analysis itself failed (as opposed to a problem with the arguments or the reports). */
    static final class AnalysisException extends Exception {
        AnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private SmellAnalyzer() {
    }

    /** Runs jnose-core on the project and returns its test classes, sorted by file path. */
    static List<TestClass> analyse(Path project) throws AnalysisException {
        try {
            List<TestClass> classes = new ArrayList<>(new JNoseCore(ALL_SMELLS).getFilesTest(project.toString()));
            classes.sort(Comparator.comparing(TestClass::getPathFile));
            return classes;
        } catch (Exception e) {
            throw new AnalysisException("Analysis failed for " + project + ": " + e, e);
        }
    }

    /** Shows jnose-core warnings as one-line messages instead of stack traces; hides its INFO messages. */
    static void configureLibraryLogging(PrintStream err) {
        Logger library = Logger.getLogger(LIBRARY_LOGGER);
        library.setUseParentHandlers(false);
        library.setLevel(Level.WARNING);
        for (Handler h : library.getHandlers()) {
            library.removeHandler(h);
        }
        library.addHandler(new OneLineHandler(err));
    }

    /** Prints each log record as a single line: "jnose-core: message (ExceptionType)". */
    static final class OneLineHandler extends Handler {
        private final PrintStream err;

        OneLineHandler(PrintStream err) {
            this.err = err;
        }

        @Override
        public void publish(LogRecord r) {
            if (r.getLevel().intValue() < Level.WARNING.intValue()) {
                return;
            }
            String cause = r.getThrown() == null ? "" : " (" + r.getThrown().getClass().getSimpleName() + ")";
            err.println("jnose-core: " + r.getMessage() + cause);
        }

        @Override
        public void flush() {
            err.flush();
        }

        @Override
        public void close() {
        }
    }
}
