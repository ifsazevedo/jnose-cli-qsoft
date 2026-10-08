/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * Test smell detection by JNose (Virgínio et al., SBES 2020). See README.md for credits and usage.
 */

import com.github.javaparser.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finds the Java files that jnose-core cannot read. jnose-core parses code with
 * JavaParser 3.3.5 (Java syntax up to Java 13) and silently skips files with
 * newer syntax (text blocks, switch expressions, pattern matching, top-level
 * records, sealed classes), so their smells would be missing from the reports.
 */
final class ParseabilityCheck {

    /** Folders that jnose-core does not analyse (same list as jnose-core 0.9.4). */
    static final Set<String> SKIPPED_FOLDERS =
            Set.of("target", "build", "classes", ".git", "node_modules", "out", "bin", "dist");

    private ParseabilityCheck() {
    }

    /** Java files of the project (outside skipped folders) that the parser cannot read, sorted by path. */
    static List<Path> findUnparseableFiles(Path project) throws IOException {
        List<Path> unparseable = new ArrayList<>();
        for (Path file : javaFiles(project)) {
            if (!isParseable(file)) {
                unparseable.add(file);
            }
        }
        return unparseable;
    }

    static List<Path> javaFiles(Path project) throws IOException {
        try (Stream<Path> files = Files.walk(project)) {
            return files.filter(f -> f.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .filter(f -> !isInSkippedFolder(project.relativize(f)))
                    .sorted()
                    .toList();
        }
    }

    static boolean isParseable(Path file) {
        try {
            JavaParser.parse(file);
            return true;
        } catch (Exception | StackOverflowError e) {
            return false;
        }
    }

    static boolean isInSkippedFolder(Path relativePath) {
        for (Path part : relativePath) {
            if (SKIPPED_FOLDERS.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }
}
