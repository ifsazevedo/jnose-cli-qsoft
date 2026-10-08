# JNoseCli
![Tests](https://github.com/<o-seu-utilizador>/jnose-cli-qsoft/actions/workflows/tests.yml/badge.svg)

Unofficial command-line front end for the **JNose** test smell detector. It runs the
JNose detection engine (`jnose-core` 0.9.4, the same engine used by JNose 2.5.0) on a
local Java project and writes two CSV reports, without the JNose web interface.

> This is not part of JNose and is not maintained by its authors. It was developed
> for teaching QSOFT (Software Quality), MEI — School of Engineering of the Technical
> University of Porto (ISEP), and is maintained for that purpose, pinned to
> `jnose-core` 0.9.4. The original tool is https://github.com/arieslab/jnose.

## Requirements

- JDK 25 or later (`jnose-core` 0.9.4 is compiled for Java 25).
- The official `jnose-core` jar, which already includes its only dependency (JavaParser):

```
curl -LO https://github.com/arieslab/jnose-core/releases/download/v0.9.4/jnose-core-0.9.4-jar-with-dependencies.jar
```

The project being analysed does **not** need to compile, to be a git repository or
to use Java 25: only its `.java` files are read.

## Usage

Download this repository (green **Code** button → **Download ZIP**, or `git clone`)
and put the `jnose-core` jar in the same folder. Keep the four `.java` files together:
no compilation is needed, the Java launcher compiles them on the fly.

```
java -cp jnose-core-0.9.4-jar-with-dependencies.jar JNoseCli.java <project-folder> [output-folder]
```

On macOS, if the default `java` is not version 25:

```
"$(/usr/libexec/java_home -v 25)/bin/java" -cp jnose-core-0.9.4-jar-with-dependencies.jar JNoseCli.java <project-folder> reports
```

## Output

| File | Content |
|---|---|
| `<project>_result_byclasstest.csv` | One row per test class: project, test class, test file, production file, LOC, number of methods, and one column per smell (21 smells, alphabetical order) with its number of occurrences. |
| `<project>_result_bytestsmells.csv` | One row per smell occurrence: project, test class, files, smell, test method, lines. |

In `Lines`, most smells give the line range of the test method; Eager Test gives
the lines where different production methods are called.

Paths are relative to the project folder. In Excel with Portuguese regional
settings, import the files with File → Import → CSV and choose the comma as
delimiter.

Exit codes: `0` success, `1` invalid arguments, `2` no test classes found,
`3` analysis or I/O error.

## Limitations (of jnose-core)

- **Java syntax up to Java 13 only.** `jnose-core` parses code with JavaParser 3.3.5.
  Files using text blocks, switch expressions, pattern matching (`instanceof String s`),
  top-level records or sealed classes cannot be parsed and are skipped; their smells
  are missing from the reports. JNoseCli lists these files in a warning.
- **JUnit 5 support is partial.**
  - Only methods annotated with `@Test` (or named `test...`) are analysed; methods
    annotated only with `@ParameterizedTest` or `@RepeatedTest` are not checked.
  - Renaming a JUnit 5 method to `test...` makes it analysed, but it is then reported
    as Ignored Test (false positive), because it is not `public`.
  - `@Disabled` is not detected as Ignored Test (only JUnit 4 `@Ignore` is).
- **Static analysis.** Smells are detected from the source code alone; for example,
  MockMvc `andExpect(...)` calls are not recognised as assertions, so such tests are
  reported as Unknown Test (false positives).

## Design

| Class | Responsibility |
|---|---|
| `JNoseCli` | Command-line interface: arguments, exit codes, messages, summary. |
| `SmellAnalyzer` | Bridge to `jnose-core`: smells to detect, library logging, running the analysis. |
| `ParseabilityCheck` | Finds the files that `jnose-core` cannot parse. |
| `CsvReports` | The two CSV reports and the CSV formatting. |

## Tests

The `test` folder has one JUnit 5 test class per class, plus `SampleProject`, a
small fixture project with known smells. To run them, download the JUnit console
launcher and, in this folder (on Windows use `;` instead of `:`):

```
curl -LO https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
javac -d build -cp jnose-core-0.9.4-jar-with-dependencies.jar:junit-platform-console-standalone-1.11.4.jar *.java test/*.java
java -jar junit-platform-console-standalone-1.11.4.jar execute --class-path build:jnose-core-0.9.4-jar-with-dependencies.jar --scan-class-path build
```

JNose itself reports **Lazy Test** in these tests: several focused test methods
exercise the same production method (for example, four tests of `CsvReports.csv`,
one per formatting rule). This is a deliberate choice: merging them would create
Assertion Roulette or Eager Test, and parameterized tests are not analysed by
JNose (see Limitations).

## Continuous integration

Every push and pull request runs the tests on GitHub Actions with JDK 25
(`.github/workflows/tests.yml`). The workflow downloads the official `jnose-core`
jar, so it also checks that the release asset is still available.

## Credits and citation

All test smell detection is performed by `jnose-core`, the engine of the JNose Test
tool, developed by Tássio Virgínio, Luana Martins, Larissa Rocha, Railana Santana,
Adriana Cruz, Heitor Costa and Ivan Machado (ARIES Lab, https://github.com/arieslab).

- JNose: https://github.com/arieslab/jnose (GPL-3.0)
- jnose-core: https://github.com/arieslab/jnose-core (GPL-3.0)
- JavaParser (dependency of jnose-core): https://javaparser.org (LGPL-3.0 or Apache-2.0)

JNoseCli only calls the `jnose-core` API and writes its results; it contains no code
copied from JNose. Please cite the original tool:

> T. Virgínio, L. Martins, L. Rocha, R. Santana, A. Cruz, H. Costa and I. Machado,
> "JNose: Java Test Smell Detector", in *Proceedings of the XXXIV Brazilian Symposium
> on Software Engineering (SBES 2020)*, ACM, 2020, pp. 564–569.
> doi:10.1145/3422392.3422499

```bibtex
@inproceedings{virginio2020jnose,
  author    = {Virg{\'\i}nio, T{\'a}ssio and Martins, Luana and Rocha, Larissa
               and Santana, Railana and Cruz, Adriana and Costa, Heitor
               and Machado, Ivan},
  title     = {{JNose}: Java Test Smell Detector},
  booktitle = {Proceedings of the XXXIV Brazilian Symposium on Software
               Engineering (SBES 2020)},
  pages     = {564--569},
  publisher = {ACM},
  year      = {2020},
  doi       = {10.1145/3422392.3422499}
}
```

## License

Copyright (C) 2026 Isabel Azevedo — QSOFT, MEI, School of Engineering of the
Technical University of Porto (ISEP).

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later version.
It is distributed WITHOUT ANY WARRANTY. See https://www.gnu.org/licenses/gpl-3.0.html.
