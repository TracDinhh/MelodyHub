package com.melodyHub.lyrics;

/**
 * Thrown when a lyrics provider encounters an error (network, rate-limit, etc.).
 */
public class LyricsProviderException extends Exception {
    private final String providerCode;

    public LyricsProviderException(String providerCode, String message) {
        super(message);
        this.providerCode = providerCode;
    }

    public LyricsProviderException(String providerCode, String message, Throwable cause) {
        super(message, cause);
        this.providerCode = providerCode;
    }

    /** Machine-readable code such as PROVIDER_UNAVAILABLE, RATE_LIMITED. */
    public String getProviderCode() {
        return providerCode;
    }
}
