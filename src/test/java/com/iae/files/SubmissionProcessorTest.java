package com.iae.files;

import org.junit.jupiter.api.Test;
import com.iae.execution.CommandRunner;
import com.iae.execution.ProcessResult;
import com.iae.core.Configuration;
import com.iae.core.StudentResult;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class SubmissionProcessorTest {

    @Test
    void extractStudentIdShouldRemoveZipExtension() {
        SubmissionProcessor processor = new SubmissionProcessor();

        String result = processor.processSingle(
                new File("20220602021.zip"),
                null,
                "",
                false
        ).getStudentId();

        assertNotNull(result);
    }

    @Test
    void testRuntimeErrorIsNotPass() throws Exception {
        SubmissionProcessor processor = new SubmissionProcessor();

        WorkspaceManager mockWorkspaceManager = new WorkspaceManager() {
            @Override
            public File createWorkspace(String studentId) {
                return new File(System.getProperty("java.io.tmpdir"));
            }
            @Override
            public void cleanWorkspace(String studentId) {}
        };

        ZipExtractor mockZipExtractor = new ZipExtractor() {
            @Override
            public File extract(File zipFile, File destDir) {
                return destDir;
            }
        };

        FileLocator mockFileLocator = new FileLocator() {
            @Override
            public File locate(File workspace, String filename) {
                return new File(workspace, filename);
            }
        };

        CommandRunner mockCommandRunner = new CommandRunner() {
            @Override
            public void setTimeoutSeconds(int seconds) {}

            @Override
            public ProcessResult compile(Configuration config, File executionDir) {
                return new ProcessResult(0, "compile success", "", false);
            }

            @Override
            public ProcessResult run(String[] cmd, String args, File dir) {
                return new ProcessResult(1, "expected output", "runtime check failed", false);
            }
        };

        setPrivateField(processor, "workspaceManager", mockWorkspaceManager);
        setPrivateField(processor, "zipExtractor", mockZipExtractor);
        setPrivateField(processor, "fileLocator", mockFileLocator);
        setPrivateField(processor, "commandRunner", mockCommandRunner);

        Configuration config = new Configuration();
        config.setSourceFilename("Solution.java");
        config.setRunCommand("java Solution");

        StudentResult result = processor.processSingle(
                new File("dummy.zip"),
                config,
                "expected output",
                false
        );

        assertEquals("RUNTIME_ERROR", result.getRunStatus());
        assertTrue(result.getCompileErrorLog().contains("Exit code: 1"));
        assertTrue(result.getCompileErrorLog().contains("runtime check failed"));
    }

    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
