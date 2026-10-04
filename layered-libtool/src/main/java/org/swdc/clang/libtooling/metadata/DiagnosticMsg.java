package org.swdc.clang.libtooling.metadata;

public class DiagnosticMsg {

    private String level;

    private String message;

    private int line;

    private int column;

    private String filePath;

    public int getColumn() {
        return column;
    }

    public int getLine() {
        return line;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
