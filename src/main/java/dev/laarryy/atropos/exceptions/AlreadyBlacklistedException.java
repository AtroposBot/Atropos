package dev.laarryy.atropos.exceptions;

public class AlreadyBlacklistedException extends Exception {
    public AlreadyBlacklistedException(String errorMessage) {
        super(errorMessage);
    }
}
