package com.melodyHub.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melodyHub.entity.Artist;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ArtistProfileResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void exposesDashboardProfileFieldsWithoutOwnershipOrDeletionInternals() {
        Artist artist = new Artist(
                12,
                44,
                "Son Tung M-TP",
                "son-tung-mtp",
                "Vietnamese artist",
                "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/profile.jpg",
                LocalDateTime.of(2026, 7, 20, 10, 30),
                LocalDateTime.of(2026, 7, 21, 9, 15),
                LocalDateTime.of(2026, 7, 21, 12, 0)
        );

        JsonNode json = objectMapper.valueToTree(ArtistProfileResponse.fromEntity(artist));

        assertEquals(7, json.size());
        assertEquals(12, json.get("id").asInt());
        assertEquals("Son Tung M-TP", json.get("name").asText());
        assertEquals("son-tung-mtp", json.get("slug").asText());
        assertEquals("Vietnamese artist", json.get("bio").asText());
        assertEquals(
                "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/profile.jpg",
                json.get("imageUrl").asText()
        );
        assertEquals("2026-07-20T10:30:00", json.get("createdAt").asText());
        assertEquals("2026-07-21T09:15:00", json.get("updatedAt").asText());
        assertFalse(json.has("userId"));
        assertFalse(json.has("deletedAt"));
    }
}
