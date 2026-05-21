package com.iae.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ResourceExtractor {

    public static File extractResource(String resourcePath, String prefix) throws IOException {
        try (InputStream in = ResourceExtractor.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new FileNotFoundException(resourcePath);
            File temp = File.createTempFile(prefix, ".html");
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }
}
