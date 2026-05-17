package com.iae.files;

import com.iae.core.Configuration;
import com.iae.core.StudentResult;
import com.iae.execution.CommandRunner;
import com.iae.execution.ProcessResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// TODO: Gözde Yılıkyılmaz tarafından implement edilecek (Core + Installer modülü — AssignmentRunner)
public class SubmissionProcessor {

    private final ZipExtractor zipExtractor           = new ZipExtractor();
    private final WorkspaceManager workspaceManager   = new WorkspaceManager();
    private final FileLocator fileLocator             = new FileLocator();
    private final OutputComparator outputComparator   = new OutputComparator();
    private final CommandRunner commandRunner         = new CommandRunner();

    /**
     * submissionsDir içindeki tüm ZIP dosyalarını işler.
     * Her ZIP için: aç → kaynak dosyayı bul → derle → çalıştır → karşılaştır → sonuç üret.
     * Bir öğrencide hata olsa bile diğerleri işlenmeye devam eder (NR-01 robustness).
     *
     * @param submissionsDir  Öğrenci ZIP dosyalarının bulunduğu dizin
     * @param config          Proje yapılandırması (derleyici yolu, komutlar vb.)
     * @param expectedOutput  Beklenen program çıktısı (karşılaştırma için)
     * @param ignoreCase      Çıktı karşılaştırmasında büyük/küçük harf duyarsızlığı
     * @return                Her öğrenci için bir StudentResult listesi
     */
    public List<StudentResult> processAll(File submissionsDir,
                                          Configuration config,
                                          String expectedOutput,
                                          boolean ignoreCase) {
        List<StudentResult> results = new ArrayList<>();

        File[] zipFiles = findZipFiles(submissionsDir);
        if (zipFiles == null) {
            return results;
        }

        for (File zipFile : zipFiles) {
            StudentResult result = processSingle(zipFile, config, expectedOutput, ignoreCase);
            results.add(result);
        }

        return results;
    }

    /**
     * Tek bir öğrencinin ZIP dosyasını işler ve StudentResult döndürür.
     * Hata durumunda (bozuk ZIP, compile hatası, dosya bulunamıyor) exception fırlatmak yerine
     * StudentResult içinde hata durumunu kaydeder.
     *
     * @param zipFile        Öğrencinin ZIP dosyası
     * @param config         Proje yapılandırması
     * @param expectedOutput Beklenen program çıktısı
     * @param ignoreCase     Büyük/küçük harf duyarsız karşılaştırma
     * @return               Öğrencinin değerlendirme sonucu
     */
    public StudentResult processSingle(File zipFile,
                                       Configuration config,
                                       String expectedOutput,
                                       boolean ignoreCase) {
        StudentResult result = new StudentResult();

        String studentId = extractStudentId(zipFile);
        result.setStudentId(studentId);
        result.setZipFilename(zipFile.getName());
        result.setEvaluatedAt(java.time.LocalDateTime.now().toString());

        File workspace = null;

        try {
            workspace = workspaceManager.createWorkspace(studentId);

            zipExtractor.extract(zipFile, workspace);

            File sourceFile = fileLocator.locate(workspace, config.getSourceFilename());
            File executionDir = sourceFile.getParentFile();

            commandRunner.setTimeoutSeconds(config.getTimeoutSeconds());

            ProcessResult compileResult = commandRunner.compile(config, executionDir);

            if (compileResult.hasFailed()) {
                result.markCompiled("COMPILE_ERROR");
                result.markRun("NOT_RUN");
                result.appendError(compileResult.getCombinedOutput());
                return result;
            }

            result.markCompiled("COMPILE_SUCCESS");

            // Prototype seviyesinde runCommand varsa çalıştır
            if (config.getRunCommand() != null && !config.getRunCommand().isBlank()) {
                String[] runParts = normalizeRunCommand(config.getRunCommand()).split("\\s+");
                ProcessResult runResult = commandRunner.run(runParts, executionDir);

                boolean passed = outputComparator.compare(
                        runResult.getStdout(),
                        expectedOutput,
                        ignoreCase
                );

                if (runResult.isTimedOut()) {
                    result.markRun("RUNTIME_ERROR");
                    result.appendError(runResult.getCombinedOutput());
                } else if (passed) {
                    result.markRun("PASS");
                    if (runResult.hasFailed()) {
                        String warning = "Program produced expected output but exited with code "
                                + runResult.getExitCode() + ".";
                        if (!runResult.getStderr().isBlank()) {
                            warning += " Stderr: " + runResult.getStderr();
                        }
                        result.appendError(warning);
                    }
                } else if (runResult.hasFailed()) {
                    result.markRun("RUNTIME_ERROR");
                    result.appendError(runResult.getCombinedOutput());
                } else {
                    result.markRun("FAIL");
                    result.appendError("Student output did not match the expected output.");
                }
            } else {
                result.markRun("RUN_COMMAND_MISSING");
            }

        } catch (InvalidZipException e) {
            result.markCompiled("EXTRACTION_ERROR");
            result.markRun("NOT_RUN");
            result.appendError(e.getMessage());

        } catch (FileNotFoundException e) {
            result.markCompiled("FILE_NOT_FOUND");
            result.markRun("NOT_RUN");
            result.appendError(e.getMessage());

        } catch (IOException e) {
            result.markCompiled("IO_ERROR");
            result.markRun("NOT_RUN");
            result.appendError(e.getMessage());

        } finally {
            try {
                workspaceManager.cleanWorkspace(studentId);
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    /**
     * Dizin içindeki tüm .zip dosyalarını döndürür.
     */
    private File[] findZipFiles(File dir) {
        // TODO: Gözde
        return dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip"));
    }

    /**
     * ZIP dosyasının adından öğrenci ID'sini çıkarır.
     * Örnek: "20220602021.zip" → "20220602021"
     */
    private String extractStudentId(File zipFile) {
        // TODO: Gözde
        String name = zipFile.getName();
        return name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
    }

    private String normalizeRunCommand(String runCommand) {
        if (runCommand == null) {
            return "";
        }
        String normalized = runCommand.trim();
        normalized = normalized.replaceAll("-cp\\.(?=\\S)", "-cp . ");
        return normalized;
    }

    /**
     * Beklenen çıktıyı bir dosyadan okur.
     */
    public String readExpectedOutput(File expectedOutputFile) throws IOException {
        // TODO: Gözde
        return Files.readString(expectedOutputFile.toPath());
    }
}
