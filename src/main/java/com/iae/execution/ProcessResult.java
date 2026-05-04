package com.iae.execution;

// TODO: Talat Karasakal tarafından implement edilecek (Execution + Plugins modülü)
public class ProcessResult {

    private int exitCode;
    private String stdout;
    private String stderr;
    private boolean timedOut;

    public int getExitCode()   { return exitCode; }
    public String getStdout()  { return stdout; }
    public String getStderr()  { return stderr; }
    public boolean isTimedOut() { return timedOut; }

    public boolean hasFailed() {
        // TODO: Talat
        return false;
    }

    public String getCombinedOutput() {
        // TODO: Talat
        return null;
    }

    public boolean isSuccessful() {
        // TODO: Talat
        return false;
    }
}
