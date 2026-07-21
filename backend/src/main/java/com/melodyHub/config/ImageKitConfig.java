package com.melodyHub.config;

import java.util.Map;
import java.util.Objects;

public final class ImageKitConfig {
    private static final String PUBLIC_KEY_ENV = "IMAGEKIT_PUBLIC_KEY";
    private static final String PRIVATE_KEY_ENV = "IMAGEKIT_PRIVATE_KEY";
    private static final String URL_ENDPOINT_ENV = "IMAGEKIT_URL_ENDPOINT";

    private final String publicKey;
    private final String privateKey;
    private final String urlEndpoint;

    private ImageKitConfig(String publicKey, String privateKey, String urlEndpoint) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.urlEndpoint = urlEndpoint;
    }

    public static ImageKitConfig fromEnvironment() {
        return new ImageKitConfig(
                AppConfig.getRequiredEnvironment(PUBLIC_KEY_ENV),
                AppConfig.getRequiredEnvironment(PRIVATE_KEY_ENV),
                AppConfig.getRequiredEnvironment(URL_ENDPOINT_ENV)
        );
    }

    static ImageKitConfig from(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        return new ImageKitConfig(
                getRequired(environment, PUBLIC_KEY_ENV),
                getRequired(environment, PRIVATE_KEY_ENV),
                getRequired(environment, URL_ENDPOINT_ENV)
        );
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getUrlEndpoint() {
        return urlEndpoint;
    }

    private static String getRequired(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }

        return value.trim();
    }
}
