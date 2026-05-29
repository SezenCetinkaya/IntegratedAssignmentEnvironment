# IAE — Integrated Assignment Environment

> **CE 316 Project** — Izmir University of Economics, Faculty of Engineering
> Advisor: Asst. Prof. Dr. İlker KORKMAZ | May 2026
> Version 2.0.0

IAE is a desktop application designed to automate the evaluation of student programming assignments on Windows. The application extracts submitted ZIP files, compiles or interprets the code, compares outputs against an expected result, and stores results in a SQLite database.

## Features

- JavaFX-based graphical user interface
- Secure extraction of student ZIP submissions (zip-slip protected)
- Isolated temporary workspace per student
- Automatic output comparison (CRLF/LF normalization, line-by-line diff)
- Support for compiled languages (C, Java) and interpreted languages (Python)
- Configurable compiler path, compile arguments, run command, and timeout per language
- SQLite-backed project, configuration, and result storage
- CSV export of evaluation results
- Configuration import/export for sharing between machines
- Gradle wrapper for single-command build and run

## Technology

- Java 21
- JavaFX 21
- Gradle 8.7 (Groovy DSL)
- SQLite (`org.xerial:sqlite-jdbc:3.45.3.0`)
- Gson (`com.google.code.gson:gson:2.10.1`)
- JUnit 5

## Project Structure

```
src/
├── main/
│   ├── java/com/iae/
│   │   ├── AppLauncher.java          # JavaFX launch entry point
│   │   ├── Main.java
│   │   ├── core/                     # Domain models (Project, Configuration, StudentResult, Report)
│   │   ├── db/                       # SQLite DAO layer
│   │   ├── execution/                # External process runner and result model
│   │   ├── files/                    # ZIP extraction, workspace, file lookup, output comparison
│   │   ├── gui/                      # JavaFX controllers
│   │   └── service/                  # ProjectFileService (import/export)
│   └── resources/com/iae/gui/
│       ├── main-view.fxml
│       ├── configuration-view.fxml
│       ├── results-view.fxml
│       ├── styles.css
│       └── help/
│           └── manual.html           # In-app user manual
└── test/java/com/iae/
```

### Main modules

| Package | Responsibility |
|---|---|
| `com.iae.core` | Domain models: `Project`, `Configuration`, `StudentResult`, `Report` |
| `com.iae.db` | SQLite DAOs: `ProjectDAO`, `ConfigurationDAO`, `StudentResultDAO` |
| `com.iae.execution` | `CommandRunner` — compiles and runs student code with timeout |
| `com.iae.files` | `ZipExtractor`, `WorkspaceManager`, `FileLocator`, `OutputComparator` |
| `com.iae.gui` | JavaFX controllers: `MainController`, `ConfigurationController`, `ResultsController` |
| `com.iae.service` | `ProjectFileService` — project file import/export |

## Requirements

- Java 21 JDK must be installed and available on PATH
- Gradle is not required; the project includes `gradlew.bat`
- Windows 10 or later

## Quick Start

### Run the application

```powershell
.\gradlew.bat run
```

### Run tests

```powershell
.\gradlew.bat test
```

### Build executable JAR

```powershell
.\gradlew.bat jar
```

The fat JAR (all dependencies bundled) is written to `C:/tmp/iae-build/libs/`.

### Package Windows EXE

```powershell
.\gradlew.bat launch4j
```

> **Note:** The Gradle `buildDir` is set to `C:/tmp/iae-build` to avoid encoding issues with non-ASCII characters in the project path.

## Usage

1. **Create a language configuration** — go to *Configuration → Manage Configurations*, define compiler path, source filename, run command, and timeout for your language (C, Java, Python, etc.).
2. **Create a project** — go to *File → New Project*, select a configuration, point to the folder of student ZIP files, and select the expected output file.
3. **Run evaluation** — click *Run Evaluation*. IAE extracts each ZIP, compiles (if applicable), runs the program, and compares output.
4. **Review results** — the Results tab shows compile status, pass/fail, and execution time per student. Click a row to view error details.
5. **Export results** — use *File → Export Results...* to save the results table as a CSV file.

## Key Design Notes

- `ZipExtractor` protects against zip-slip path traversal attacks.
- `OutputComparator` normalises `\r\n` / `\n` line endings before comparison.
- `WorkspaceManager` creates an isolated temporary directory per student and cleans up after processing.
- `CommandRunner` kills the student process (and all its descendants) if it exceeds the configured timeout.
- The build output directory is redirected to `C:/tmp/iae-build` to avoid Gradle classpath encoding failures on Windows paths containing non-ASCII characters (e.g. `Masaüstü`).