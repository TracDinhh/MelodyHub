package com.melodyHub.service.artist;

import com.melodyHub.dto.response.PagedResponse;
import com.melodyHub.dto.response.SongResponse;
import com.melodyHub.repository.SongRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ArtistSongService {
    private final SongRepository songRepository;

    public ArtistSongService() {
        this(new SongRepository());
    }

    public ArtistSongService(SongRepository songRepository) {
        this.songRepository = Objects.requireNonNull(songRepository, "songRepository must not be null");
    }

    public PagedResponse<SongResponse> getOwnPage(int artistId, int page, int size) throws SQLException {
        int offset = calculateOffset(page, size);
        List<SongResponse> items = songRepository.getOwnedPage(artistId, size, offset)
                .stream()
                .map(SongResponse::fromEntity)
                .toList();
        long total = songRepository.countOwned(artistId);

        return new PagedResponse<>(items, total, page, size);
    }

    private int calculateOffset(int page, int size) {
        long offset = ((long) page - 1L) * size;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page is too large");
        }
        return (int) offset;
    }

    public Optional<SongResponse> getOwnByIdentifier(int artistId, String identifier) throws SQLException {
        String normalizedIdentifier = normalize(identifier);
        if (normalizedIdentifier == null) {
            return Optional.empty();
        }

        Integer songId = parsePositiveInteger(normalizedIdentifier);
        if (songId != null) {
            Optional<SongResponse> songById = songRepository.findOwnedById(artistId, songId)
                    .map(SongResponse::fromEntity);
            if (songById.isPresent()) {
                return songById;
            }
        }

        return songRepository.findOwnedBySlug(artistId, normalizedIdentifier)
                .map(SongResponse::fromEntity);
    }

    private Integer parsePositiveInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
