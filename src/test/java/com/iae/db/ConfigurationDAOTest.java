package com.iae.db;

import com.iae.core.Configuration;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigurationDAO.
 *
 * Her test metodu izole bir SQLite dosyası üzerinde çalışır.
 */
class ConfigurationDAOTest {

    private File dbFile;
    private DatabaseHelper helper;
    private ConfigurationDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = File.createTempFile("iae_test_config_", ".sqlite");
        dbFile.deleteOnExit();
        // DatabaseHelper'ı package-visible constructor ile doğrudan dosyaya yönlendiriyoruz
        helper = new DatabaseHelper(dbFile.getAbsolutePath());
        helper.initialiseSchema();
        dao = new ConfigurationDAO(helper); // bkz. not: package-visible constructor gerekiyor
    }

    @AfterEach
    void tearDown() {
        if (dbFile != null) dbFile.delete();
    }

    // -----------------------------------------------------------------------
    // insert() + findById()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("insert() pozitif ID döndürmeli ve findById() aynı nesneyi geri vermeli")
    void insert_and_findById_roundTrip() {
        Configuration c = buildConfig("C Programming", "C");
        int id = dao.insert(c);

        assertTrue(id > 0, "insert() pozitif ID döndürmeli");

        Configuration found = dao.findById(id);
        assertNotNull(found);
        assertEquals("C Programming", found.getName());
        assertEquals("C", found.getLanguage());
        assertEquals("gcc", found.getCompilerPath());
        assertEquals("-o main", found.getCompileArgs());
        assertEquals("main.c", found.getSourceFilename());
        assertEquals("./main", found.getRunCommand());
        assertFalse(found.isInterpreted());
        assertEquals(30, found.getTimeoutSeconds());
    }

    @Test
    @DisplayName("insert() birden fazla kayıt farklı ID almalı")
    void insert_multipleRecords_uniqueIds() {
        int id1 = dao.insert(buildConfig("Java", "Java"));
        int id2 = dao.insert(buildConfig("Python", "Python"));

        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("findById() olmayan ID için null döndürmeli")
    void findById_nonExistent_returnsNull() {
        assertNull(dao.findById(9999));
    }

    // -----------------------------------------------------------------------
    // findAll()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAll() boş tabloda boş liste döndürmeli")
    void findAll_emptyTable_returnsEmptyList() {
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    @DisplayName("findAll() insert edilen tüm kayıtları döndürmeli")
    void findAll_returnsAllInserted() {
        dao.insert(buildConfig("Java", "Java"));
        dao.insert(buildConfig("Python", "Python"));
        dao.insert(buildConfig("C", "C"));

        List<Configuration> all = dao.findAll();
        assertEquals(3, all.size());
    }

    // -----------------------------------------------------------------------
    // findByName()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByName() var olan isimle doğru kaydı döndürmeli")
    void findByName_existingName_returnsConfig() {
        dao.insert(buildConfig("Java", "Java"));
        Configuration found = dao.findByName("Java");

        assertNotNull(found);
        assertEquals("Java", found.getName());
    }

    @Test
    @DisplayName("findByName() yok olan isimde null döndürmeli")
    void findByName_nonExistent_returnsNull() {
        assertNull(dao.findByName("DoesNotExist"));
    }

    // -----------------------------------------------------------------------
    // update()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update() alanları değiştirmeli, findById() güncel değerleri göstermeli")
    void update_changesFieldsPersist() {
        Configuration c = buildConfig("Old Name", "C");
        int id = dao.insert(c);
        c.setConfigId(id);

        c.setName("New Name");
        c.setCompilerPath("/usr/bin/gcc");
        c.setTimeoutSeconds(60);
        dao.update(c);

        Configuration updated = dao.findById(id);
        assertNotNull(updated);
        assertEquals("New Name", updated.getName());
        assertEquals("/usr/bin/gcc", updated.getCompilerPath());
        assertEquals(60, updated.getTimeoutSeconds());
    }

    @Test
    @DisplayName("update() isInterpreted alanını doğru güncellenmeli")
    void update_isInterpreted_flag() {
        Configuration c = buildConfig("Python", "Python");
        c.setInterpreted(false);
        int id = dao.insert(c);
        c.setConfigId(id);

        c.setInterpreted(true);
        dao.update(c);

        assertTrue(dao.findById(id).isInterpreted());
    }

    // -----------------------------------------------------------------------
    // delete()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("delete() kaydı kaldırmalı, findById() null döndürmeli")
    void delete_removesRecord() {
        int id = dao.insert(buildConfig("ToDelete", "C"));
        assertNotNull(dao.findById(id));

        dao.delete(id);

        assertNull(dao.findById(id));
    }

    @Test
    @DisplayName("delete() olmayan ID için hata fırlatmamalı")
    void delete_nonExistent_noException() {
        assertDoesNotThrow(() -> dao.delete(9999));
    }

    // -----------------------------------------------------------------------
    // count()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("count() insert sayısı ile eşleşmeli")
    void count_matchesInsertions() {
        assertEquals(0, dao.count());
        dao.insert(buildConfig("C", "C"));
        assertEquals(1, dao.count());
        dao.insert(buildConfig("Java", "Java"));
        assertEquals(2, dao.count());
    }

    // -----------------------------------------------------------------------
    // seedDefaultsIfEmpty()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("seedDefaultsIfEmpty() boş tabloya 3 varsayılan konfigürasyon eklemeli")
    void seedDefaultsIfEmpty_addsThreeDefaults() {
        dao.seedDefaultsIfEmpty();
        assertEquals(3, dao.count());
    }

    @Test
    @DisplayName("seedDefaultsIfEmpty() C, Java ve Python konfigürasyonlarını içermeli")
    void seedDefaultsIfEmpty_expectedLanguages() {
        dao.seedDefaultsIfEmpty();
        List<Configuration> all = dao.findAll();

        assertTrue(all.stream().anyMatch(c -> "C".equals(c.getLanguage())),
                "C konfigürasyonu eklenmiş olmalı");
        assertTrue(all.stream().anyMatch(c -> "Java".equals(c.getLanguage())),
                "Java konfigürasyonu eklenmiş olmalı");
        assertTrue(all.stream().anyMatch(c -> "Python".equals(c.getLanguage())),
                "Python konfigürasyonu eklenmiş olmalı");
    }

    @Test
    @DisplayName("seedDefaultsIfEmpty() tablo dolu iken ek kayıt eklememelidir")
    void seedDefaultsIfEmpty_doesNotSeedIfNotEmpty() {
        dao.insert(buildConfig("Existing", "C"));
        dao.seedDefaultsIfEmpty(); // bir kayıt zaten var, seed edilmemeli

        assertEquals(1, dao.count(), "Tablo dolu iken seed işlemi gerçekleşmemeli");
    }

    @Test
    @DisplayName("Python konfigürasyonu isInterpreted=true olarak eklenmeli")
    void seedDefaultsIfEmpty_pythonIsInterpreted() {
        dao.seedDefaultsIfEmpty();
        Configuration python = dao.findByName("Python");
        assertNotNull(python);
        assertTrue(python.isInterpreted());
    }

    @Test
    @DisplayName("C konfigürasyonu isInterpreted=false olarak eklenmeli")
    void seedDefaultsIfEmpty_cIsCompiled() {
        dao.seedDefaultsIfEmpty();
        Configuration c = dao.findByName("C Programming");
        assertNotNull(c);
        assertFalse(c.isInterpreted());
    }

    // -----------------------------------------------------------------------
    // Yardımcı metot
    // -----------------------------------------------------------------------

    private Configuration buildConfig(String name, String language) {
        Configuration c = new Configuration();
        c.setName(name);
        c.setLanguage(language);
        c.setCompilerPath("gcc");
        c.setCompileArgs("-o main");
        c.setSourceFilename("main.c");
        c.setRunCommand("./main");
        c.setInterpreted(false);
        c.setTimeoutSeconds(30);
        return c;
    }
}