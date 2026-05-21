package com.iae.db;

import com.iae.core.Project;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {
    private final DatabaseHelper dbHelper = new DatabaseHelper();

    public int insert(Project project) {
        String sql = "INSERT INTO Project(name, createdAt, lastRunAt, expectedOutputPath, submissionDir, runArguments, configId) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getCreatedAt() != null ? project.getCreatedAt().toString() : LocalDateTime.now().toString());
            pstmt.setString(3, project.getLastRunAt());
            pstmt.setString(4, project.getExpectedOutputPath());
            pstmt.setString(5, project.getSubmissionDir());
            pstmt.setString(6, project.getRunArguments());
            pstmt.setInt(7, project.getConfigId());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                project.setProjectId(id);
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ============================================================
    // TODO [OWNER: Sıla Karabağ (DB)] [PHASE: 1] [REQ: 3, 10]
    // GÖREV: findAll() cache yapma, her çağrıda DB sorgusu çalıştır
    // AÇIKLAMA:
    //   Eğer ileride statik cache eklenirse, Open Project dialog'u bayat veri gösterebilir.
    //   Her çağrıda taze ResultSet ile dönüleceği garanti altına alınmalı.
    // ADIMLAR:
    //   1. Eğer static List<Project> cache field'i varsa kaldır.
    //   2. Her çağrıda taze ResultSet ile döndür (mevcut implementasyon bu şekilde —
    //      ilerleyen refactoring'lerde bu davranışı bozmamaya dikkat et).
    // KABUL KRİTERİ:
    //   Aynı session içinde bir proje insert/update edildiğinde, bir sonraki findAll() onu da içeriyor.
    // ============================================================
    public List<Project> findAll() {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM Project";
        try (Connection conn = dbHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projects;
    }

    public Project findById(int id) {
        String sql = "SELECT * FROM Project WHERE projectId = ?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToProject(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Project project) {
        String sql = "UPDATE Project SET name=?, lastRunAt=?, expectedOutputPath=?, submissionDir=?, runArguments=?, configId=? WHERE projectId=?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getLastRunAt());
            pstmt.setString(3, project.getExpectedOutputPath());
            pstmt.setString(4, project.getSubmissionDir());
            pstmt.setString(5, project.getRunArguments());
            pstmt.setInt(6, project.getConfigId());
            pstmt.setInt(7, project.getProjectId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("projectId"));
        p.setName(rs.getString("name"));
        String createdAtStr = rs.getString("createdAt");
        if (createdAtStr != null) p.setCreatedAt(LocalDateTime.parse(createdAtStr));
        p.setLastRunAt(rs.getString("lastRunAt"));
        p.setExpectedOutputPath(rs.getString("expectedOutputPath"));
        p.setSubmissionDir(rs.getString("submissionDir"));
        p.setRunArguments(rs.getString("runArguments"));
        p.setConfigId(rs.getInt("configId"));
        return p;
    }
}