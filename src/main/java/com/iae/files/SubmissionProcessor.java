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
        return processAll(submissionsDir, config, expectedOutput, ignoreCase, "");
    }

    public List<StudentResult> processAll(File submissionsDir,
                                          Configuration config,
                                          String expectedOutput,
                                          boolean ignoreCase,
                                          String runArguments) {
        List<StudentResult> results = new ArrayList<>();

        File[] zipFiles = findZipFiles(submissionsDir);
        if (zipFiles == null) {
            return results;
        }

        for (File zipFile : zipFiles) {
            StudentResult result = processSingle(zipFile, config, expectedOutput, ignoreCase, runArguments);
            results.add(result);
        }

        return results;
    }

    public StudentResult processSingle(File zipFile,
                                       Configuration config,
                                       String expectedOutput,
                                       boolean ignoreCase) {
        return processSingle(zipFile, config, expectedOutput, ignoreCase, "");
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
     * @param runArguments   Programın çalıştırılırken alacağı argümanlar
     * @return               Öğrencinin değerlendirme sonucu
     */
    public StudentResult processSingle(File zipFile,
                                       Configuration config,
                                       String expectedOutput,
                                       boolean ignoreCase,
                                       String runArguments) {
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

            if (config.isInterpreted()) {
                if (!new File(executionDir, config.getSourceFilename()).exists()) {
                    result.markCompiled("SOURCE_NOT_FOUND");
                    result.appendError("Beklenen kaynak dosya yok: " + config.getSourceFilename());
                    return result;
                }
            }
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
                // ============================================================
                // TODO [OWNER: Talat Karasakal (Execution)] [PHASE: 3] [REQ: 4]
                // GÖREV: executionTimeMs'i ölç ve result'a yaz
                // AÇIKLAMA:
                //   StudentResult'ta alan ve DB kolonu var, UI'da kolon var — ama ölçüm hiç yapılmıyor.
                //   Her öğrenci için UI "0 ms" ya da boş gösteriyor, bu yanıltıcı.
                // ADIMLAR:
                //   1. commandRunner.run() çağrısından önce: long start = System.nanoTime();
                //   2. Çağrıdan sonra: result.setExecutionTimeMs((int)((System.nanoTime()-start)/1_000_000));
                // KABUL KRİTERİ:
                //   Results tablosunda executionTimeMs kolonu her satır için pozitif ms değeri gösteriyor.
                // ============================================================
                ProcessResult runResult = commandRunner.run(runParts, runArguments, executionDir);

                boolean passed = outputComparator.compare(
                        runResult.getStdout(),
                        expectedOutput,
                        ignoreCase
                );

                // ============================================================
                // TODO [OWNER: Talat Karasakal (Execution)] [PHASE: 3] [REQ: 7, 8]
                // GÖREV: Runtime error'ı PASS gibi gösterme bug'ını düzelt
                // AÇIKLAMA:
                //   Hoca raporu: "runtime error pass gibi görünüyo"
                //   Şu an: passed=true ise, exitCode!=0 olsa bile PASS veriliyor (satır altında).
                //   Process exit code != 0 ise çıktı karşılaştırması yapılmamalı.
                // ADIMLAR:
                //   1. isTimedOut kontrolünden SONRA, passed kontrolünden ÖNCE şunu ekle:
                //      if (runResult.getExitCode() != 0 && !runResult.isTimedOut()) {
                //          result.markRun("RUNTIME_ERROR");
                //          result.appendError("Exit code: " + runResult.getExitCode()
                //              + (runResult.getStderr().isBlank() ? "" : "\n" + runResult.getStderr()));
                //          return result;
                //      }
                //   2. Bu sayede exitCode!=0 olan programlar PASS sayılmaz.
                // KABUL KRİTERİ:
                //   Segfault eden veya exception fırlatan öğrenci kodu RUNTIME_ERROR
                //   olarak raporlanıyor, yanlışlıkla PASS sayılmıyor.
                // ============================================================
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
        return dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip"));
    }

    /**
     * ZIP dosyasının adından öğrenci ID'sini çıkarır.
     * Örnek: "20220602021.zip" → "20220602021"
     */
    private String extractStudentId(File zipFile) {
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
        return Files.readString(expectedOutputFile.toPath());
    }
}
