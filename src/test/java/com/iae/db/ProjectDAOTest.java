package com.iae.db;

import com.iae.core.Configuration;
import com.iae.core.Project;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProjectDAO.
 */
class ProjectDAOTest {

    private File dbFile;
    private DatabaseHelper helper;
    private ConfigurationDAO configDAO;
    private ProjectDAO projectDAO;
    private int seedConfigId;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = File.createTempFile("iae_test_project_", ".sqlite");
        dbFile.deleteOnExit();
        helper = new DatabaseHelper(dbFile.getAbsolutePath());
        helper.initialiseSchema();

        configDAO = new ConfigurationDAO(helper);
        projectDAO = new ProjectDAO(helper);

        // Testlerde gerekli olan Configuration kaydı
        Configuration c = new Configuration();
        c.setName("Test Config");
        c.setLanguage("Java");
        c.setCompilerPath("javac");
        c.setCompileArgs("");
        c.setSourceFilename("Main.java");
        c.setRunCommand("java Main");
        c.setInterpreted(false);
        c.setTimeoutSeconds(30);
        seedConfigId = configDAO.insert(c);
        assertTrue(seedConfigId > 0);
    }

    @AfterEach
    void tearDown() {
        if (dbFile != null) dbFile.delete();
    }

    // -----------------------------------------------------------------------
    // insert() + findById()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("insert() pozitif ID döndürmeli")
    void insert_returnsPositiveId() {
        Project p = buildProject("Assignment 1");
        int id = projectDAO.insert(p);
        assertTrue(id > 0);
    }

    @Test
    @DisplayName("insert() Project.projectId alanını güncellemelidir")
    void insert_setsProjectIdOnObject() {
        Project p = buildProject("Assignment 1");
        int id = projectDAO.insert(p);
        assertEquals(id, p.getProjectId(), "insert() sonrası nesnenin projectId'si set edilmeli");
    }

    @Test
    @DisplayName("findById() insert edilen projeyi tam olarak geri vermeli")
    void findById_roundTrip() {
        Project p = buildProject("CE316 HW1");
        p.setExpectedOutputPath("/outputs/expected.txt");
        p.setSubmissionDir("/submissions/hw1");
        p.setRunArguments("--verbose");
        int id = projectDAO.insert(p);

        Project found = projectDAO.findById(id);
        assertNotNull(found);
        assertEquals("CE316 HW1", found.getName());
        assertEquals("/outputs/expected.txt", found.getExpectedOutputPath());
        assertEquals("/submissions/hw1", found.getSubmissionDir());
        assertEquals("--verbose", found.getRunArguments());
        assertEquals(seedConfigId, found.getConfigId());
    }

    @Test
    @DisplayName("findById() olmayan ID için null döndürmeli")
    void findById_nonExistent_returnsNull() {
        assertNull(projectDAO.findById(9999));
    }

    @Test
    @DisplayName("createdAt alanı null olmadan persist edilip parse edilmeli")
    void insert_createdAt_persistedAndParsed() {
        LocalDateTime before = LocalDateTime.now().withNano(0);

        Project p = buildProject("TimestampTest");
        p.setCreatedAt(before);
        int id = projectDAO.insert(p);

        Project found = projectDAO.findById(id);
        assertNotNull(found.getCreatedAt());
        // Nanosaniye hassasiyeti farkını tolere et; saniye düzeyinde eşit olmalı
        assertEquals(before.withNano(0), found.getCreatedAt().withNano(0));
    }

    // -----------------------------------------------------------------------
    // findAll()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAll() boş tabloda boş liste döndürmeli")
    void findAll_emptyTable() {
        assertTrue(projectDAO.findAll().isEmpty());
    }

    @Test
    @DisplayName("findAll() insert edilen tüm projeleri döndürmeli")
    void findAll_returnsAllInserted() {
        projectDAO.insert(buildProject("HW1"));
        projectDAO.insert(buildProject("HW2"));
        projectDAO.insert(buildProject("HW3"));

        List<Project> all = projectDAO.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("findAll() insert sonrası çağrıldığında yeni kaydı da içermeli (no cache)")
    void findAll_noCachingBehavior() {
        projectDAO.insert(buildProject("First"));
        assertEquals(1, projectDAO.findAll().size());

        projectDAO.insert(buildProject("Second"));
        assertEquals(2, projectDAO.findAll().size(), "findAll() her çağrıda taze veri döndürmeli");
    }

    // -----------------------------------------------------------------------
    // update()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update() name alanını değiştirmeli")
    void update_name_persists() {
        Project p = buildProject("OldName");
        int id = projectDAO.insert(p);
        p.setName("NewName");
        p.setLastRunAt(LocalDateTime.now().toString());
        projectDAO.update(p);

        assertEquals("NewName", projectDAO.findById(id).getName());
    }

    @Test
    @DisplayName("update() submissionDir ve expectedOutputPath değişikliklerini persist etmeli")
    void update_paths_persist() {
        Project p = buildProject("PathTest");
        int id = projectDAO.insert(p);

        p.setSubmissionDir("/new/dir");
        p.setExpectedOutputPath("/new/expected.txt");
        projectDAO.update(p);

        Project updated = projectDAO.findById(id);
        assertEquals("/new/dir", updated.getSubmissionDir());
        assertEquals("/new/expected.txt", updated.getExpectedOutputPath());
    }

    @Test
    @DisplayName("update() configId değişikliğini persist etmeli")
    void update_configId_persists() {
        // İkinci bir config ekle
        Configuration c2 = new Configuration();
        c2.setName("C Config");
        c2.setLanguage("C");
        c2.setCompilerPath("gcc");
        c2.setCompileArgs("-o main");
        c2.setSourceFilename("main.c");
        c2.setRunCommand("./main");
        c2.setInterpreted(false);
        c2.setTimeoutSeconds(20);
        int newConfigId = configDAO.insert(c2);

        Project p = buildProject("ConfigChangeTest");
        int id = projectDAO.insert(p);
        p.setConfigId(newConfigId);
        projectDAO.update(p);

        assertEquals(newConfigId, projectDAO.findById(id).getConfigId());
    }

    @Test
    @DisplayName("update() var olmayan ID ile çağrıldığında hata fırlatmamalı")
    void update_nonExistentId_noException() {
        Project ghost = buildProject("Ghost");
        ghost.setProjectId(9999);
        assertDoesNotThrow(() -> projectDAO.update(ghost));
    }

    // -----------------------------------------------------------------------
    // Yardımcı metot
    // -----------------------------------------------------------------------

    private Project buildProject(String name) {
        Project p = new Project();
        p.setName(name);
        p.setCreatedAt(LocalDateTime.now());
        p.setExpectedOutputPath("/default/expected.txt");
        p.setSubmissionDir("/default/submissions");
        p.setRunArguments("");
        p.setConfigId(seedConfigId);
        return p;
    }
}