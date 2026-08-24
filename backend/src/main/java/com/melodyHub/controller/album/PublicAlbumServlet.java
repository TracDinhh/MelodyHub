package com.melodyHub.controller.album;

import com.melodyHub.controller.JsonServlet;
import com.melodyHub.dto.response.AlbumResponse;
import com.melodyHub.entity.Album;
import com.melodyHub.entity.Song;
import com.melodyHub.entity.Artist;
import com.melodyHub.repository.AlbumRepository;
import com.melodyHub.repository.SongRepository;
import com.melodyHub.repository.ArtistRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Public album browsing. No authentication required.
 *
 * <ul>
 *   <li>{@code GET /api/albums/{slug}}                    — album detail with songs</li>
 *   <li>{@code GET /api/albums?artistSlug=xxx}           — albums by artist slug</li>
 * </ul>
 */
public class PublicAlbumServlet extends JsonServlet {
    private final AlbumRepository albumRepository = new AlbumRepository();
    private final SongRepository songRepository = new SongRepository();
    private final ArtistRepository artistRepository = new ArtistRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = getPath(request);

            if ("/".equals(path)) {
                handleByArtistSlug(request, response);
                return;
            }

            String slug = path.startsWith("/") ? path.substring(1) : path;
            handleDetail(response, slug);
        } catch (SQLException exception) {
            writeDatabaseError(response);
        }
    }

    private void handleByArtistSlug(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        String artistSlug = request.getParameter("artistSlug");
        if (artistSlug == null || artistSlug.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "MISSING_ARTIST_SLUG", "artistSlug query param is required");
            return;
        }

        Optional<com.melodyHub.entity.Artist> artistOpt = artistRepository.findActiveBySlug(artistSlug);
        if (artistOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "ARTIST_NOT_FOUND", "Artist not found");
            return;
        }

        int artistId = artistOpt.get().getId();
        List<Album> albums = albumRepository.findByArtistId(artistId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Album album : albums) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", album.getId());
            item.put("title", album.getTitle());
            item.put("slug", album.getSlug());
            item.put("albumType", album.getAlbumType());
            item.put("coverUrl", album.getCoverUrl());
            item.put("releaseDate", album.getReleaseDate());
            item.put("songCount", songRepository.countPublishedByAlbum(album.getId()));
            result.add(item);
        }

        writeJson(response, HttpServletResponse.SC_OK, result);
    }

    private void handleDetail(HttpServletResponse response, String slug) throws IOException, SQLException {
        if (slug == null || slug.isBlank()) {
            writeNotFound(response);
            return;
        }

        Optional<Album> albumOpt = albumRepository.findActiveBySlug(slug);
        if (albumOpt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "ALBUM_NOT_FOUND", "Album not found");
            return;
        }

        Album album = albumOpt.get();

        // Get artist info
        Optional<com.melodyHub.entity.Artist> artistOpt = artistRepository.findActiveById(album.getArtistId());
        String artistName = artistOpt.map(com.melodyHub.entity.Artist::getName).orElse("Unknown Artist");

        // Get published songs
        List<Song> songs = songRepository.findPublishedByAlbum(album.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", album.getId());
        result.put("title", album.getTitle());
        result.put("slug", album.getSlug());
        result.put("albumType", album.getAlbumType());
        result.put("coverUrl", album.getCoverUrl());
        result.put("releaseDate", album.getReleaseDate());
        result.put("artistId", album.getArtistId());
        result.put("artistName", artistName);
        result.put("songCount", songs.size());

        List<Map<String, Object>> songList = new ArrayList<>();
        for (Song song : songs) {
            Map<String, Object> songItem = new LinkedHashMap<>();
            songItem.put("id", song.getId());
            songItem.put("title", song.getTitle());
            songItem.put("slug", song.getSlug());
            songItem.put("coverUrl", song.getCoverUrl());
            songItem.put("durationSec", song.getDurationSec());
            songItem.put("playCount", song.getPlayCount());
            songItem.put("audioUrl", song.getFilePath());

            // Get artists for this song
            List<Artist> artists = songRepository.findArtistsForSong(song.getId());
            List<Map<String, String>> artistList = new ArrayList<>();
            for (Artist artist : artists) {
                Map<String, String> artistMap = new LinkedHashMap<>();
                artistMap.put("id", String.valueOf(artist.getId()));
                artistMap.put("name", artist.getName());
                artistMap.put("slug", artist.getSlug());
                artistList.add(artistMap);
            }
            songItem.put("artists", artistList);

            songList.add(songItem);
        }
        result.put("songs", songList);

        writeJson(response, HttpServletResponse.SC_OK, result);
    }

    private void writeNotFound(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Album not found");
    }

    private void writeDatabaseError(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Database error occurred");
    }
}
