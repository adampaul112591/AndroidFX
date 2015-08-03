/*
 *   AndroidFX
 *   Copyright (C) {2015}  {Almas Baimagambetov}
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */
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

/**
 * Main UI controller
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 *
 */
public class UIController {

    private Stage stage;

    @FXML
    private Label labelOutputDir;
    @FXML
    private Label labelPackage;
    @FXML
    private Label labelClassName;
    @FXML
    private Label labelAndroidSDK;

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
                .or(labelClassName.textProperty().isEmpty())
                .or(labelAndroidSDK.textProperty().isEmpty()));
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
    private void browseAndroidSDK() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Android SDK directory");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            labelAndroidSDK.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void finish() {
        btnFinish.disableProperty().unbind();
        btnFinish.setDisable(true);
        btnExit.setDisable(true);

        ProjectGeneratorTask task = new ProjectGeneratorTask(outputDir, classFile, labelPackage.getText(), labelAndroidSDK.getText(), log);

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
