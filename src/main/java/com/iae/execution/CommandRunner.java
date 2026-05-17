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
