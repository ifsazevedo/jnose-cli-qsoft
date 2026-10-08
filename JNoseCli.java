/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Isabel Azevedo — QSOFT (Software Quality), MEI,
 * School of Engineering of the Technical University of Porto (ISEP)
 * Test smell detection by JNose (Virgínio et al., SBES 2020). See README.md for credits and usage.
 */

import io.github.arieslab.dto.TestClass;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line interface: reads the arguments, runs the analysis, writes the
 * reports and reports the outcome through messages and exit codes.
 *
 * <p>Usage: {@code java -cp jnose-core-0.9.4-jar-with-dependencies.jar JNoseCli.java <project-folder> [output-folder]}
 */
public final class JNoseCli {

    static final int EXIT_OK = 0;
    static final int EXIT_USAGE = 1;
    static final int EXIT_NO_TESTS = 2;
    static final int EXIT_ERROR = 3;

    static final String USAGE = "Usage: JNoseCli <project-folder> [output-folder]";

    private JNoseCli() {
    }

    /** Command-line options: the project to analyse and the folder for the CSV files. */
    static final class Options {
        private final Path project;
        private final Path outDir;

        Options(Path project, Path outDir) {
            this.project = project;
            this.outDir = outDir;
        }

        Path project() {
            return project;
        }

        Path outDir() {
            return outDir;
        }
    }

    /** Error that ends the program with a message and a specific exit code. */
    static final class CliException extends Exception {
        final int exitCode;

        CliException(int exitCode, String message) {
            super(message);
            this.exitCode = exitCode;
        }
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs the whole program and returns its exit code (separate from main so it can be tested). */
    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Options options = parseArgs(args);
            SmellAnalyzer.configureLibraryLogging(err);

            List<TestClass> classes = SmellAnalyzer.analyse(options.project());
            warnAboutUnparseableFiles(ParseabilityCheck.findUnparseableFiles(options.project()), options.project(), err);
            if (classes.isEmpty()) {
                throw new CliException(EXIT_NO_TESTS, "No JUnit test classes found in " + options.project());
            }

            Files.createDirectories(options.outDir());
            String name = options.project().getFileName().toString();
            Path byClass = options.outDir().resolve(name + CsvReports.BY_CLASS_SUFFIX);
            Path bySmell = options.outDir().resolve(name + CsvReports.BY_SMELL_SUFFIX);

            CsvReports.writeByClass(classes, options.project(), byClass, err);
            int occurrences = CsvReports.writeBySmell(classes, options.project(), bySmell);

            printSummary(out, classes.size(), occurrences, byClass, bySmell);
            return EXIT_OK;
        } catch (CliException e) {
            err.println(e.getMessage());
            return e.exitCode;
        } catch (SmellAnalyzer.AnalysisException e) {
            err.println(e.getMessage());
            return EXIT_ERROR;
        } catch (IOException e) {
            err.println("Could not read the project or write the reports: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    /** Validates the command-line arguments. */
    static Options parseArgs(String[] args) throws CliException {
        if (args.length < 1 || args.length > 2) {
            throw new CliException(EXIT_USAGE, USAGE);
        }
        Path project = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isDirectory(project)) {
            throw new CliException(EXIT_USAGE, "Not a folder: " + project);
        }
        Path outDir = args.length > 1 ? Path.of(args[1]) : Path.of(".");
        return new Options(project, outDir);
    }

    /** Tells the user which files were left out of the analysis. */
    static void warnAboutUnparseableFiles(List<Path> files, Path project, PrintStream err) {
        if (files.isEmpty()) {
            return;
        }
        err.println("Warning: " + files.size() + " Java file(s) use syntax newer than Java 13 and were skipped by jnose-core;");
        err.println("         test smells in these files are NOT included in the reports:");
        files.forEach(f -> err.println("           " + project.relativize(f)));
    }

    /** Prints a short summary of the analysis and where the reports were written. */
    static void printSummary(PrintStream out, int testClasses, int occurrences, Path byClass, Path bySmell) {
        out.printf("%d test classes, %d smell occurrences%n", testClasses, occurrences);
        out.println("Written: " + byClass.toAbsolutePath());
        out.println("Written: " + bySmell.toAbsolutePath());
    }
}
