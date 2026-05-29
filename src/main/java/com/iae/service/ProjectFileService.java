package com.iae.service;

import com.google.gson.*;
import com.iae.core.Configuration;
import com.iae.core.Project;
import com.iae.core.StudentResult;
import com.iae.db.ConfigurationDAO;
import com.iae.db.ProjectDAO;
import com.iae.db.StudentResultDAO;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;

public class ProjectFileService {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, t, ctx) -> LocalDateTime.parse(json.getAsString()))
            .setPrettyPrinting()
            .create();

    private final ConfigurationDAO configurationDAO = new ConfigurationDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final StudentResultDAO studentResultDAO = new StudentResultDAO();

    private static class ProjectBundle {
        Project project;
        Configuration configuration;
        List<StudentResult> results;
    }

    public void saveProject(Project project, Configuration config,
                            List<StudentResult> results, File target) throws IOException {
        ProjectBundle bundle = new ProjectBundle();
        bundle.project = project;
        bundle.configuration = config;
        bundle.results = results;
        try (Writer writer = new FileWriter(target)) {
            gson.toJson(bundle, writer);
        }
    }

    public Project openProject(File source) throws IOException {
        try (Reader reader = new FileReader(source)) {
            ProjectBundle bundle = gson.fromJson(reader, ProjectBundle.class);

            if (bundle.configuration != null) {
                Configuration existing = configurationDAO.findByName(bundle.configuration.getName());
                int configId = (existing != null)
                        ? existing.getConfigId()
                        : configurationDAO.insert(bundle.configuration);
                bundle.project.setConfigId(configId);
            }

            bundle.project.setProjectId(0);
            projectDAO.insert(bundle.project);

            if (bundle.results != null) {
                for (StudentResult result : bundle.results) {
                    studentResultDAO.insert(bundle.project.getProjectId(), result);
                }
            }

            return bundle.project;
        }
    }
}
