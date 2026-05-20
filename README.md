# IAE — Integrated Assignment Environment

> **CE 316 Project** — Izmir University of Economics, Faculty of Engineering
> Advisor: Asst. Prof. Dr. İlker KORKMAZ | May 2026

IAE is a desktop application designed to automate the evaluation of student programming assignments on Windows. The application extracts submitted ZIP files, runs the code, compares outputs, and stores results in a SQLite database.

## Features

- JavaFX-based user interface
- Secure extraction of student ZIP submissions
- Isolated temporary workspace per student
- Automatic output comparison (handles CRLF differences and optional case sensitivity)
- SQLite-backed project and result storage
- Gradle wrapper for single-command build and run

## Technology

- Java 21
- JavaFX 21
- Gradle 8.7
- SQLite (org.xerial:sqlite-jdbc)
- JUnit 5

## Project Structure

```
src/
├── main/
│   ├── java/com/iae/
│   │   ├── AppLauncher.java
│   │   ├── Main.java
│   │   ├── core/
│   │   ├── db/
│   │   ├── execution/
│   │   ├── files/
│   │   └── gui/
│   └── resources/com/iae/gui/
│       ├── main-view.fxml
│       ├── configuration-view.fxml
│       ├── results-view.fxml
│       └── styles.css
└── test/java/com/iae/
```

### Main modules

- `com.iae.AppLauncher` — JavaFX application entry point
- `com.iae.Main` — main application class
- `com.iae.core` — project, configuration, student result, and report models
- `com.iae.db` — SQLite DAO layer
- `com.iae.execution` — external process execution and result handling
- `com.iae.files` — ZIP extraction, workspace management, source lookup, output comparison
- `com.iae.gui` — JavaFX controllers

## Requirements

- Java 21 JDK must be installed
- Gradle is not required; the project includes `gradlew.bat`

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

### Package Windows EXE

```powershell
.\gradlew.bat launch4j
```

## Usage

1. Launch the application.
2. Enter test inputs, expected outputs, and build settings in the configuration screen.
3. Load student ZIP submissions.
4. Start execution: each submission is extracted, compiled, executed, and compared.
5. Review results in the `Results` tab.

## Notes

- `OutputComparator` normalizes `\r\n` and `\n` differences when comparing outputs.
- `ZipExtractor` protects against zip-slip attacks.
- `WorkspaceManager` uses a temporary working directory for each student and cleans it after processing.

## Development

- The code is written for Java 21.
- Tests are implemented with JUnit 5.
- JavaFX FXML files are located under `src/main/resources/com/iae/gui`.
