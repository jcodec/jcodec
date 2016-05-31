package org.jcodec.common.logging;

public class Message {
    private LogLevel level;
    private String fileName;
    private String className;
    private int lineNumber;
    private String message;
    private String methodName;

    public Message(LogLevel level, String fileName, String className, String methodName, int lineNumber, String message) {
        this.level = level;
        this.fileName = fileName;
        this.className = className;
        this.methodName = methodName;
        this.message = methodName;
        this.lineNumber = lineNumber;
        this.message = message;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getFileName() {
        return fileName;
    }

    public String getClassName() {
        return className;
    }
    
    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        return message;
    }
}