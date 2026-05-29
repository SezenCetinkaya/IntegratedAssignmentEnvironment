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

    public ProcessResult run(String[] command, String runArguments, File workDir) {
        List<String> argList = new ArrayList<>(Arrays.asList(command));
        if (runArguments != null && !runArguments.isBlank()) {
            argList.addAll(Arrays.asList(runArguments.trim().split("\\s+")));
        }
        String[] mergedCommand = argList.toArray(new String[0]);

        // Windows'ta 'main.exe' gibi yerel dizindeki dosyalar PATH'te aranır,
        // bulunamazsa hata verir. cmd /c ile sarmalayarak mevcut dizinden çalıştırırız.
        String[] finalCommand = mergedCommand;
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            finalCommand = new String[mergedCommand.length + 2];
            finalCommand[0] = "cmd";
            finalCommand[1] = "/c";
            System.arraycopy(mergedCommand, 0, finalCommand, 2, mergedCommand.length);
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

            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }

            outThread.join(2000);
            errThread.join(2000);

            if (!finished) {
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

        String compilerPath = config.getCompilerPath();
        if (compilerPath != null && !compilerPath.isBlank()) {
            File compilerFile = new File(compilerPath);
            if (!compilerFile.exists() && !isOnPath(compilerPath)) {
                return new ProcessResult(-1, "",
                        "COMPILER_NOT_FOUND: " + compilerPath, false);
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(config.getCompilerPath());

        String args = config.getCompileArgs();
        if (args != null && !args.isBlank()) {
            cmd.addAll(Arrays.asList(args.split("\\s+")));
        }

        cmd.add(config.getSourceFilename());

        return run(cmd.toArray(new String[0]), "", sourceDir);
    }

    private boolean isOnPath(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) return false;
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            File candidate = new File(dir, name);
            if (candidate.exists() && candidate.isFile()) return true;
            // Windows için .exe uzantısı dene
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                File withExe = new File(dir, name + ".exe");
                if (withExe.exists() && withExe.isFile()) return true;
            }
        }
        return false;
    }

    private int timeoutSeconds = 10;

    public void setTimeoutSeconds(int seconds) {
        this.timeoutSeconds = seconds;
    }
}
