# IAE — Integrated Assignment Environment

> **CE 316 Project** — Izmir University of Economics, Faculty of Engineering  
> Advisor: Asst. Prof. Dr. İlker KORKMAZ | May 2026

A standalone desktop application that automates the evaluation of student programming assignments on Windows. Lecturers can compile, run, and compare student submissions in batch — no manual grading.


## Project Structure

```
src/
├── main/
│   ├── java/com/iae/
│   │   ├── Main.java                          # JavaFX entry point
│   │   ├── core/
│   │   │   ├── Configuration.java             # Language config model
│   │   │   ├── Project.java                   # Project model
│   │   │   ├── StudentResult.java             # Per-student result model
│   │   │   ├── TestCase.java                  # Test case model
│   │   │   ├── Report.java                    # Report aggregator
│   │   │   ├── Logger.java                    # File-based logger
│   │   │   └── Validator.java                 # Config & project validator
│   │   ├── db/
│   │   │   ├── DatabaseHelper.java            # SQLite connection & schema
│   │   │   ├── ProjectDAO.java                # Project persistence
│   │   │   ├── ConfigurationDAO.java          # Config persistence
│   │   │   └── StudentResultDAO.java          # Result persistence
│   │   ├── execution/
│   │   │   ├── CommandRunner.java             # ProcessBuilder wrapper
│   │   │   └── ProcessResult.java             # Command output model
│   │   ├── files/                             
│   │   │   ├── ZipExtractor.java              # ZIP extraction, zip-slip safe
│   │   │   ├── WorkspaceManager.java          # Temp workspace lifecycle
│   │   │   ├── FileLocator.java               # Case-insensitive source finder
│   │   │   ├── OutputComparator.java          # Output diff (CRLF + ignoreCase)
│   │   │   └── InvalidZipException.java       # Custom exception
│   │   └── gui/
│   │       ├── MainController.java            # Main window controller
│   │       ├── ConfigurationController.java   # Config editor controller
│   │       └── ResultsController.java         # Results table controller
│   └── resources/com/iae/gui/
│       ├── main-view.fxml                     # Main window layout
│       ├── configuration-view.fxml            # Config editor layout
│       ├── results-view.fxml                  # Results table layout
│       └── styles.css                         # JavaFX stylesheet
└── test/java/com/iae/files/                  
    ├── ZipExtractorTest.java                  # 8 tests (valid, corrupt, zip-slip)
    ├── WorkspaceManagerTest.java              # 5 tests (create, clean, isolation)
    ├── FileLocatorTest.java                   # 6 tests (case-insensitive, nested)
    └── OutputComparatorTest.java              # 10 tests (CRLF, ignoreCase, diff msg)
```

---

## Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 21 LTS |
| GUI Framework | JavaFX | 21 |
| Database | SQLite via JDBC | 3.45.3.0 |
| Build Tool | Gradle | 8.7 |
| Test Framework | JUnit 5 | 5.10.2 |
| Installer | Inno Setup | 6.3.3 |

---

## Build & Run

### Prerequisites
- Java 21 JDK (Temurin recommended)
- No Gradle install needed — wrapper is included

### Run the application
```powershell
.\gradlew.bat run
```

### Run all tests
```powershell
.\gradlew.bat test
```

Expected output: `BUILD SUCCESSFUL` — 29 tests, 0 failures.

### Build fat-JAR
```powershell
.\gradlew.bat jar
```

Output: `C:/tmp/iae-build/libs/CE_316_Project-1.0.0.jar`

> **Note:** `buildDir` is set to `C:/tmp/iae-build` (not the default `build/`) Do not change this line in `build.gradle`.


## Architecture

The application follows the **MVC** pattern layered over a service and data-access tier:

```
View (FXML)  →  Controller  →  Service Layer  →  DAO Layer  →  SQLite
                                    ↓
                      ZipExtractor / CommandRunner / OutputComparator
```

Dependency flow is strictly **top-down** — no lower layer references a higher one.

---

## files/ Module API Reference

> All classes are in `com.iae.files`.

### ZipExtractor
```java
File extract(File zipFile, File targetDir) throws InvalidZipException
boolean validateArchive(File zipFile)
```
Extracts a student ZIP into `targetDir`. Guards against zip-slip attacks. Throws `InvalidZipException` on empty, corrupt, or missing files.

### WorkspaceManager
```java
File   createWorkspace(String studentId) throws IOException
void   cleanWorkspace(String studentId)  throws IOException
```
Creates and destroys a per-student temporary directory under the system temp folder.

### FileLocator
```java
File locate(File workspaceDir, String filename) throws FileNotFoundException
```
Recursively searches for `filename` (case-insensitive). Handles `main.c` vs `Main.c` transparently.

### OutputComparator
```java
boolean compare(String actualOutput, String expectedOutput)
boolean compare(String actualOutput, String expectedOutput, boolean ignoreCase)
boolean compareAgainstExpected(String actual, String expected)
boolean compareAgainstExpected(String actual, String expected, boolean ignoreCase)
String  getCombinedOutput()   // returns line-level diff message on mismatch
```
Normalises `\r\n` → `\n`, trims leading/trailing whitespace, then compares line-by-line. `ignoreCase=true` disables case sensitivity.