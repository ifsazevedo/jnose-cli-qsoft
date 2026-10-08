/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * See README.md for credits and usage.
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test fixture: a tiny Java 8 project whose test class has exactly four smells: one Sleepy Test
 * and one Eager Test (incrementsAfterWaiting calls Thread.sleep and two Counter methods), one
 * Empty Test and one Ignored Test. An optional test file uses Java 15 syntax.
 */
final class SampleProject {

    static final String NAME = "sample";
    static final String SMELLY_TEST_CLASS = "CounterTest";
    static final String TEST_FOLDER = "src/test/java/demo";
    static final String MODERN_TEST_FILE = "ModernTest.java";

    /** Production class. */
    static final String COUNTER = String.join("\n",
            "package demo;",
            "public class Counter {",
            "    private int value;",
            "    public void increment() { value++; }",
            "    public int value() { return value; }",
            "}");

    /** Test class with one Sleepy Test, one Eager Test, one Empty Test and one Ignored Test. */
    static final String COUNTER_TEST = String.join("\n",
            "package demo;",
            "import org.junit.Ignore;",
            "import org.junit.Test;",
            "import static org.junit.Assert.assertTrue;",
            "public class CounterTest {",
            "    @Test",
            "    public void incrementsAfterWaiting() throws Exception {",
            "        Counter counter = new Counter();",
            "        Thread.sleep(SHORT_DELAY);",
            "        counter.increment();",
            "        assertTrue(\"value should be positive\", counter.value() > ZERO);",
            "    }",
            "    @Test",
            "    public void notWrittenYet() {",
            "    }",
            "    @Ignore",
            "    @Test",
            "    public void disabled() {",
            "        assertTrue(\"always true\", true);",
            "    }",
            "    private static final long SHORT_DELAY = 10;",
            "    private static final int ZERO = 0;",
            "}");

    /** A test class using a Java 15 text block, which jnose-core's parser cannot read. */
    static final String MODERN_TEST = String.join("\n",
            "package demo;",
            "import org.junit.Test;",
            "public class ModernTest {",
            "    @Test",
            "    public void usesTextBlock() {",
            "        String s = \"\"\"",
            "            hello",
            "            \"\"\";",
            "    }",
            "}");

    private SampleProject() {
    }

    /** Creates the sample project inside the given folder and returns the project folder. */
    static Path create(Path parent) throws IOException {
        Path project = parent.resolve(NAME);
        Path main = Files.createDirectories(project.resolve("src/main/java/demo"));
        Path test = Files.createDirectories(project.resolve(TEST_FOLDER));
        Files.writeString(main.resolve("Counter.java"), COUNTER);
        Files.writeString(test.resolve("CounterTest.java"), COUNTER_TEST);
        return project;
    }

    /** Adds the test file with Java 15 syntax to an existing sample project and returns its path. */
    static Path addModernTest(Path project) throws IOException {
        Path file = project.resolve(TEST_FOLDER).resolve(MODERN_TEST_FILE);
        Files.writeString(file, MODERN_TEST);
        return file;
    }
}
