package dev.laarryy.atropos.exceptions;

public class TooManyEntriesException extends Exception {
    public TooManyEntriesException(String errorMessage) {
        super(errorMessage);
    }
}
