package com.iae.gui;

import com.iae.db.ConfigurationDAO;
import com.iae.gui.support.TestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationSeedTest {

    private final TestDatabase testDatabase = new TestDatabase();

    @BeforeEach
    void setUp() throws Exception {
        testDatabase.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        testDatabase.tearDown();
    }

    @Test
    void seedDefaultsIfEmpty_insertsThreeTemplates() {
        ConfigurationDAO dao = new ConfigurationDAO();
        assertEquals(0, dao.count());

        dao.seedDefaultsIfEmpty();

        assertEquals(5, dao.count());
        assertNotNull(dao.findByName("C Programming"));
        assertNotNull(dao.findByName("Java"));
        assertNotNull(dao.findByName("Python"));
        assertNotNull(dao.findByName("Haskell"));
        assertNotNull(dao.findByName("Prolog"));
    }

    @Test
    void seedDefaultsIfEmpty_isIdempotent() {
        ConfigurationDAO dao = new ConfigurationDAO();
        dao.seedDefaultsIfEmpty();
        dao.seedDefaultsIfEmpty();

        assertEquals(5, dao.count());
    }

    @Test
    void seededCConfiguration_hasExpectedFields() {
        ConfigurationDAO dao = new ConfigurationDAO();
        dao.seedDefaultsIfEmpty();

        var c = dao.findByName("C Programming");
        assertNotNull(c);
        assertEquals("gcc", c.getCompilerPath());
        assertEquals("main.c", c.getSourceFilename());
        assertFalse(c.isInterpreted());
    }
}
