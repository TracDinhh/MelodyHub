package com.melodyHub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate counts for the admin overview dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long listeners;
    private long admins;
    private long artistProfiles;
    private long pendingArtistRequests;
    private long publishedSongs;
}
