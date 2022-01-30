package dev.laarryy.atropos.exceptions;

public class CannotSendModMailException extends Exception {
    public CannotSendModMailException(String errorMessage) {
        super(errorMessage);
    }
}
