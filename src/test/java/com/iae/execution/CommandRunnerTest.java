package com.iae.execution;

import com.iae.core.Configuration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class CommandRunnerTest {

    @Test
    void testSuccessfulCommand() {
        CommandRunner runner = new CommandRunner();
        String[] cmd = System.getProperty("os.name").toLowerCase().contains("win")
                ? new String[]{"cmd", "/c", "echo", "hello"}
                : new String[]{"echo", "hello"};

        ProcessResult result = runner.run(cmd, "", new File("."));
        assertEquals(0, result.getExitCode(), "Exit code should be 0");
        assertTrue(result.getStdout().trim().contains("hello"), "Stdout should contain hello");
        assertFalse(result.isTimedOut(), "Should not timeout");
    }

    @Test
    void testNonExistentCommand() {
        CommandRunner runner = new CommandRunner();
        String[] cmd = new String[]{"nonexistentcommand12345"};

        ProcessResult result = runner.run(cmd, "", new File("."));
        assertTrue(result.getExitCode() != 0 || !result.getStderr().isEmpty(),
                "Exit code should be non-zero or stderr should not be empty");
    }

    @Test
    void testCommandTimeout() {
        CommandRunner runner = new CommandRunner();
        runner.setTimeoutSeconds(1);

        String[] cmd;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            cmd = new String[]{"ping", "127.0.0.1", "-n", "5"};
        } else {
            cmd = new String[]{"sleep", "3"};
        }

        ProcessResult result = runner.run(cmd, "", new File("."));
        assertTrue(result.isTimedOut(), "Command should timeout");
        assertTrue(result.hasFailed(), "Timed out command should be considered failed");
    }

    @Test
    void testRunWithArguments() {
        CommandRunner runner = new CommandRunner();
        String[] cmd = System.getProperty("os.name").toLowerCase().contains("win")
                ? new String[]{"cmd", "/c", "echo"}
                : new String[]{"echo"};
        String runArguments = "alpha beta";

        ProcessResult result = runner.run(cmd, runArguments, new File("."));
        assertEquals(0, result.getExitCode(), "Exit code should be 0");
        String stdout = result.getStdout();
        assertTrue(stdout.contains("alpha"), "Stdout should contain alpha");
        assertTrue(stdout.contains("beta"), "Stdout should contain beta");
    }

    @Test
    void testCompileInterpretedLanguage() {
        CommandRunner runner = new CommandRunner();
        Configuration config = new Configuration();
        config.setInterpreted(true);

        ProcessResult result = runner.compile(config, new File("."));
        assertEquals(0, result.getExitCode(), "Interpreted language compile should return 0");
        assertEquals("", result.getStderr(), "Interpreted language compile should have empty stderr");
        assertTrue(result.isSuccessful(), "Interpreted language compile should be successful");
    }
}
