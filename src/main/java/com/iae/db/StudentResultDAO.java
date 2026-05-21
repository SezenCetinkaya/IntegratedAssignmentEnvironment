package com.iae.db;

import com.iae.core.StudentResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentResultDAO {
    private final DatabaseHelper dbHelper = new DatabaseHelper();

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
                // ============================================================
                // TODO [OWNER: Sıla Karabağ (DB)] [PHASE: 3] [REQ: 4]
                // GÖREV: executionTimeMs için null-safe okuma kullan
                // AÇIKLAMA:
                //   rs.getInt() SQL NULL'u 0 olarak döndürür. Hiç çalıştırılmamış (NOT_RUN)
                //   öğrenciler için "0 ms" gösterilir — yanıltıcı.
                // ADIMLAR:
                //   1. Şu satırı: res.setExecutionTimeMs(rs.getInt("executionTimeMs"));
                //      Şununla değiştir: int ms = rs.getInt("executionTimeMs");
                //                        res.setExecutionTimeMs(rs.wasNull() ? null : ms);
                //   2. Veya: res.setExecutionTimeMs(rs.getObject("executionTimeMs", Integer.class));
                // KABUL KRİTERİ:
                //   NOT_RUN öğrenciler için executionTimeMs null kalıyor, UI boş hücre gösteriyor.
                // ============================================================
                res.setExecutionTimeMs(rs.getInt("executionTimeMs"));
                results.add(res);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}