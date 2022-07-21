package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.api.SourceCodeCollector;
import eu.assuremoss.framework.modules.src.LocalSourceFile;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.CLIArgumentHandler;
import eu.assuremoss.utils.CLIFlag;

import java.util.Properties;

import static eu.assuremoss.utils.Configuration.PROJECT_PATH_KEY;

public class SourceCodeCollectorFactory {

    public static SourceCodeCollector getInstance(CLIArgumentHandler CLIArgHandler, Properties props) {
        if (CLIArgHandler.isFlagWithValueExists(CLIFlag.SINGLE_FILE)){
            return new LocalSourceFile(CLIArgHandler.getFlagValue(CLIFlag.SINGLE_FILE));
        }

        return new LocalSourceFolder(props.getProperty(PROJECT_PATH_KEY));
    }

}
