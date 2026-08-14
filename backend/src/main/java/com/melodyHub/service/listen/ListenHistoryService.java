package com.melodyHub.service.listen;

import com.melodyHub.dto.response.ListenHistoryResponse;
import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.entity.Artist;
import com.melodyHub.entity.ListenHistory;
import com.melodyHub.repository.ListenHistoryRepository;
import com.melodyHub.repository.ListenHistoryRepository.HistoryRow;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.util.Pagination;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ListenHistoryService {

    private final ListenHistoryRepository listenHistoryRepository;
    private final SongRepository songRepository;

    public ListenHistoryService() {
        this(new ListenHistoryRepository(), new SongRepository());
    }

    public ListenHistoryService(
            ListenHistoryRepository listenHistoryRepository,
            SongRepository songRepository) {
        this.listenHistoryRepository = Objects.requireNonNull(listenHistoryRepository,
                "listenHistoryRepository must not be null");
        this.songRepository = Objects.requireNonNull(songRepository,
                "songRepository must not be null");
    }

    /**
     * Records a listen. Returns the persisted entry's id.
     */
    public long record(int userId, int songId, int playedSec) throws SQLException {
        ListenHistory entry = new ListenHistory(null, userId, songId, Math.max(0, playedSec), null);
        return listenHistoryRepository.record(entry).getId();
    }

    public PagedResponse<ListenHistoryResponse> getPage(int userId, int page, int size) throws SQLException {
        int offset = Pagination.offset(page, size);
        List<HistoryRow> rows = listenHistoryRepository.getPageByUser(userId, size, offset);
        long total = listenHistoryRepository.countByUser(userId);

        // Batch-load artists for all songs on the page in a single query (avoids N+1).
        List<Integer> songIds = rows.stream().map(row -> row.song().getId()).toList();
        Map<Integer, List<Artist>> artistsBySong = songRepository.findArtistsForSongs(songIds);

        List<ListenHistoryResponse> items = new ArrayList<>(rows.size());
        for (HistoryRow row : rows) {
            ListenHistoryResponse item = ListenHistoryResponse.fromEntity(
                    row.historyId(),
                    row.song(),
                    row.listenedAt(),
                    row.playedSec(),
                    artistsBySong.getOrDefault(row.song().getId(), List.of())
            );
            items.add(item);
        }

        return new PagedResponse<>(items, total, page, size);
    }

    public boolean delete(int userId, long historyId) throws SQLException {
        return listenHistoryRepository.deleteForUser(userId, historyId);
    }

    public int clear(int userId) throws SQLException {
        return listenHistoryRepository.clearAllForUser(userId);
    }
}
