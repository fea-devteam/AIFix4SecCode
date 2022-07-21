package eu.assuremoss.utils;

// Allowed CLI Arguments
public enum CLIFlag {
    SINGLE_FILE_FLAG("-singleFile");

    private final String text;

    CLIFlag(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}