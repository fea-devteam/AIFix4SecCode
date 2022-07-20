package eu.assuremoss.utils;

import java.security.InvalidParameterException;

public class CLIArgumentHandler {
    private final String[] args;

    // Allowed Arguments
    public static final String SINGLE_FILE_FLAG = "-singleFile";

    public CLIArgumentHandler(String[] args) {
        this.args = args;
    }

    public boolean isFlagWithValueExists(String flag) {
        if (getFlagIndex(flag) == -1) return false;
        if (getFlagIndex(flag) + 1 == args.length) throw new InvalidParameterException("No path found for '" + flag + "' flag");

        return true;
    }


    private int getFlagIndex(String flag) {
        for (int i = 0; i < args.length; i++) {
            if (flag.equals(args[i])) {
                return i;
            }
        }

        return -1;
    }

    public String getFlagValue(String flag) {
        if (!isFlagWithValueExists(flag)) throw new InvalidParameterException("Invalid flag: " + flag);

        return args[getFlagIndex(flag) + 1];
    }
}
