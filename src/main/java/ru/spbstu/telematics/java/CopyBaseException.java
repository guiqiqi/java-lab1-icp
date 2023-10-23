package ru.spbstu.telematics.java;

/**
 * All Exception in this project will be replaced with this Exception.
 * Add "cp: " prefix to its error message.
 */
public class CopyBaseException extends RuntimeException{
    CopyBaseException(String message) {
        super("cp: " + message);
    }
}
