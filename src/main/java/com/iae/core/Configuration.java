package com.iae.core;

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
        // TODO: Gözde
    }

    public static Configuration importFromFile(String path) {
        // TODO: Gözde
        return null;
    }

    public void updateCompilerPath(String compilerPath) {
        // TODO: Gözde
    }
}
