package com.iae.core;

public class StudentResult {

    private int resultId;
    private String studentId;
    private String zipFilename;
    private String compileStatus;
    private String runStatus;
    private String compileErrorLog;
    private String evaluatedAt;
    private Integer executionTimeMs;

    public int getResultId() { return resultId; }
    public void setResultId(int resultId) { this.resultId = resultId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getZipFilename() { return zipFilename; }
    public void setZipFilename(String zipFilename) { this.zipFilename = zipFilename; }

    public String getCompileStatus() { return compileStatus; }
    public void setCompileStatus(String compileStatus) { this.compileStatus = compileStatus; }

    public String getRunStatus() { return runStatus; }
    public void setRunStatus(String runStatus) { this.runStatus = runStatus; }

    public String getCompileErrorLog() { return compileErrorLog; }
    public void setCompileErrorLog(String compileErrorLog) { this.compileErrorLog = compileErrorLog; }

    public String getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(String evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public Integer getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Integer executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public void markCompiled(String status) {
        this.compileStatus = status;
    }

    public void markRun(String status) {
        this.runStatus = status;
    }

    public void recordComparison(String result) {
        this.runStatus = result;
    }

    public void appendError(String message) {
        if (compileErrorLog == null || compileErrorLog.isBlank()) {
            compileErrorLog = message;
        } else {
            compileErrorLog += "\n" + message;
        }
    }
}
