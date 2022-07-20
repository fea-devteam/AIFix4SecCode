package eu.assuremoss.framework.modules.src;

import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.SourceCodeCollector;
import eu.assuremoss.utils.PathHandler;

import java.io.File;

import static eu.assuremoss.utils.Configuration.PROJECT_PATH_KEY;
import static eu.assuremoss.utils.Configuration.PROJECT_SOURCE_PATH_KEY;

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

        srcLocation = new File(filePath);
    }

    @Override
    public File getSourceCodeLocation() {
        return srcLocation;
    }
}
