package com.iae.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public void save() {
        // Prototype aşamasında database bağlanmadan boş bırakılabilir.
        this.lastRunAt = LocalDateTime.now().toString();
    }

    public void setConfiguration(Configuration configuration) {
        if (configuration != null) {
            this.configId = configuration.getConfigId();
        }
    }


    public void addResult(StudentResult result) {
        if (result != null) {
            results.add(result);
        }
    }

    public void removeResult(int resultId) {
        results.removeIf(result -> result.getResultId() == resultId);
    }
    @Override
    public String toString() {
        if (name != null && !name.isBlank()) {
            if (createdAt != null ) {
                return name + " - " + createdAt;
            }
            return name;
        }

        return "Unnamed Project";
    }
    public static Project load(int id) {
        return null;
    }
}
