package com.melodyHub.exception;

public class PremiumAlreadyActiveException extends RuntimeException {
    public PremiumAlreadyActiveException() {
        super("Premium is already active for this account");
    }
}
