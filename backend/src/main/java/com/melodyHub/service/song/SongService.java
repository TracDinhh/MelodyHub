package com.melodyHub.service.song;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SongService {
    private final SongRepository songRepository;

    public SongService() {
        this(new SongRepository());
    }

    public SongService(SongRepository songRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public PagedResponse<SongResponse> getPage(int page, int size) throws SQLException {
        int offset = (page - 1) * size;

        List<SongResponse> items = songRepository.getPage(size, offset)
                .stream()
                .map(SongResponse::fromEntity)
                .toList();
        long total = songRepository.count();

        return new PagedResponse<>(items, total, page, size);
    }

    public Optional<SongResponse> getBySlug(String slug) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        return songRepository.findBySlug(slug.trim())
                .map(SongResponse::fromEntity);
    }
}
