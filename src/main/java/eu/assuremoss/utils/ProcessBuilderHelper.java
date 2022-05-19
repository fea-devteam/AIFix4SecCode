package eu.assuremoss.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static eu.assuremoss.VulnRepairDriver.MLOG;

public class ProcessBuilderHelper {
    private static final Logger LOG = LogManager.getLogger(ProcessBuilderHelper.class);

    public static void runProcess(ProcessBuilder processBuilder) {
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                MLOG.fInfo(line);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public static String runProcessSaveMessage(ProcessBuilder processBuilder) {
        StringBuilder message = new StringBuilder();

        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                MLOG.fInfo(line);
                message.append(line);
            }
        } catch (IOException e) {
            LOG.error(e);
        }

        return message.toString();
    }
}
