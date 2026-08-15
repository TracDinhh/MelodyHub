package com.melodyHub.service.admin;

import com.melodyHub.dto.response.AdminAnalyticsResponse;
import com.melodyHub.dto.response.AdminAnalyticsResponse.DailyCount;
import com.melodyHub.dto.response.AdminAnalyticsResponse.LabeledCount;
import com.melodyHub.dto.response.AdminAnalyticsResponse.TopSong;
import com.melodyHub.dto.response.AdminStatsResponse;
import com.melodyHub.entity.ArtistRequestStatus;
import com.melodyHub.entity.UserRole;
import com.melodyHub.exception.AuthException;
import com.melodyHub.repository.ArtistRepository;
import com.melodyHub.repository.ArtistRequestRepository;
import com.melodyHub.repository.ListenHistoryRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.repository.UserRepository;
import com.melodyHub.service.auth.AuthorizationService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates counts for the admin overview dashboard.
 */
public class AdminStatsService {
    /** Number of trailing days (including today) covered by the time-series charts. */
    private static final int TIME_SERIES_DAYS = 30;
    /** How many songs the "top songs" chart shows. */
    private static final int TOP_SONGS_LIMIT = 10;

    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ArtistRequestRepository artistRequestRepository;
    private final SongRepository songRepository;
    private final ListenHistoryRepository listenHistoryRepository;

    public AdminStatsService() {
        this(
                new AuthorizationService(),
                new UserRepository(),
                new ArtistRepository(),
                new ArtistRequestRepository(),
                new SongRepository(),
                new ListenHistoryRepository()
        );
    }

    public AdminStatsService(
            AuthorizationService authorizationService,
            UserRepository userRepository,
            ArtistRepository artistRepository,
            ArtistRequestRepository artistRequestRepository,
            SongRepository songRepository,
            ListenHistoryRepository listenHistoryRepository
    ) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.artistRepository = Objects.requireNonNull(artistRepository, "artistRepository must not be null");
        this.artistRequestRepository = Objects.requireNonNull(
                artistRequestRepository, "artistRequestRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
        this.listenHistoryRepository = Objects.requireNonNull(
                listenHistoryRepository, "listenHistoryRepository must not be null");
    }

    public AdminStatsResponse getStats(String token) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        long listeners = userRepository.countUsers(UserRole.USER, null);
        long artists = userRepository.countUsers(UserRole.ARTIST, null);
        long admins = userRepository.countUsers(UserRole.ADMIN, null);

        return new AdminStatsResponse(
                listeners + artists + admins,
                listeners,
                artists,
                admins,
                artistRepository.count(null),
                artistRequestRepository.countByStatus(ArtistRequestStatus.PENDING),
                songRepository.count(null, null)
        );
    }

    /**
     * Builds the detailed analytics payload for the admin dashboard charts.
     * Time-series charts cover the last {@link #TIME_SERIES_DAYS} days with
     * missing days zero-filled so the frontend sees a continuous series.
     */
    public AdminAnalyticsResponse getAnalytics(String token) throws AuthException, SQLException {
        authorizationService.requireRole(token, UserRole.ADMIN);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(TIME_SERIES_DAYS - 1L);
        LocalDateTime since = startDate.atStartOfDay();

        List<DailyCount> userGrowth = toDailySeries(
                userRepository.countCreatedByDaySince(since), startDate, today);
        List<DailyCount> listensByDay = toDailySeries(
                listenHistoryRepository.countByDaySince(since), startDate, today);

        List<LabeledCount> usersByRole = toLabeledCounts(userRepository.countByRoleGrouped());
        List<LabeledCount> songsByStatus = toLabeledCounts(songRepository.countByStatusGrouped());
        List<LabeledCount> artistRequestFunnel = toLabeledCounts(
                artistRequestRepository.countByStatusGrouped());

        List<TopSong> topSongs = songRepository.findTopByPlayCount(TOP_SONGS_LIMIT).stream()
                .map(row -> new TopSong(row.title(), row.slug(), row.playCount()))
                .toList();

        return new AdminAnalyticsResponse(
                userGrowth,
                listensByDay,
                usersByRole,
                songsByStatus,
                artistRequestFunnel,
                topSongs
        );
    }

    /**
     * Expands a sparse day→count map (keyed by {@code yyyy-MM-dd}) into a dense
     * list spanning {@code start}..{@code end} inclusive, filling missing days
     * with zero so charts show a continuous line.
     */
    private List<DailyCount> toDailySeries(Map<String, Long> byDay, LocalDate start, LocalDate end) {
        List<DailyCount> series = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            String key = day.toString();
            series.add(new DailyCount(key, byDay.getOrDefault(key, 0L)));
        }
        return series;
    }

    /** Maps a label→count grouping into an ordered list of {@link LabeledCount}. */
    private List<LabeledCount> toLabeledCounts(Map<String, Long> grouped) {
        List<LabeledCount> counts = new ArrayList<>();
        grouped.forEach((label, count) -> counts.add(new LabeledCount(label, count)));
        return counts;
    }
}
