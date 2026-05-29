package com.iae.db;

import org.junit.jupiter.api.*;
import java.io.File;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatabaseHelper.
 *
 * Her test kendi geçici SQLite dosyasını oluşturur; birbirini kirletmez.
 * Test sonunda dosya silinir.
 */
class DatabaseHelperTest {

    private File dbFile;
    private DatabaseHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = File.createTempFile("iae_test_db_", ".sqlite");
        dbFile.deleteOnExit();
        helper = new DatabaseHelper(dbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (dbFile != null) dbFile.delete();
    }

    // -----------------------------------------------------------------------
    // getConnection()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getConnection() geçerli bir Connection döndürmeli")
    void getConnection_returnsOpenConnection() throws SQLException {
        try (Connection conn = helper.getConnection()) {
            assertNotNull(conn, "Connection null olmamalı");
            assertFalse(conn.isClosed(), "Connection açık olmalı");
        }
    }

    @Test
    @DisplayName("getConnection() her çağrıda yeni Connection nesnesi döndürmeli")
    void getConnection_returnsNewInstanceEachTime() throws SQLException {
        try (Connection c1 = helper.getConnection();
             Connection c2 = helper.getConnection()) {
            assertNotSame(c1, c2, "Her çağrı ayrı Connection nesnesi üretmeli");
        }
    }

    // -----------------------------------------------------------------------
    // initialiseSchema()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("initialiseSchema() Configuration tablosunu oluşturmalı")
    void initialiseSchema_createsConfigurationTable() throws SQLException {
        helper.initialiseSchema();
        assertTrue(tableExists(helper.getConnection(), "Configuration"),
                "Configuration tablosu oluşturulmalı");
    }

    @Test
    @DisplayName("initialiseSchema() Project tablosunu oluşturmalı")
    void initialiseSchema_createsProjectTable() throws SQLException {
        helper.initialiseSchema();
        assertTrue(tableExists(helper.getConnection(), "Project"),
                "Project tablosu oluşturulmalı");
    }

    @Test
    @DisplayName("initialiseSchema() StudentResult tablosunu oluşturmalı")
    void initialiseSchema_createsStudentResultTable() throws SQLException {
        helper.initialiseSchema();
        assertTrue(tableExists(helper.getConnection(), "StudentResult"),
                "StudentResult tablosu oluşturulmalı");
    }

    @Test
    @DisplayName("initialiseSchema() iki kez çağrılınca hata fırlatmamalı (IF NOT EXISTS)")
    void initialiseSchema_idempotent() {
        assertDoesNotThrow(() -> {
            helper.initialiseSchema();
            helper.initialiseSchema(); // ikinci çağrı hata üretmemeli
        });
    }

    @Test
    @DisplayName("initialiseSchema() sonrası Configuration tablosu beklenen sütunlara sahip olmalı")
    void initialiseSchema_configurationColumns() throws SQLException {
        helper.initialiseSchema();

        try (Connection conn = helper.getConnection()) {
            ResultSet rs = conn.createStatement()
                    .executeQuery("PRAGMA table_info(Configuration)");

            java.util.Set<String> cols = new java.util.HashSet<>();
            while (rs.next()) cols.add(rs.getString("name"));

            assertTrue(cols.contains("configId"),       "configId sütunu olmalı");
            assertTrue(cols.contains("name"),           "name sütunu olmalı");
            assertTrue(cols.contains("language"),       "language sütunu olmalı");
            assertTrue(cols.contains("compilerPath"),   "compilerPath sütunu olmalı");
            assertTrue(cols.contains("compileArgs"),    "compileArgs sütunu olmalı");
            assertTrue(cols.contains("sourceFilename"), "sourceFilename sütunu olmalı");
            assertTrue(cols.contains("runCommand"),     "runCommand sütunu olmalı");
            assertTrue(cols.contains("isInterpreted"),  "isInterpreted sütunu olmalı");
            assertTrue(cols.contains("timeoutSeconds"), "timeoutSeconds sütunu olmalı");
        }
    }

    @Test
    @DisplayName("initialiseSchema() sonrası Project tablosu configId FK sütununa sahip olmalı")
    void initialiseSchema_projectHasConfigIdColumn() throws SQLException {
        helper.initialiseSchema();

        try (Connection conn = helper.getConnection()) {
            ResultSet rs = conn.createStatement()
                    .executeQuery("PRAGMA table_info(Project)");

            java.util.Set<String> cols = new java.util.HashSet<>();
            while (rs.next()) cols.add(rs.getString("name"));

            assertTrue(cols.contains("configId"), "Project tablosunda configId FK sütunu olmalı");
        }
    }

    // -----------------------------------------------------------------------
    // closeConnection()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("closeConnection() açık bağlantıyı kapatmalı")
    void closeConnection_closesOpenConnection() throws SQLException {
        Connection conn = helper.getConnection();
        assertFalse(conn.isClosed());
        helper.closeConnection(conn);
        assertTrue(conn.isClosed(), "Bağlantı kapatılmış olmalı");
    }

    @Test
    @DisplayName("closeConnection(null) hata fırlatmamalı")
    void closeConnection_nullSafe() {
        assertDoesNotThrow(() -> helper.closeConnection(null));
    }

    @Test
    @DisplayName("closeConnection() zaten kapalı bağlantıda hata fırlatmamalı")
    void closeConnection_alreadyClosed() throws SQLException {
        Connection conn = helper.getConnection();
        conn.close();
        assertDoesNotThrow(() -> helper.closeConnection(conn));
    }

    // -----------------------------------------------------------------------
    // Yardımcı metot
    // -----------------------------------------------------------------------

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}