package com.iae.core;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

// TODO: Gözde Yılıkyılmaz tarafından implement edilecek (Core + Installer modülü)
public class Configuration {

    private int configId;
    private String name;
    private String language;
    private String compilerPath;
    private String compileArgs;
    private String sourceFilename;
    private String runCommand;
    private boolean isInterpreted;
    private int timeoutSeconds;

    public int getConfigId() { return configId; }
    public void setConfigId(int configId) { this.configId = configId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCompilerPath() { return compilerPath; }
    public void setCompilerPath(String compilerPath) { this.compilerPath = compilerPath; }

    public String getCompileArgs() { return compileArgs; }
    public void setCompileArgs(String compileArgs) { this.compileArgs = compileArgs; }

    public String getSourceFilename() { return sourceFilename; }
    public void setSourceFilename(String sourceFilename) { this.sourceFilename = sourceFilename; }

    public String getRunCommand() { return runCommand; }
    public void setRunCommand(String runCommand) { this.runCommand = runCommand; }

    public boolean isInterpreted() { return isInterpreted; }
    public void setInterpreted(boolean interpreted) { isInterpreted = interpreted; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public void exportToFile(String path) {
        Properties props = new Properties();

        props.setProperty("configId", String.valueOf(configId));
        props.setProperty("name", name);
        props.setProperty("language", language);
        props.setProperty("compilerPath", compilerPath);
        props.setProperty("sourceFilename", sourceFilename);
        props.setProperty("runCommand", runCommand);
        props.setProperty("timeoutSeconds", String.valueOf(timeoutSeconds));

        try (FileOutputStream out = new FileOutputStream(path)) {
            props.store(out, "IAE Configuration");
        } catch (IOException e) {
            throw new RuntimeException("Configuration could not be exported: " + path, e);
        }
    }

    public static Configuration importFromFile(String path) {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(path)) {
            props.load(in);

            Configuration config = new Configuration();
            config.setConfigId(Integer.parseInt(props.getProperty("configId", "0")));
            config.setName(props.getProperty("name", ""));
            config.setLanguage(props.getProperty("language", ""));
            config.setCompilerPath(props.getProperty("compilerPath", ""));
            config.setSourceFilename(props.getProperty("sourceFilename", ""));

            config.setRunCommand(props.getProperty("runCommand", ""));
            config.setTimeoutSeconds(Integer.parseInt(props.getProperty("timeoutSeconds", "30")));

            return config;
        } catch (IOException e) {
            throw new RuntimeException("Configuration could not be imported: " + path, e);
        }
    }

    public void updateCompilerPath(String compilerPath) {
        if (compilerPath == null || compilerPath.isBlank()) {
            throw new IllegalArgumentException("Compiler path cannot be empty.");
        }

        this.compilerPath = compilerPath;
    }
}
