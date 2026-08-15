package com.melodyHub.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed analytics for the admin dashboard charts. Complements the plain
 * counts in {@link AdminStatsResponse} with time-series and distribution data.
 *
 * <p>Dates are ISO {@code yyyy-MM-dd} strings so the frontend can use them
 * directly as chart categories, consistent with the project's ISO-date rule.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private List<DailyCount> userGrowth;
    private List<DailyCount> listensByDay;
    private List<LabeledCount> usersByRole;
    private List<LabeledCount> songsByStatus;
    private List<LabeledCount> artistRequestFunnel;
    private List<TopSong> topSongs;

    /** A single day in a time series ({@code date} = {@code yyyy-MM-dd}). */
    public record DailyCount(String date, long count) {
    }

    /** A named bucket in a distribution (role, status, ...). */
    public record LabeledCount(String label, long count) {
    }

    /** A song ranked by play count. */
    public record TopSong(String title, String slug, long playCount) {
    }
}
