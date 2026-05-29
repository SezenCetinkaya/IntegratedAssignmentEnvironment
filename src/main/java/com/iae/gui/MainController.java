package com.iae.gui;

import com.iae.core.Configuration;
import com.iae.core.Project;
import com.iae.core.StudentResult;
import com.iae.db.ConfigurationDAO;
import com.iae.db.ProjectDAO;
import com.iae.db.StudentResultDAO;
import com.iae.files.ResourceExtractor;
import com.iae.files.SubmissionProcessor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.awt.Desktop;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private TextField projectNameField;
    @FXML private TextField configNameField;
    @FXML private TextField submissionDirField;
    @FXML private TextField expectedOutputField;
    @FXML private TextField runArgsField;
    @FXML private Button browseSubmissionsBtn;
    @FXML private Button browseExpectedBtn;
    @FXML private Button runButton;
    @FXML private ProgressIndicator runProgress;
    @FXML private Label statusLabel;
    @FXML private Label projectStatusLabel;
    @FXML private VBox projectCard;
    @FXML private VBox resultsCard;
    @FXML private VBox contentBox;
    @FXML private HBox toolbarBox;
    @FXML private HBox statusBar;
    @FXML private ResultsController resultsPanelController;

    private Stage primaryStage;
    private Project currentProject;
    private Configuration currentConfiguration;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final ConfigurationDAO configDAO = new ConfigurationDAO();
    private final StudentResultDAO resultDAO = new StudentResultDAO();

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void initialize() {
        setupVisualEffects();
        updateProjectUI();
    }

    @FXML
    public void onExportResults() {
        if (resultsPanelController != null) {
            resultsPanelController.exportReport();
        }
    }

    private void setupVisualEffects() {
        UiAnimations.applyCardShadow(projectCard);
        UiAnimations.applyCardShadow(resultsCard);
        UiAnimations.wireButtonPress(runButton);
        toolbarBox.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .forEach(UiAnimations::wireButtonPress);

        UiAnimations.fadeIn(toolbarBox, Duration.ZERO, Duration.millis(400));
        UiAnimations.staggerFadeInUp(
                List.of(projectCard, resultsCard),
                Duration.millis(120),
                Duration.millis(100));

        if (resultsPanelController != null) {
            resultsPanelController.playEntranceAnimation();
        }
    }

    @FXML
    public void onNewProject() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a new evaluation project");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(16));

        TextField nameInput = new TextField();
        nameInput.setPromptText("e.g. CE316 Assignment 1 — String Sort");

        ComboBox<Configuration> configCombo = new ComboBox<>();
        configCombo.setConverter(configurationLabelConverter());
        configCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Configuration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatConfigurationLabel(item));
            }
        });
        configCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Configuration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatConfigurationLabel(item));
            }
        });
        configCombo.setMaxWidth(Double.MAX_VALUE);
        refreshConfigurationCombo(configCombo);

        Button manageConfigBtn = new Button("Manage…");
        manageConfigBtn.getStyleClass().add("secondary-button");
        manageConfigBtn.setOnAction(e -> {
            onManageConfigurations();
            refreshConfigurationCombo(configCombo);
        });
        HBox configRow = new HBox(8, configCombo, manageConfigBtn);
        HBox.setHgrow(configCombo, Priority.ALWAYS);
        configCombo.setPrefWidth(320);

        TextField expectedInput = new TextField();
        Button expectedBrowse = new Button("Browse…");
        expectedBrowse.setOnAction(e -> {
            File f = chooseFile("Select expected output file");
            if (f != null) {
                expectedInput.setText(f.getAbsolutePath());
            }
        });
        HBox expectedRow = new HBox(8, expectedInput, expectedBrowse);
        HBox.setHgrow(expectedInput, javafx.scene.layout.Priority.ALWAYS);

        TextField submissionInput = new TextField();
        Button submissionBrowse = new Button("Browse…");
        submissionBrowse.setOnAction(e -> {
            File d = chooseDirectory("Select folder containing student ZIP files");
            if (d != null) {
                submissionInput.setText(d.getAbsolutePath());
            }
        });
        HBox submissionRow = new HBox(8, submissionInput, submissionBrowse);
        HBox.setHgrow(submissionInput, javafx.scene.layout.Priority.ALWAYS);

        TextField argsInput = new TextField();
        argsInput.setPromptText("Optional command-line arguments");

        grid.add(new Label("Project name:"), 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(new Label("Language configuration:"), 0, 1);
        grid.add(configRow, 1, 1);
        grid.add(new Label("Expected output:"), 0, 2);
        grid.add(expectedRow, 1, 2);
        grid.add(new Label("Submissions folder:"), 0, 3);
        grid.add(submissionRow, 1, 3);
        grid.add(new Label("Run arguments:"), 0, 4);
        grid.add(argsInput, 1, 4);

        if (configCombo.getItems().isEmpty()) {
            Label hint = new Label(
                    "No language configurations found. Click Manage… to create one, or restart the app to load defaults.");
            hint.setWrapText(true);
            hint.setStyle("-fx-text-fill: #c0392b;");
            grid.add(hint, 1, 5);
        } else {
            Label hint = new Label("Choose how student code is compiled and run (C, Java, Python, etc.).");
            hint.setWrapText(true);
            hint.getStyleClass().add("field-label");
            grid.add(hint, 1, 5);
        }

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        if (nameInput.getText().isBlank()) {
            showWarning("Validation", "Project name is required.");
            return;
        }
        if (configCombo.getValue() == null) {
            showWarning("Validation", "Select a language configuration.");
            return;
        }
        if (expectedInput.getText().isBlank()) {
            showWarning("Validation", "Expected output file is required.");
            return;
        }
        if (submissionInput.getText().isBlank()) {
            showWarning("Validation", "Submissions folder is required.");
            return;
        }

        Project project = new Project();
        project.setName(nameInput.getText().trim());
        project.setConfigId(configCombo.getValue().getConfigId());
        project.setExpectedOutputPath(expectedInput.getText().trim());
        project.setSubmissionDir(submissionInput.getText().trim());
        project.setRunArguments(argsInput.getText().trim());
        project.setCreatedAt(LocalDateTime.now());

        int id = projectDAO.insert(project);
        if (id <= 0) {
            showError("Error", "Could not save project to database.");
            return;
        }

        setCurrentProject(project);
        setStatus("Project created: " + project.getName(), "status-label-success");
        UiAnimations.highlightField(projectNameField);
        projectCard.getStyleClass().add("section-card-accent");
    }

    // ============================================================
    // TODO [OWNER: Uğur Emin Baynal (GUI) + Sıla Karabağ (DB)] [PHASE: 1] [REQ: 3, 10]
    // GÖREV: Open Project ekranı bayat veri gösteriyor; config değişmeden kalıyor
    // AÇIKLAMA:
    //   Proje listesi cache'lenebiliyor veya UI yüklenirken önceki proje state'i kalıyor.
    //   Hoca bu bug'ı raporladı (WhatsApp notu).
    // ADIMLAR:
    //   1. onOpenProject() içinde her seferinde DB'den taze çek: projectDAO.findAll()
    //      (cachedProjects gibi bir field varsa kaldır).
    //   2. Seçilen Project yüklenince UI alanlarını güncel değerlerle doldur:
    //      currentProject = projectDAO.findById(selectedId);
    //      Configuration config = configDAO.findById(currentProject.getConfigId());
    //      configNameField.setText(config != null ? config.getName() : "");
    //      submissionDirField.setText(currentProject.getSubmissionDir());
    //      runArgsField.setText(currentProject.getRunArguments());
    //      expectedOutputField.setText(currentProject.getExpectedOutputPath());
    //   3. Config değişikliği varsa: currentProject.setConfigId(newConfig.getId());
    //      projectDAO.update(currentProject);
    // KABUL KRİTERİ: Proje aç → config combobox doğru config'i gösteriyor. Config değişikliği kalıcı.
    // ============================================================
    @FXML
    public void onOpenProject() {
        List<Project> projects = projectDAO.findAll();
        if (projects.isEmpty()) {
            showWarning("No projects", "Create a project first with File → New Project.");
            return;
        }

        ChoiceDialog<Project> dialog = new ChoiceDialog<>(projects.get(0), projects);
        dialog.setTitle("Open Project");
        dialog.setHeaderText("Select a project to open");
        dialog.setContentText("Project:");

        Optional<Project> chosen = dialog.showAndWait();
        chosen.ifPresent(p -> {
            Project loaded = projectDAO.findById(p.getProjectId());
            if (loaded != null) {
                setCurrentProject(loaded);
                setStatus("Opened project: " + loaded.getName(), "status-label-active");
                UiAnimations.fadeInUp(projectCard, Duration.ZERO);
            }
        });
    }

    @FXML
    // ============================================================
    // TODO [OWNER: Sezen Çetinkaya (Files) + Uğur Emin Baynal (GUI)] [PHASE: 2] [REQ: 10]
    // GÖREV: onSaveProject() ve onOpenProject()'i ProjectFileService ile bağla
    // AÇIKLAMA:
    //   Şu an "Save" sadece DB'ye yazıyor, taşınabilir .iaeproject dosyası üretmiyor.
    //   ProjectFileService.java stub'ı oluşturuldu (com.iae.service paketi).
    // ADIMLAR:
    //   1. ProjectFileService implement edildikten sonra buraya FileChooser ekle.
    //   2. onSaveProject(): FileChooser ile hedef .iaeproject yolu seç → service.saveProject() çağır.
    //   3. onOpenProject(): FileChooser ile .iaeproject seç → service.openProject() çağır,
    //      dönen Project'i currentProject'e ata, UI'ı güncelle.
    // KABUL KRİTERİ:
    //   "Export Project" butonu .iaeproject dosyası üretiyor.
    //   "Import Project" butonu .iaeproject dosyasından projeyi geri yüklüyor.
    // ============================================================
    public void onSaveProject() {
        if (currentProject == null) {
            showWarning("No project", "Open or create a project first.");
            return;
        }

        currentProject.setSubmissionDir(submissionDirField.getText().trim());
        currentProject.setExpectedOutputPath(expectedOutputField.getText().trim());
        currentProject.setRunArguments(runArgsField.getText().trim());
        projectDAO.update(currentProject);
        setStatus("Project saved.", "status-label-success");
        UiAnimations.crossfadeLabelText(projectStatusLabel, "Saved — " + currentProject.getName());
    }

    @FXML
    public void onBrowseSubmissions() {
        File dir = chooseDirectory("Select submissions folder");
        if (dir != null && currentProject != null) {
            submissionDirField.setText(dir.getAbsolutePath());
            currentProject.setSubmissionDir(dir.getAbsolutePath());
        }
    }

    @FXML
    public void onBrowseExpectedOutput() {
        File file = chooseFile("Select expected output file");
        if (file != null && currentProject != null) {
            expectedOutputField.setText(file.getAbsolutePath());
            currentProject.setExpectedOutputPath(file.getAbsolutePath());
        }
    }

    @FXML
    public void onRunEvaluation() {
        if (currentProject == null) {
            showWarning("No project", "Open or create a project first.");
            return;
        }

        if (currentConfiguration == null) {
            currentConfiguration = configDAO.findById(currentProject.getConfigId());
        }
        if (currentConfiguration == null) {
            showError("Configuration missing", "The project's configuration could not be loaded.");
            return;
        }

        File submissionsDir = new File(submissionDirField.getText().trim());
        if (!submissionsDir.isDirectory()) {
            showWarning("Invalid folder", "Submissions folder does not exist.");
            return;
        }

        File expectedFile = new File(expectedOutputField.getText().trim());
        if (!expectedFile.isFile()) {
            showWarning("Invalid file", "Expected output file does not exist.");
            return;
        }

        String expectedContent;
        try {
            expectedContent = Files.readString(expectedFile.toPath());
        } catch (IOException e) {
            showError("Read error", "Could not read expected output: " + e.getMessage());
            return;
        }

        currentProject.setSubmissionDir(submissionsDir.getAbsolutePath());
        currentProject.setExpectedOutputPath(expectedFile.getAbsolutePath());
        currentProject.setRunArguments(runArgsField.getText().trim());
        projectDAO.update(currentProject);

        runButton.setDisable(true);
        browseSubmissionsBtn.setDisable(true);
        browseExpectedBtn.setDisable(true);
        runArgsField.setDisable(true);
        runProgress.setVisible(true);
        runProgress.setOpacity(0);
        UiAnimations.fadeIn(runProgress, Duration.ZERO, Duration.millis(200));
        UiAnimations.spinProgress(runProgress);
        runButton.getStyleClass().add("primary-button-running");
        UiAnimations.startPulse(runButton);
        setStatus("Running evaluation…", "status-label-active");

        String expected = expectedContent;
        Configuration config = currentConfiguration;
        Project project = currentProject;

        Task<List<StudentResult>> task = new Task<>() {
            @Override
            protected List<StudentResult> call() {
                SubmissionProcessor processor = new SubmissionProcessor();
                // ============================================================
                // TODO [OWNER: Uğur Emin Baynal (GUI)] [PHASE: 1] [REQ: 6, 8]
                // GÖREV: runArgsField.getText() değerini processAll'a ilet
                // AÇIKLAMA:
                //   project.setRunArguments(...) yapılıyor ama bu değer processAll'a
                //   parametre olarak geçilmiyor. Öğrenci programı argümanları almıyor.
                // ADIMLAR:
                //   1. processAll imzası runArguments parametresi alacak şekilde güncellendikten sonra:
                //      processor.processAll(submissionsDir, config, expected, false,
                //                           project.getRunArguments())
                //   2. Alternatif: SubmissionProcessor'a project'i geç ve
                //      processSingle içinde project.getRunArguments() oku.
                // KABUL KRİTERİ:
                //   GUI'ye "hello world" yazıp Run'a basınca processAll'a "hello world" iletiliyor.
                // ============================================================
                return processor.processAll(submissionsDir, config, expected, false);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<StudentResult> results = task.getValue();
            persistResults(project, results);
            resultsPanelController.setResults(results);
            project.setLastRunAt(LocalDateTime.now().toString());
            projectDAO.update(project);
            finishRun("Evaluation complete — " + results.size() + " submission(s) processed.",
                    "status-label-success");
            UiAnimations.flashSuccess(resultsCard);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            finishRun("Evaluation failed: " + (ex != null ? ex.getMessage() : "unknown error"),
                    "status-label-error");
            UiAnimations.shake(runButton);
            showError("Evaluation error", ex != null ? ex.getMessage() : "Unknown error");
        }));

        new Thread(task, "iae-evaluation").start();
    }

    @FXML
    public void onManageConfigurations() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/gui/configuration-view.fxml"));
            Parent root = loader.load();
            ConfigurationController controller = loader.getController();
            Stage dialog = new Stage();
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Manage Configurations");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/iae/gui/styles.css").toExternalForm());
            dialog.setScene(scene);
            controller.setStage(dialog);
            UiAnimations.slideInRight(root, Duration.millis(380));
            dialog.showAndWait();
        } catch (IOException ex) {
            showError("UI error", "Could not open configuration editor: " + ex.getMessage());
        }
    }

    @FXML
    public void onOpenHelp() {
        try {
            if (!Desktop.isDesktopSupported()) {
                showInfo("User Manual", "Desktop browsing is not supported on this platform.");
                return;
            }
            File manual = ResourceExtractor.extractResource("/com/iae/gui/help/manual.html", "iae-manual");
            Desktop.getDesktop().browse(manual.toURI());
        } catch (Exception ex) {
            showError("Help unavailable", "Could not open user manual: " + ex.getMessage());
        }
    }

    @FXML
    public void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About IAE");
        alert.setHeaderText("Integrated Assignment Environment");
        alert.setContentText("CE316 Project — Izmir University of Economics\n"
                + "Automates batch evaluation of student programming submissions.\n\n"
                + "Version 1.0.0");
        alert.showAndWait();
    }

    @FXML
    public void onExit() {
        if (primaryStage != null) {
            primaryStage.close();
        } else {
            Platform.exit();
        }
    }

    public void setCurrentProject(Project project) {
        this.currentProject = project;
        if (project != null && project.getConfigId() > 0) {
            this.currentConfiguration = configDAO.findById(project.getConfigId());
        } else {
            this.currentConfiguration = null;
        }
        updateProjectUI();
        if (resultsPanelController != null) {
            resultsPanelController.loadResults(project);
        }
    }

    private void updateProjectUI() {
        boolean hasProject = currentProject != null;

        projectNameField.setText(hasProject ? currentProject.getName() : "");
        configNameField.setText(currentConfiguration != null ? currentConfiguration.getName() : "");
        submissionDirField.setText(hasProject ? nullSafe(currentProject.getSubmissionDir()) : "");
        expectedOutputField.setText(hasProject ? nullSafe(currentProject.getExpectedOutputPath()) : "");
        runArgsField.setText(hasProject ? nullSafe(currentProject.getRunArguments()) : "");

        browseSubmissionsBtn.setDisable(!hasProject);
        browseExpectedBtn.setDisable(!hasProject);
        runArgsField.setDisable(!hasProject);
        runButton.setDisable(!hasProject);

        projectStatusLabel.setText(hasProject
                ? "Project: " + currentProject.getName()
                : "No project loaded");
    }

    private void persistResults(Project project, List<StudentResult> results) {
        for (StudentResult result : results) {
            resultDAO.insert(project.getProjectId(), result);
        }
        project.getResults().clear();
        project.getResults().addAll(results);
    }

    private void finishRun(String message, String statusStyle) {
        runButton.setDisable(currentProject == null);
        browseSubmissionsBtn.setDisable(currentProject == null);
        browseExpectedBtn.setDisable(currentProject == null);
        runArgsField.setDisable(currentProject == null);
        runProgress.setVisible(false);
        UiAnimations.stopSpin(runProgress);
        UiAnimations.stopPulse(runButton);
        runButton.getStyleClass().remove("primary-button-running");
        setStatus(message, statusStyle);
    }

    private void setStatus(String text) {
        setStatus(text, null);
    }

    private void setStatus(String text, String styleClass) {
        statusLabel.getStyleClass().removeAll(
                "status-label-active", "status-label-success", "status-label-error");
        if (styleClass != null && !styleClass.isBlank()) {
            statusLabel.getStyleClass().add(styleClass);
        }
        UiAnimations.crossfadeLabelText(statusLabel, text);
    }

    private File chooseDirectory(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        return chooser.showDialog(primaryStage);
    }

    private File chooseFile(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files", "*.txt", "*.out", "*.expected"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        return chooser.showOpenDialog(primaryStage);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void refreshConfigurationCombo(ComboBox<Configuration> combo) {
        int selectedId = combo.getValue() != null ? combo.getValue().getConfigId() : -1;
        combo.getItems().setAll(configDAO.findAll());
        if (selectedId > 0) {
            combo.getItems().stream()
                    .filter(c -> c.getConfigId() == selectedId)
                    .findFirst()
                    .ifPresent(combo::setValue);
        }
        if (combo.getValue() == null && !combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    private static StringConverter<Configuration> configurationLabelConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Configuration config) {
                return config == null ? "" : formatConfigurationLabel(config);
            }

            @Override
            public Configuration fromString(String string) {
                return null;
            }
        };
    }

    private static String formatConfigurationLabel(Configuration config) {
        String lang = config.getLanguage() != null ? config.getLanguage() : "?";
        String compiler = config.getCompilerPath() != null ? config.getCompilerPath() : "—";
        return config.getName() + "  ·  " + lang + "  ·  " + compiler;
    }
}
