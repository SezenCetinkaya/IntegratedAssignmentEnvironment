package com.iae.db;

import com.iae.core.StudentResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentResultDAO {
    private final DatabaseHelper dbHelper;

    public StudentResultDAO() {
        this(new DatabaseHelper());
    }

    StudentResultDAO(DatabaseHelper dbHelper) {    // package-visible
        this.dbHelper = dbHelper;
    }

    public void insert(int projectId, StudentResult result) {
        String sql = "INSERT INTO StudentResult(projectId, studentId, zipFilename, compileStatus, runStatus, compileErrorLog, evaluatedAt, executionTimeMs) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            pstmt.setString(2, result.getStudentId());
            pstmt.setString(3, result.getZipFilename());
            pstmt.setString(4, result.getCompileStatus());
            pstmt.setString(5, result.getRunStatus());
            pstmt.setString(6, result.getCompileErrorLog());
            pstmt.setString(7, result.getEvaluatedAt());
            pstmt.setObject(8, result.getExecutionTimeMs());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<StudentResult> findByProject(int projectId) {
        List<StudentResult> results = new ArrayList<>();
        String sql = "SELECT * FROM StudentResult WHERE projectId = ?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                StudentResult res = new StudentResult();
                res.setResultId(rs.getInt("resultId"));
                res.setStudentId(rs.getString("studentId"));
                res.setZipFilename(rs.getString("zipFilename"));
                res.setCompileStatus(rs.getString("compileStatus"));
                res.setRunStatus(rs.getString("runStatus"));
                res.setCompileErrorLog(rs.getString("compileErrorLog"));
                res.setEvaluatedAt(rs.getString("evaluatedAt"));

                // Senin düzelttiğin null-safe süre okuma mantığı
                int ms = rs.getInt("executionTimeMs");
                res.setExecutionTimeMs(rs.wasNull() ? null : ms);

                results.add(res);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}