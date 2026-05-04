package com.iae.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// TODO: Gözde Yılıkyılmaz tarafından implement edilecek (Core + Installer modülü)
public class Project {

    private int projectId;
    private String name;
    private LocalDateTime createdAt;
    private String lastRunAt;
    private String expectedOutputPath;
    private String submissionDir;
    private String runArguments;
    private int configId;
    private final List<StudentResult> results = new ArrayList<>();

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(String lastRunAt) { this.lastRunAt = lastRunAt; }

    public String getExpectedOutputPath() { return expectedOutputPath; }
    public void setExpectedOutputPath(String expectedOutputPath) { this.expectedOutputPath = expectedOutputPath; }

    public String getSubmissionDir() { return submissionDir; }
    public void setSubmissionDir(String submissionDir) { this.submissionDir = submissionDir; }

    public String getRunArguments() { return runArguments; }
    public void setRunArguments(String runArguments) { this.runArguments = runArguments; }

    public int getConfigId() { return configId; }
    public void setConfigId(int configId) { this.configId = configId; }

    public List<StudentResult> getResults() { return results; }

    public static Project load(int id) {
        // TODO: Gözde
        return null;
    }

    public void save() {
        // TODO: Gözde
    }

    public void setConfiguration(Configuration configuration) {
        // TODO: Gözde
    }

    public void addResult(StudentResult result) {
        // TODO: Gözde
    }

    public void removeResult(int resultId) {
        // TODO: Gözde
    }
}
