package com.iae.execution;

import com.iae.core.Configuration;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandRunner {

    // ============================================================
    // TODO [OWNER: Talat Karasakal (Execution)] [PHASE: 1] [REQ: 6, 8]
    // GÖREV: run komutuna kullanıcı argümanlarını güvenli şekilde ekle
    // AÇIKLAMA:
    //   Şu an run(String[] command, File workDir) imzası runArguments almıyor.
    //   SubmissionProcessor, GUI'den gelen run args'ı geçemiyor.
    // ADIMLAR:
    //   1. İmzayı güncelle: run(String[] command, String runArguments, File workDir)
    //   2. ProcessBuilder'a argümanları ekle:
    //      List<String> args = new ArrayList<>(Arrays.asList(command));
    //      if (runArguments != null && !runArguments.isBlank()) {
    //          args.addAll(Arrays.asList(runArguments.split("\\s+")));
    //      }
    //      new ProcessBuilder(args).directory(workDir).start();
    //   3. String concat (command + " " + runArguments) YAPMA — boşluk içeren
    //      argümanlar bozulur. split("\\s+") veya satır başına bir argüman kullan.
    // KABUL KRİTERİ:
    //   "apple banana" girilince öğrenci programı argv[1]="apple", argv[2]="banana" alıyor.
    // ============================================================
    public ProcessResult run(String[] command, File workDir) {
        // Windows'ta 'main.exe' gibi yerel dizindeki dosyalar PATH'te aranır,
        // bulunamazsa hata verir. cmd /c ile sarmalayarak mevcut dizinden çalıştırırız.
        String[] finalCommand = command;
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            finalCommand = new String[command.length + 2];
            finalCommand[0] = "cmd";
            finalCommand[1] = "/c";
            System.arraycopy(command, 0, finalCommand, 2, command.length);
        }
        ProcessBuilder pb = new ProcessBuilder(finalCommand);
        pb.directory(workDir);

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            Process process = pb.start();

            Thread outThread = new Thread(() -> {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });

            Thread errThread = new Thread(() -> {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });

            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            
            outThread.join(2000);
            errThread.join(2000);

            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, stdout.toString(), stderr.toString(), true);
            }

            return new ProcessResult(process.exitValue(),
                                     stdout.toString(),
                                     stderr.toString(),
                                     false);

        } catch (IOException e) {
            return new ProcessResult(-1, "", "Komut başlatılamadı: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "Çalıştırma kesintiye uğradı: " + e.getMessage(), false);
        }
    }

    // ============================================================
    // TODO [OWNER: Talat Karasakal (Execution)] [PHASE: 3] [REQ: 7]
    // GÖREV: Compile öncesi compiler path varlığını kontrol et
    // AÇIKLAMA:
    //   PDF: "what if they don't exist!" — gizli requirement.
    //   Şu an geçersiz gcc path verilirse Process exception fırlatıyor,
    //   açıklayıcı mesaj yok ve batch durabilir.
    // ADIMLAR:
    //   1. compile() en başına ekle:
    //      String compilerPath = config.getCompilerPath();
    //      if (compilerPath != null && !compilerPath.isBlank()) {
    //          File compilerFile = new File(compilerPath);
    //          if (!compilerFile.exists() && !isOnPath(compilerPath)) {
    //              return new ProcessResult(-1, "", "COMPILER_NOT_FOUND: " + compilerPath, false);
    //          }
    //      }
    //   2. isOnPath() helper yaz:
    //      private boolean isOnPath(String name) {
    //          String pathEnv = System.getenv("PATH");
    //          if (pathEnv == null) return false;
    //          for (String dir : pathEnv.split(File.pathSeparator)) {
    //              if (new File(dir, name).exists()) return true;
    //          }
    //          return false;
    //      }
    //   3. GUI'de bu status'u da göster (Uğur'la koordine et).
    // KABUL KRİTERİ:
    //   Geçersiz gcc path → result tablosunda "COMPILER_NOT_FOUND", app crash olmuyor,
    //   batch sonraki öğrenciye geçiyor.
    // ============================================================
    public ProcessResult compile(Configuration config, File sourceDir) {
        if (config.isInterpreted()) {
            return new ProcessResult(0, "", "", false);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(config.getCompilerPath());

        String args = config.getCompileArgs();
        if (args != null && !args.isBlank()) {
            cmd.addAll(Arrays.asList(args.split("\\s+")));
        }

        cmd.add(config.getSourceFilename());

        return run(cmd.toArray(new String[0]), sourceDir);
    }

    private int timeoutSeconds = 10;

    public void setTimeoutSeconds(int seconds) {
        this.timeoutSeconds = seconds;
    }
}
