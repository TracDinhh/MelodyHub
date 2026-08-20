package com.melodyHub.lyrics.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyHub.lyrics.LyricsProvider;
import com.melodyHub.lyrics.LyricsProviderException;
import com.melodyHub.lyrics.LyricsSearchResult;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * LRCLIB lyrics provider. Uses the official LRCLIB API at lrclib.net.
 * <p>
 * Attempts {@code GET /api/get} (exact lookup) first, then falls back to
 * {@code GET /api/search} when the exact match returns 404.
 * <p>
 * LRCLIB does <b>not</b> generate lyrics — it retrieves lyrics that already exist.
 */
public class LrclibLyricsProvider implements LyricsProvider {

    private static final String DEFAULT_BASE_URL = "https://lrclib.net";
    private static final String DEFAULT_CLIENT_NAME = "MelodyHub/1.0";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_SEARCH_RESULTS = 50;

    private final String baseUrl;
    private final String clientName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LrclibLyricsProvider() {
        this(DEFAULT_BASE_URL, DEFAULT_CLIENT_NAME);
    }

    public LrclibLyricsProvider(String baseUrl, String clientName) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        this.clientName = clientName != null ? clientName : DEFAULT_CLIENT_NAME;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "LRCLIB";
    }

    @Override
    public List<LyricsSearchResult> search(String trackName, String artistName,
                                            String albumName, Integer durationSec)
            throws LyricsProviderException {

        // 1. Try exact lookup first
        LyricsSearchResult exact = exactLookup(trackName, artistName, albumName, durationSec);
        if (exact != null) {
            return List.of(exact);
        }

        // 2. Search with the catalog artist first.
        List<LyricsSearchResult> results = searchEndpoint(trackName, artistName, albumName);
        if (!results.isEmpty()) {
            return results;
        }

        // 3. LRCLIB may store collaborations under a different artist string.
        return searchEndpoint(trackName, null, null);
    }

    /**
     * Calls {@code GET /api/get} with exact query parameters.
     * Returns a single result or null if 404.
     */
    private LyricsSearchResult exactLookup(String trackName, String artistName,
                                            String albumName, Integer durationSec)
            throws LyricsProviderException {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/get?track_name=").append(encode(trackName))
                .append("&artist_name=").append(encode(artistName));

        if (albumName != null && !albumName.isBlank()) {
            url.append("&album_name=").append(encode(albumName));
        }
        if (durationSec != null && durationSec > 0) {
            url.append("&duration=").append(durationSec);
        }

        HttpResponse<String> response = doGet(url.toString());

        if (response.statusCode() == 404) {
            return null; // Not found — try search
        }

        handleErrorStatus(response);

        try {
            JsonNode node = objectMapper.readTree(response.body());
            return mapResult(node);
        } catch (IOException e) {
            throw new LyricsProviderException("INVALID_RESPONSE",
                    "LRCLIB returned invalid JSON", e);
        }
    }

    /**
     * Calls {@code GET /api/search} for broader results.
     */
    private List<LyricsSearchResult> searchEndpoint(String trackName, String artistName,
                                                     String albumName)
            throws LyricsProviderException {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/search?track_name=").append(encode(trackName));

        if (artistName != null && !artistName.isBlank()) {
            url.append("&artist_name=").append(encode(artistName));
        }

        if (albumName != null && !albumName.isBlank()) {
            url.append("&album_name=").append(encode(albumName));
        }

        HttpResponse<String> response = doGet(url.toString());
        handleErrorStatus(response);

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return List.of();
            }

            List<LyricsSearchResult> results = new ArrayList<>();
            for (int i = 0; i < Math.min(root.size(), MAX_SEARCH_RESULTS); i++) {
                LyricsSearchResult result = mapResult(root.get(i));
                if (result != null) {
                    results.add(result);
                }
            }
            return results;
        } catch (IOException e) {
            throw new LyricsProviderException("INVALID_RESPONSE",
                    "LRCLIB returned invalid JSON", e);
        }
    }

    private HttpResponse<String> doGet(String url) throws LyricsProviderException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", clientName)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LyricsProviderException("PROVIDER_UNAVAILABLE",
                    "Request to LRCLIB was interrupted", e);
        } catch (IOException e) {
            throw new LyricsProviderException("PROVIDER_UNAVAILABLE",
                    "Unable to reach LRCLIB: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new LyricsProviderException("INVALID_REQUEST",
                    "Invalid request to LRCLIB: " + e.getMessage(), e);
        }
    }

    private void handleErrorStatus(HttpResponse<String> response) throws LyricsProviderException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return; // Success
        }
        if (status == 429) {
            String retryAfter = response.headers()
                    .firstValue("Retry-After").orElse("unknown");
            throw new LyricsProviderException("RATE_LIMITED",
                    "LRCLIB rate limited. Retry-After: " + retryAfter);
        }
        if (status >= 500) {
            throw new LyricsProviderException("PROVIDER_UNAVAILABLE",
                    "LRCLIB server error (HTTP " + status + ")");
        }
        // Other non-success status
        throw new LyricsProviderException("PROVIDER_ERROR",
                "LRCLIB returned HTTP " + status);
    }

    private LyricsSearchResult mapResult(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String trackName = textOrNull(node, "trackName");
        String artistName = textOrNull(node, "artistName");
        String albumName = textOrNull(node, "albumName");
        Integer duration = node.has("duration") && node.get("duration").isNumber()
                ? (int) Math.round(node.get("duration").asDouble()) : null;
        String syncedLyrics = textOrNull(node, "syncedLyrics");
        String plainLyrics = textOrNull(node, "plainLyrics");

        // Skip results that have neither synced nor plain lyrics
        if (syncedLyrics == null && plainLyrics == null) {
            return null;
        }

        return new LyricsSearchResult(trackName, artistName, albumName,
                duration, syncedLyrics, plainLyrics);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull() || child.isMissingNode()) {
            return null;
        }
        String text = child.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
