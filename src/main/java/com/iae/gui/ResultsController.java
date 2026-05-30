package com.iae.gui;

import com.iae.core.Project;
import com.iae.core.Report;
import com.iae.core.StudentResult;
import com.iae.db.StudentResultDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import java.util.List;

public class ResultsController {

    @FXML private TableView<StudentResult> resultsTable;
    @FXML private TableColumn<StudentResult, String> colStudentId;
    @FXML private TableColumn<StudentResult, String> colZipFile;
    @FXML private TableColumn<StudentResult, String> colCompile;
    @FXML private TableColumn<StudentResult, String> colRun;
    @FXML private TableColumn<StudentResult, String> colTime;
    @FXML private TableColumn<StudentResult, String> colEvaluated;
    @FXML private TextArea errorLogArea;
    @FXML private Label statTotalLabel;
    @FXML private Label statPassLabel;
    @FXML private Label statFailLabel;
    @FXML private Label statErrorLabel;

    private final ObservableList<StudentResult> tableData = FXCollections.observableArrayList();
    private final StudentResultDAO resultDAO = new StudentResultDAO();
    private Project boundProject;

    @FXML
    public void initialize() {
        colStudentId.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getStudentId())));
        colZipFile.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getZipFilename())));
        colCompile.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getCompileStatus())));
        colRun.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getRunStatus())));
        colTime.setCellValueFactory(c -> {
            Integer ms = c.getValue().getExecutionTimeMs();
            return new SimpleStringProperty(ms == null ? "—" : String.valueOf(ms));
        });
        colEvaluated.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getEvaluatedAt())));

        resultsTable.setItems(tableData);
        resultsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentResult item, boolean empty) {
                super.updateItem(item, empty);
                updateRowStyle();
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateRowStyle();
            }

            private void updateRowStyle() {
                StudentResult item = getItem();
                boolean empty = isEmpty();
                getStyleClass().removeAll("row-pass", "row-fail", "row-error");
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                String run = nullSafe(item.getRunStatus());
                String compile = nullSafe(item.getCompileStatus());
                if ("PASS".equalsIgnoreCase(run)) {
                    getStyleClass().add("row-pass");
                } else if ("FAIL".equalsIgnoreCase(run)) {
                    getStyleClass().add("row-fail");
                } else if (compile.contains("ERROR") || run.contains("ERROR")
                        || "FILE NOT FOUND".equalsIgnoreCase(run)
                        || "EXTRACTION ERROR".equalsIgnoreCase(run)) {
                    getStyleClass().add("row-error");
                }

                if (isSelected()) {
                    setStyle("-fx-text-background-color: white; -fx-text-fill: white; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-background-color: #2c3e50; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                }
            }
        });

        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            String text;
            if (selected == null) {
                text = "";
            } else {
                String log = selected.getCompileErrorLog();
                text = log == null || log.isBlank() ? "(no errors recorded)" : log;
            }
            animateErrorLog(text);
        });

        UiAnimations.applyCardShadow(resultsTable);
    }

    public void playEntranceAnimation() {
        UiAnimations.fadeInUp(resultsTable, Duration.millis(200));
        UiAnimations.fadeInUp(errorLogArea, Duration.millis(350));
    }

    public void loadResults(Project project) {
        boundProject = project;
        if (project == null) {
            clear();
            return;
        }

        List<StudentResult> results;
        if (project.getProjectId() > 0) {
            results = resultDAO.findByProject(project.getProjectId());
            project.getResults().clear();
            project.getResults().addAll(results);
        } else {
            results = project.getResults();
        }
        setResults(results, false);
    }

    public void setResults(List<StudentResult> results) {
        setResults(results, true);
    }

    private void setResults(List<StudentResult> results, boolean animate) {
        tableData.setAll(results);
        updateStats(results, animate);
        if (animate && !results.isEmpty()) {
            animateRowsIn();
        }
    }

    public void addResult(StudentResult result) {
        if (result == null) {
            return;
        }
        tableData.add(result);
        if (boundProject != null) {
            boundProject.addResult(result);
        }
        updateStats(tableData, true);
    }

    public void clear() {
        boundProject = null;
        tableData.clear();
        errorLogArea.clear();
        updateStats(List.of(), false);
    }

    @FXML
    public void exportReport() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fc.showSaveDialog(resultsTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // BOM — Excel'in UTF-8'i otomatik tanıması için
            pw.print('﻿');
            // sep= satırı — Türkçe/Avrupa Excel'ine ayracın ; olduğunu bildirir
            pw.println("sep=;");
            pw.println("Student ID;ZIP File;Compile;Result;Time (ms);Evaluated At;Error Log");
            for (StudentResult r : tableData) {
                String time = r.getExecutionTimeMs() == null ? "" : String.valueOf(r.getExecutionTimeMs());
                pw.printf("%s;%s;%s;%s;%s;%s;%s%n",
                        csvField(r.getStudentId()),
                        csvField(r.getZipFilename()),
                        csvField(r.getCompileStatus()),
                        csvField(r.getRunStatus()),
                        time,
                        csvField(r.getEvaluatedAt()),
                        csvField(r.getCompileErrorLog()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String csvField(String value) {
        if (value == null || value.isEmpty()) return "";
        // Noktalı virgül, çift tırnak veya newline içeren alanlar tırnak içine alınır
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @FXML
    public void refreshResults() {
        if (boundProject != null) {
            loadResults(boundProject);
            UiAnimations.bounceLabel(statTotalLabel);
        }
    }

    private void animateRowsIn() {
        resultsTable.setOpacity(0.35);
        UiAnimations.fadeIn(resultsTable, Duration.millis(80), Duration.millis(420));
    }

    private void animateErrorLog(String text) {
        errorLogArea.setOpacity(0);
        errorLogArea.setText(text);
        UiAnimations.fadeIn(errorLogArea, Duration.ZERO, Duration.millis(220));
    }

    private void updateStats(List<StudentResult> results, boolean animate) {
        Report report = new Report();
        report.calculateStats(results);

        String total = "Total: " + report.getTotalStudents();
        String pass = "Pass: " + report.getPassedCount();
        String fail = "Fail: " + report.getFailedCount();
        String errors = "Errors: " + report.getErrorCount();

        if (animate && !results.isEmpty()) {
            UiAnimations.crossfadeLabelText(statTotalLabel, total);
            UiAnimations.crossfadeLabelText(statPassLabel, pass);
            UiAnimations.crossfadeLabelText(statFailLabel, fail);
            UiAnimations.crossfadeLabelText(statErrorLabel, errors);
            UiAnimations.bounceLabel(statPassLabel);
        } else {
            statTotalLabel.setText(total);
            statPassLabel.setText(pass);
            statFailLabel.setText(fail);
            statErrorLabel.setText(errors);
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
