package eu.assuremoss.utils;

// Allowed CLI Arguments
public enum CLIFlag {
    SINGLE_FILE("-singleFile"),
    MAP_FILE("-map"),
    CONFIG_FILE("-config");


    private final String text;

    CLIFlag(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}