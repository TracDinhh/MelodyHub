package com.melodyHub.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ArtistProfileUpdateRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsTheEditableArtistProfileFields() throws Exception {
        String json = """
                {
                  "name": "Son Tung M-TP",
                  "slug": "son-tung-mtp",
                  "bio": "Vietnamese artist",
                  "imageUrl": "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/profile.jpg"
                }
                """;

        ArtistProfileUpdateRequest request = objectMapper.readValue(json, ArtistProfileUpdateRequest.class);

        assertEquals("Son Tung M-TP", request.getName());
        assertEquals("son-tung-mtp", request.getSlug());
        assertEquals("Vietnamese artist", request.getBio());
        assertEquals(
                "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/profile.jpg",
                request.getImageUrl()
        );
    }
}
