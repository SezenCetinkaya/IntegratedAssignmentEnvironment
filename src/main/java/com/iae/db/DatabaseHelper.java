package com.iae.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:iae.db";

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.err.println("Bağlantı hatası: " + e.getMessage());
            return null;
        }
    }

    public void initialiseSchema() {
        String configTable = "CREATE TABLE IF NOT EXISTS Configuration (" +
                "configId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, language TEXT, compilerPath TEXT, compileArgs TEXT, " +
                "sourceFilename TEXT, runCommand TEXT, isInterpreted INTEGER, timeoutSeconds INTEGER);";

        String projectTable = "CREATE TABLE IF NOT EXISTS Project (" +
                "projectId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, createdAt TEXT, lastRunAt TEXT, expectedOutputPath TEXT, " +
                "submissionDir TEXT, runArguments TEXT, configId INTEGER, " +
                "FOREIGN KEY(configId) REFERENCES Configuration(configId));";

        String resultTable = "CREATE TABLE IF NOT EXISTS StudentResult (" +
                "resultId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "projectId INTEGER, studentId TEXT, zipFilename TEXT, " +
                "compileStatus TEXT, runStatus TEXT, compileErrorLog TEXT, " +
                "evaluatedAt TEXT, executionTimeMs INTEGER, " +
                "FOREIGN KEY(projectId) REFERENCES Project(projectId));";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(configTable);
            stmt.execute(projectTable);
            stmt.execute(resultTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}