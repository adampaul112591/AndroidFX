package com.almasb.androidfx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class AndroidFXApp extends Application {

    private static void printLines(String name, InputStream ins) throws Exception {
        Thread t = new Thread(() -> {
            String line = null;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(ins))) {
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    /**
     * Will block thread execution until process completes
     *
     * @param command
     * @throws Exception
     */
    private void runProcess(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        Process pro = builder.directory(outputDir.toFile()).redirectErrorStream(true).start();

        pro.getOutputStream().close();
        printLines(command + " stdout:", pro.getInputStream());
        printLines(command + " stderr:", pro.getErrorStream());

        pro.waitFor();

        pro.getInputStream().close();
        pro.getErrorStream().close();
    }

    @FXML
    private Label labelOutputDir;
    @FXML
    private Label labelPackage;
    @FXML
    private Label labelClassName;

    private Path outputDir;
    private Path classFile;

    public void browseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            outputDir = dir.toPath();
            labelOutputDir.setText(dir.getAbsolutePath());
//            try {
//                runProcess(dir.getAbsolutePath() + "/test.bat");
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            System.out.println("done!");
        }
    }

    public void browseAppFile() {
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
                e.printStackTrace();
            }
        }
    }

    public void updateGradleBuild() throws Exception {
        List<String> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(outputDir.resolve("build.gradle"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("$CLASSNAME")) {
                    line = line.replace("$CLASSNAME", labelPackage.getText() + "." + labelClassName.getText().replace(".java", ""));
                }

                if (line.contains("$PACKAGE")) {
                    line = line.replace("$PACKAGE", labelPackage.getText());
                }

                result.add(line);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputDir.resolve("build.gradle"))) {
            for (String line : result) {
                writer.write(line + "\n");
            }
        }
    }

    public void finish() {
        try {
            // copy gradle build files
            copyToOutputDir("gradlew");
            copyToOutputDir("gradlew.bat");
            copyToOutputDir("build.gradle");
            copyToOutputDir("gradle/wrapper/gradle-wrapper.jar");
            copyToOutputDir("gradle/wrapper/gradle-wrapper.properties");

            // create source folders
            Path srcDir = outputDir.resolve("src/main/java/" + labelPackage.getText().replace(".", "/"));
            Files.createDirectories(srcDir);

            // copy fx file
            Files.copy(classFile, Files.newOutputStream(srcDir.resolve(classFile.getFileName())));

            // replace gradle build macros
            updateGradleBuild();

            runProcess(outputDir.toAbsolutePath().toString() + "/gradlew.bat run");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exit() {
        System.exit(0);
    }

    private void copyToOutputDir(String name) {
        try (InputStream is = getClass().getResourceAsStream("/res/" + name)) {
            Path destinationFile = outputDir.resolve(name);
            Files.createDirectories(destinationFile.getParent());
            Files.copy(is, outputDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ui.fxml"));
        //loader.setController(this);
        Parent root = loader.load();

        primaryStage.setTitle("Android FX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
