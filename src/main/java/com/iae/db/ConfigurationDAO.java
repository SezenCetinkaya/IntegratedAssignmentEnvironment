package com.iae.db;

import com.iae.core.Configuration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationDAO {
    private final DatabaseHelper dbHelper = new DatabaseHelper();

    public int insert(Configuration config) {
        String sql = "INSERT INTO Configuration(name, language, compilerPath, compileArgs, sourceFilename, runCommand, isInterpreted, timeoutSeconds) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, config.getName());
            pstmt.setString(2, config.getLanguage());
            pstmt.setString(3, config.getCompilerPath());
            pstmt.setString(4, config.getCompileArgs());
            pstmt.setString(5, config.getSourceFilename());
            pstmt.setString(6, config.getRunCommand());
            pstmt.setInt(7, config.isInterpreted() ? 1 : 0);
            pstmt.setInt(8, config.getTimeoutSeconds());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM Configuration";
        try (Connection conn = dbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Inserts built-in C, Java, and Python templates when the database has no configurations yet.
     */
    public void seedDefaultsIfEmpty() {
        if (count() > 0) {
            return;
        }

        // NOT: CommandRunner.compile() her zaman sourceFilename'i sona ekler.
        // Bu yüzden compileArgs içine kaynak dosya adı YAZILMAMALI.
        // Üretilen komut: [compilerPath] [compileArgs...] [sourceFilename]

        Configuration c = new Configuration();
        c.setName("C Programming");
        c.setLanguage("C");
        c.setCompilerPath("gcc");
        c.setCompileArgs("-o main");        // gcc -o main main.c
        c.setSourceFilename("main.c");
        c.setRunCommand("main.exe");
        c.setInterpreted(false);
        c.setTimeoutSeconds(30);
        insert(c);

        Configuration java = new Configuration();
        java.setName("Java");
        java.setLanguage("Java");
        java.setCompilerPath("javac");
        java.setCompileArgs("");            // javac Main.java
        java.setSourceFilename("Main.java");
        java.setRunCommand("java -cp . Main");
        java.setInterpreted(false);
        java.setTimeoutSeconds(45);
        insert(java);

        Configuration python = new Configuration();
        python.setName("Python");
        python.setLanguage("Python");
        python.setCompilerPath("python");
        python.setCompileArgs("");
        python.setSourceFilename("main.py");
        python.setRunCommand("python main.py");
        python.setInterpreted(true);
        python.setTimeoutSeconds(30);
        insert(python);
    }

    public List<Configuration> findAll() {
        List<Configuration> configs = new ArrayList<>();
        String sql = "SELECT * FROM Configuration";
        try (Connection conn = dbHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                configs.add(mapResultSetToConfig(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return configs;
    }

    public Configuration findById(int id) {
        String sql = "SELECT * FROM Configuration WHERE configId = ?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToConfig(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Configuration config) {
        String sql = "UPDATE Configuration SET name=?, language=?, compilerPath=?, compileArgs=?, "
                + "sourceFilename=?, runCommand=?, isInterpreted=?, timeoutSeconds=? WHERE configId=?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, config.getName());
            pstmt.setString(2, config.getLanguage());
            pstmt.setString(3, config.getCompilerPath());
            pstmt.setString(4, config.getCompileArgs());
            pstmt.setString(5, config.getSourceFilename());
            pstmt.setString(6, config.getRunCommand());
            pstmt.setInt(7, config.isInterpreted() ? 1 : 0);
            pstmt.setInt(8, config.getTimeoutSeconds());
            pstmt.setInt(9, config.getConfigId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Configuration findByName(String name) {
        String sql = "SELECT * FROM Configuration WHERE name = ?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToConfig(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Configuration mapResultSetToConfig(ResultSet rs) throws SQLException {
        Configuration c = new Configuration();
        c.setConfigId(rs.getInt("configId"));
        c.setName(rs.getString("name"));
        c.setLanguage(rs.getString("language"));
        c.setCompilerPath(rs.getString("compilerPath"));
        c.setCompileArgs(rs.getString("compileArgs"));
        c.setSourceFilename(rs.getString("sourceFilename"));
        c.setRunCommand(rs.getString("runCommand"));
        c.setInterpreted(rs.getInt("isInterpreted") == 1);
        c.setTimeoutSeconds(rs.getInt("timeoutSeconds"));
        return c;
    }

    public void delete(int id) {
        String sql = "DELETE FROM Configuration WHERE configId = ?";
        try (Connection conn = dbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}