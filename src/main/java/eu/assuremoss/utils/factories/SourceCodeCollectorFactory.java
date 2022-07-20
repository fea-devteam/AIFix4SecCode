package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.api.SourceCodeCollector;
import eu.assuremoss.framework.modules.src.LocalSourceFile;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.CLIArgumentHandler;

import java.util.Properties;

import static eu.assuremoss.utils.CLIArgumentHandler.SINGLE_FILE_FLAG;
import static eu.assuremoss.utils.Configuration.PROJECT_PATH_KEY;

public class SourceCodeCollectorFactory {

    public static SourceCodeCollector getInstance(CLIArgumentHandler CLIArgHandler, Properties props) {
        if (CLIArgHandler.isFlagWithValueExists(SINGLE_FILE_FLAG)){
            return new LocalSourceFile(CLIArgHandler.getFlagValue(SINGLE_FILE_FLAG));
        }

        return new LocalSourceFolder(props.getProperty(PROJECT_PATH_KEY));
    }

}
