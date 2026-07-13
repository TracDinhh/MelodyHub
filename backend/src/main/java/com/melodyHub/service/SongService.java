package com.melodyHub.service;

import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class SongService {
    private final SongRepository songRepository;

    public SongService() {
        this(new SongRepository());
    }

    public SongService(SongRepository songRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public List<SongResponse> getAll() throws SQLException {
        return songRepository.getAll()
                .stream()
                .map(SongResponse::fromEntity)
                .toList();
    }
}
