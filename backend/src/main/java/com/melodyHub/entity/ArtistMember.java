package com.melodyHub.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user's membership in an Artist profile.
 *
 * <p>This is the core of the new Artist architecture:
 * instead of {@code artists.user_id} (1:1), membership is N:N through this table.
 * A user can be OWNER or MANAGER of multiple artists.
 * Multiple users can manage a single artist.</p>
 *
 * <p>Membership is binary — if a row exists, the user has access.
 * To revoke access, the row is deleted. There is no status field.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistMember {
    private Integer id;
    private Integer artistId;
    private Integer userId;
    private ArtistMemberRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
