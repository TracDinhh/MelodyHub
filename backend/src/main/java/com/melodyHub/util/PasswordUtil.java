package com.melodyHub.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private static final int BCRYPT_LOG_ROUNDS = 12;

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    public static boolean verify(String plainPassword, String passwordHash) {
        if (plainPassword == null || plainPassword.isBlank()
                || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
