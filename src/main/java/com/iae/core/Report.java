package com.iae.core;

import java.util.List;

public class Report {
    private int totalStudents;
    private int passedCount;
    private int failedCount;
    private int compileErrorCount;
    private int errorCount;
    private double averageTimeMs;

    public void calculateStats(List<StudentResult> results) {
        if (results == null || results.isEmpty()) return;

        this.totalStudents = results.size();
        this.passedCount = (int) results.stream().filter(r -> "PASS".equals(r.getRunStatus())).count();
        this.failedCount = (int) results.stream().filter(r -> "FAIL".equals(r.getRunStatus())).count();
        this.compileErrorCount = (int) results.stream().filter(r -> "COMPILE_ERROR".equals(r.getCompileStatus())).count();
        this.errorCount = totalStudents - passedCount - failedCount;

        this.averageTimeMs = results.stream()
                .filter(r -> r.getExecutionTimeMs() != null)
                .mapToInt(StudentResult::getExecutionTimeMs)
                .average()
                .orElse(0.0);
    }

    public int getTotalStudents() { return totalStudents; }
    public int getPassedCount() { return passedCount; }
    public int getFailedCount() { return failedCount; }
    public int getCompileErrorCount() { return compileErrorCount; }
    public int getErrorCount() { return errorCount; }
    public double getAverageTimeMs() { return averageTimeMs; }
    public double getSuccessRate() { return totalStudents == 0 ? 0 : (double) passedCount / totalStudents * 100; }
}