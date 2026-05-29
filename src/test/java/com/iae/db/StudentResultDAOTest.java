package com.iae.db;

import com.iae.core.Configuration;
import com.iae.core.Project;
import com.iae.core.StudentResult;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StudentResultDAO.
 */
class StudentResultDAOTest {

    private File dbFile;
    private DatabaseHelper helper;
    private ConfigurationDAO configDAO;
    private ProjectDAO projectDAO;
    private StudentResultDAO resultDAO;

    private int projectId;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = File.createTempFile("iae_test_result_", ".sqlite");
        dbFile.deleteOnExit();
        helper = new DatabaseHelper(dbFile.getAbsolutePath());
        helper.initialiseSchema();

        configDAO = new ConfigurationDAO(helper);
        projectDAO = new ProjectDAO(helper);
        resultDAO  = new StudentResultDAO(helper);

        // Her test için ortak proje
        Configuration c = new Configuration();
        c.setName("Java");
        c.setLanguage("Java");
        c.setCompilerPath("javac");
        c.setCompileArgs("");
        c.setSourceFilename("Main.java");
        c.setRunCommand("java Main");
        c.setInterpreted(false);
        c.setTimeoutSeconds(30);
        int configId = configDAO.insert(c);

        Project p = new Project();
        p.setName("Test Project");
        p.setCreatedAt(LocalDateTime.now());
        p.setConfigId(configId);
        p.setExpectedOutputPath("/expected.txt");
        p.setSubmissionDir("/submissions");
        p.setRunArguments("");
        projectId = projectDAO.insert(p);
        assertTrue(projectId > 0, "Seed proje ID'si pozitif olmalı");
    }

    @AfterEach
    void tearDown() {
        if (dbFile != null) dbFile.delete();
    }

    // -----------------------------------------------------------------------
    // insert() + findByProject()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("insert() + findByProject() round-trip: temel alanlar doğru geri gelmeli")
    void insertAndFindByProject_basicFields() {
        StudentResult r = buildResult("20220601001", "COMPILE_SUCCESS", "PASS", 150);
        resultDAO.insert(projectId, r);

        List<StudentResult> results = resultDAO.findByProject(projectId);
        assertEquals(1, results.size());

        StudentResult found = results.get(0);
        assertEquals("20220601001", found.getStudentId());
        assertEquals("COMPILE_SUCCESS", found.getCompileStatus());
        assertEquals("PASS", found.getRunStatus());
        assertEquals(150, found.getExecutionTimeMs());
    }

    @Test
    @DisplayName("insert() birden fazla öğrenci kaydı aynı projeye eklenebilmeli")
    void insert_multipleResults_sameProject() {
        resultDAO.insert(projectId, buildResult("S001", "COMPILE_SUCCESS", "PASS", 100));
        resultDAO.insert(projectId, buildResult("S002", "COMPILE_SUCCESS", "FAIL", 200));
        resultDAO.insert(projectId, buildResult("S003", "COMPILE_ERROR",   "NOT_RUN", null));

        List<StudentResult> results = resultDAO.findByProject(projectId);
        assertEquals(3, results.size());
    }

    @Test
    @DisplayName("executionTimeMs null olarak insert edildiğinde null olarak geri gelmeli")
    void insert_nullExecutionTime_persisted() {
        StudentResult r = buildResult("S_NULL", "COMPILE_ERROR", "NOT_RUN", null);
        resultDAO.insert(projectId, r);

        List<StudentResult> results = resultDAO.findByProject(projectId);
        assertEquals(1, results.size());
        assertNull(results.get(0).getExecutionTimeMs(), "null executionTimeMs saklanıp geri gelmeli");
    }

    @Test
    @DisplayName("compileErrorLog alanı insert edilip geri gelmeli")
    void insert_errorLog_persisted() {
        StudentResult r = buildResult("S_ERR", "COMPILE_ERROR", "NOT_RUN", null);
        r.setCompileErrorLog("error: ';' expected\n  at line 5");
        resultDAO.insert(projectId, r);

        List<StudentResult> found = resultDAO.findByProject(projectId);
        assertEquals(1, found.size());
        assertTrue(found.get(0).getCompileErrorLog().contains("error: ';' expected"));
    }

    @Test
    @DisplayName("zipFilename ve evaluatedAt alanları persist edilmeli")
    void insert_zipFilenameAndEvaluatedAt_persisted() {
        StudentResult r = buildResult("S_ZIP", "COMPILE_SUCCESS", "PASS", 80);
        r.setZipFilename("20220601001.zip");
        r.setEvaluatedAt("2024-05-01T10:30:00");
        resultDAO.insert(projectId, r);

        StudentResult found = resultDAO.findByProject(projectId).get(0);
        assertEquals("20220601001.zip", found.getZipFilename());
        assertEquals("2024-05-01T10:30:00", found.getEvaluatedAt());
    }

    // -----------------------------------------------------------------------
    // findByProject() — kenar durumlar
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByProject() kayıt olmayan proje ID'si için boş liste döndürmeli")
    void findByProject_noResults_returnsEmptyList() {
        List<StudentResult> results = resultDAO.findByProject(9999);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("findByProject() yalnızca ilgili projenin sonuçlarını getirmeli")
    void findByProject_isolation() {
        // İkinci proje oluştur
        Project p2 = new Project();
        p2.setName("Other Project");
        p2.setCreatedAt(LocalDateTime.now());
        p2.setConfigId(1);
        p2.setExpectedOutputPath("/e.txt");
        p2.setSubmissionDir("/s");
        p2.setRunArguments("");
        int otherId = projectDAO.insert(p2);

        resultDAO.insert(projectId, buildResult("S_MAIN", "COMPILE_SUCCESS", "PASS", 100));
        resultDAO.insert(otherId,   buildResult("S_OTHER", "COMPILE_SUCCESS", "FAIL", 200));

        List<StudentResult> mainResults = resultDAO.findByProject(projectId);
        assertEquals(1, mainResults.size(), "Sadece birinci projenin sonuçları gelmeli");
        assertEquals("S_MAIN", mainResults.get(0).getStudentId());
    }

    @Test
    @DisplayName("resultId alanı > 0 olarak atanmış olmalı")
    void findByProject_resultId_isPositive() {
        resultDAO.insert(projectId, buildResult("S_ID", "COMPILE_SUCCESS", "PASS", 50));
        StudentResult found = resultDAO.findByProject(projectId).get(0);
        assertTrue(found.getResultId() > 0, "DB'den dönen resultId pozitif olmalı");
    }

    // -----------------------------------------------------------------------
    // compileStatus değerleri — tam set testi
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Farklı compileStatus ve runStatus değerleri doğru persist edilmeli")
    void insert_variousStatuses() {
        String[][] cases = {
                {"COMPILE_SUCCESS", "PASS"},
                {"COMPILE_SUCCESS", "FAIL"},
                {"COMPILE_SUCCESS", "RUNTIME_ERROR"},
                {"COMPILE_ERROR",   "NOT_RUN"},
                {"FILE_NOT_FOUND",  "NOT_RUN"},
                {"EXTRACTION_ERROR","NOT_RUN"},
        };

        for (String[] pair : cases) {
            StudentResult r = new StudentResult();
            r.setStudentId("S_" + pair[0] + "_" + pair[1]);
            r.setCompileStatus(pair[0]);
            r.setRunStatus(pair[1]);
            resultDAO.insert(projectId, r);
        }

        List<StudentResult> results = resultDAO.findByProject(projectId);
        assertEquals(cases.length, results.size());

        for (int i = 0; i < cases.length; i++) {
            // En az bir sonuç beklenen statüslere sahip olmalı
            final String compile = cases[i][0];
            final String run     = cases[i][1];
            assertTrue(
                    results.stream().anyMatch(r -> compile.equals(r.getCompileStatus())
                            && run.equals(r.getRunStatus())),
                    "Statü çifti bulunamadı: " + compile + " / " + run
            );
        }
    }

    // -----------------------------------------------------------------------
    // Yardımcı metot
    // -----------------------------------------------------------------------

    private StudentResult buildResult(String studentId,
                                      String compileStatus,
                                      String runStatus,
                                      Integer execMs) {
        StudentResult r = new StudentResult();
        r.setStudentId(studentId);
        r.setZipFilename(studentId + ".zip");
        r.setCompileStatus(compileStatus);
        r.setRunStatus(runStatus);
        r.setExecutionTimeMs(execMs);
        r.setEvaluatedAt(LocalDateTime.now().toString());
        return r;
    }
}