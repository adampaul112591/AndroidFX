package com.almasb.androidfx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

public final class ProjectGeneratorTask extends Task<Void> {

    private Path outputDir;
    private Path appFile;
    private String packageName;

    private TextArea log;

    public ProjectGeneratorTask(Path outputDir, Path appFile, String packageName, TextArea log) {
        this.outputDir = outputDir;
        this.appFile = appFile;
        this.packageName = packageName;
        this.log = log;
    }

    private void redirectStream(InputStream ins) throws Exception {
        Thread t = new Thread(() -> {
            String line = null;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(ins))) {
                while ((line = in.readLine()) != null) {
                    logMessage(line);
                }
            }
            catch (Exception e) {
                logMessage("Failed to redirect stream: " + e.getMessage());
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
        logMessage("Starting gradle build process from " + outputDir.toAbsolutePath());

        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        Process pro = builder.directory(outputDir.toFile()).redirectErrorStream(true).start();

        pro.getOutputStream().close();
        redirectStream(pro.getInputStream());
        redirectStream(pro.getErrorStream());

        pro.waitFor();

        pro.getInputStream().close();
        pro.getErrorStream().close();
    }

    private void copyToOutputDir(String name) throws Exception {
        logMessage("Copying " + name + " to " + outputDir.toAbsolutePath());

        try (InputStream is = getClass().getResourceAsStream("/res/" + name)) {
            Path destinationFile = outputDir.resolve(name);
            Files.createDirectories(destinationFile.getParent());
            Files.copy(is, outputDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void updateGradleBuild() throws Exception {
        logMessage("Configuring gradle.build");

        List<String> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(outputDir.resolve("build.gradle"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("$CLASSNAME")) {
                    line = line.replace("$CLASSNAME", packageName + "." + appFile.getFileName().toString().replace(".java", ""));
                }

                if (line.contains("$PACKAGE")) {
                    line = line.replace("$PACKAGE", packageName);
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

    private void logMessage(String message) {
        Platform.runLater(() -> log.appendText(message + "\n"));
    }

    @Override
    protected Void call() throws Exception {
        logMessage("Starting project generation task");

        // copy gradle build files
        copyToOutputDir("gradlew");
        copyToOutputDir("gradlew.bat");
        copyToOutputDir("build.gradle");
        copyToOutputDir("gradle/wrapper/gradle-wrapper.jar");
        copyToOutputDir("gradle/wrapper/gradle-wrapper.properties");

        // create source folders
        Path srcDir = outputDir.resolve("src/main/java/" + packageName.replace(".", "/"));
        Files.createDirectories(srcDir);

        // copy javafx file
        Files.copy(appFile, Files.newOutputStream(srcDir.resolve(appFile.getFileName())));

        // configure build.gradle
        updateGradleBuild();

        // run gradle build
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            logMessage("OS: WINDOWS. Using gradlew.bat");
            runProcess(outputDir.toAbsolutePath() + "/gradlew.bat android");
        }
        else {
            logMessage("OS: NON-WINDOWS. Using gradlew");
            runProcess(outputDir.toAbsolutePath() + "/gradlew android");
        }

        logMessage("Completing project generation task");
        return null;
    }

    @Override
    protected void failed() {
        logMessage("Project generation failed: " + getException().getMessage());
    }
}
