package com.melodyHub.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImageKitConfigTest {
    @Test
    void readsImageKitCredentialsFromEnvironmentValues() {
        ImageKitConfig config = ImageKitConfig.from(Map.of(
                "IMAGEKIT_PUBLIC_KEY", "public-key",
                "IMAGEKIT_PRIVATE_KEY", "private-key",
                "IMAGEKIT_URL_ENDPOINT", "https://ik.imagekit.io/melodyhub"
        ));

        assertEquals("public-key", config.getPublicKey());
        assertEquals("private-key", config.getPrivateKey());
        assertEquals("https://ik.imagekit.io/melodyhub", config.getUrlEndpoint());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "IMAGEKIT_PUBLIC_KEY",
            "IMAGEKIT_PRIVATE_KEY",
            "IMAGEKIT_URL_ENDPOINT"
    })
    void rejectsEachMissingImageKitCredentialWithoutExposingOtherSecrets(String missingKey) {
        Map<String, String> environment = new HashMap<>(Map.of(
                "IMAGEKIT_PUBLIC_KEY", "public-secret-value",
                "IMAGEKIT_PRIVATE_KEY", "private-secret-value",
                "IMAGEKIT_URL_ENDPOINT", "https://secret-endpoint.example"
        ));
        environment.remove(missingKey);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ImageKitConfig.from(environment)
        );

        assertTrue(exception.getMessage().contains(missingKey));
        assertTrue(!exception.getMessage().contains("public-secret-value"));
        assertTrue(!exception.getMessage().contains("private-secret-value"));
        assertTrue(!exception.getMessage().contains("secret-endpoint.example"));
    }
}
