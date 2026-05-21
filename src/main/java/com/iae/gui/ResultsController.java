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
                getStyleClass().removeAll("row-pass", "row-fail", "row-error");
                if (empty || item == null) {
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

    // ============================================================
    // TODO [OWNER: Sıla Karabağ (DB + Reports)] [PHASE: 2] [REQ: 9]
    // GÖREV: CSV export uygula — Results tablosunu dosyaya aktar
    // AÇIKLAMA:
    //   Sonuçları dışa aktarma butonu/menüsü yok. Hoca sonuçları başka araçlara
    //   taşıyabilmeli. Minimum: CSV. Sonradan PDF eklenebilir.
    // ADIMLAR:
    //   1. FXML'de File menüsüne "Export Results..." menü öğesi ekle (Uğur ile koordine).
    //   2. Bu metodu @FXML ile işaretle ve handler olarak bağla.
    //   3. Metod gövdesi:
    //      FileChooser fc = new FileChooser();
    //      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    //      File f = fc.showSaveDialog(resultsTable.getScene().getWindow());
    //      if (f == null) return;
    //      try (PrintWriter pw = new PrintWriter(f)) {
    //          pw.println("studentId,compileStatus,runStatus,executionTimeMs,error");
    //          for (StudentResult r : tableData) {
    //              pw.printf("%s,%s,%s,%d,\"%s\"%n",
    //                  r.getStudentId(), r.getCompileStatus(), r.getRunStatus(),
    //                  r.getExecutionTimeMs() == null ? 0 : r.getExecutionTimeMs(),
    //                  r.getCompileErrorLog() == null ? "" :
    //                      r.getCompileErrorLog().replace("\"", "\"\""));
    //          }
    //      }
    // KABUL KRİTERİ:
    //   Excel'de açıldığında satırlar bozulmadan görünüyor, virgül/quote escape doğru.
    // ============================================================
    public void exportReport() {
        // Yukarıdaki TODO'yu uygula
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
        String errors = "Errors: " + report.getCompileErrorCount();

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
