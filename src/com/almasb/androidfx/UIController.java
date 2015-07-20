package com.almasb.androidfx;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class UIController {

    private Stage stage;

    @FXML
    private Label labelOutputDir;
    @FXML
    private Label labelPackage;
    @FXML
    private Label labelClassName;

    @FXML
    private Button btnFinish;
    @FXML
    private Button btnExit;

    @FXML
    private TextArea log;

    private Path outputDir;
    private Path classFile;

    public UIController(Stage stage) {
        this.stage = stage;
    }

    public void initialize() {
        btnFinish.disableProperty().bind(
                labelOutputDir.textProperty().isEmpty()
                .or(labelPackage.textProperty().isEmpty())
                .or(labelClassName.textProperty().isEmpty()));
    }

    @FXML
    private void browseOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            outputDir = dir.toPath();
            labelOutputDir.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void browseAppFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new ExtensionFilter("JavaFX App File", "*.java"));
        chooser.setTitle("Select JavaFX Application File");
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            classFile = file.toPath();
            labelClassName.setText(file.getName());

            try (BufferedReader reader = Files.newBufferedReader(classFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("package")) {
                        String pkg = line.replace("package", "").replace(";", "").trim();
                        labelPackage.setText(pkg);
                        break;
                    }
                }
            }
            catch (Exception e) {
                handleException(e);
            }
        }
    }

    @FXML
    private void finish() {
        btnFinish.disableProperty().unbind();
        btnFinish.setDisable(true);
        btnExit.setDisable(true);

        ProjectGeneratorTask task = new ProjectGeneratorTask(outputDir, classFile, labelPackage.getText(), log);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void exit() {
        System.exit(0);
    }

    private void handleException(Exception e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
