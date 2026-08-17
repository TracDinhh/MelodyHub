package com.melodyHub.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserPremiumTest {
    @Test
    void isPremiumOnlyWhilePremiumUntilIsInTheFuture() {
        User user = new User();
        assertFalse(user.isPremium());

        user.setPremiumUntil(LocalDateTime.now().plusMinutes(1));
        assertTrue(user.isPremium());

        user.setPremiumUntil(LocalDateTime.now().minusMinutes(1));
        assertFalse(user.isPremium());
    }
}
