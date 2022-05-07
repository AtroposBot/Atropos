package dev.laarryy.atropos.exceptions;

public class TryAgainException extends Exception {
    public TryAgainException(String errorMessage) {
        super(errorMessage);
    }
}
