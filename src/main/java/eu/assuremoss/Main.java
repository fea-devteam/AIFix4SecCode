package eu.assuremoss;

import eu.assuremoss.utils.Configuration;

import java.io.IOException;

import static eu.assuremoss.utils.Utils.getConfigFile;

public class Main {

    public static void main(String[] args) throws IOException {
        Configuration config = new Configuration(getConfigFile(args));
        VulnRepairDriver driver = new VulnRepairDriver(config.properties);

        driver.bootstrap();
    }
}
