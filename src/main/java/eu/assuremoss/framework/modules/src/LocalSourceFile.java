package eu.assuremoss.framework.modules.src;

import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.SourceCodeCollector;
import eu.assuremoss.utils.PathHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static eu.assuremoss.utils.Configuration.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LocalSourceFile implements SourceCodeCollector {
    private final String location;
    private File srcLocation;

    public LocalSourceFile(String location) {
        this.location = location;
    }

    @Override
    public void collectSourceCode() {
        String projectAbsolutePath = VulnRepairDriver.properties.getProperty(PROJECT_PATH_KEY);
        String projectSourceDir = VulnRepairDriver.properties.getProperty(PROJECT_SOURCE_PATH_KEY);

        String filePath = PathHandler.joinPath(projectAbsolutePath, projectSourceDir, location);

        String newPath = copyToNewProject(filePath, projectSourceDir);

        if (newPath != null) {
            srcLocation = new File(newPath);
        } else {
            throw new RuntimeException("Provided location doesn't exist: " + filePath);
        }
    }

    @Override
    public File getSourceCodeLocation() {
        return srcLocation;
    }

    private String copyToNewProject(String filePath, String projectSourceDir) {
        String projectName = VulnRepairDriver.properties.getProperty(PROJECT_NAME_KEY);

        Path file = Paths.get(filePath);

        // Get the subfolder in which the file is in
        Path locationParent = Paths.get(location).getParent();
        String locationFolder = locationParent == null ? "" : locationParent.toString();

        Path tempFolder = Paths.get("temp", projectName, projectSourceDir, locationFolder);

        try {
            Files.createDirectories(tempFolder);
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }

        System.out.println("Moving " + projectName + " to a temp directory: " + tempFolder.toAbsolutePath());

        Path tempFile = Paths.get(tempFolder.toString(), file.getFileName().toString());

        try {
            Files.copy(file, tempFile, REPLACE_EXISTING);
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }

        return tempFile.toAbsolutePath().toString();
    }
}
