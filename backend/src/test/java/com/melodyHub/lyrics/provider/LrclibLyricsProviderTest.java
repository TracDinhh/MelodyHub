package com.melodyHub.lyrics.provider;

import com.melodyHub.lyrics.LyricsSearchResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LrclibLyricsProviderTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesByTrackNameWhenArtistFilteredSearchIsEmpty() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/get", exchange -> respond(exchange, 404, ""));
        server.createContext("/api/search", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            if (query.contains("artist_name=") || query.contains("album_name=")) {
                respond(exchange, 200, "[]");
                return;
            }
            respond(exchange, 200, """
                    [{
                      "trackName": "Come My Way",
                      "artistName": "Sơn Tùng M-TP & Tyga",
                      "albumName": "Come My Way",
                      "duration": 193,
                      "syncedLyrics": "[00:00.00]Yeah"
                    }]
                    """);
        });
        server.start();

        LrclibLyricsProvider provider = new LrclibLyricsProvider(baseUrl(), "MelodyHub-Test/1.0");

        List<LyricsSearchResult> results = provider.search(
                "Come My Way", "Sơn Tùng MTP", "MelodyHub Single", 193);

        assertEquals(List.of("Sơn Tùng M-TP & Tyga"),
                results.stream().map(LyricsSearchResult::artistName).toList());
    }

    @Test
    void keepsMatchingLyricsThatAppearAfterTheFirstTenSearchResults() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/get", exchange -> respond(exchange, 404, ""));
        server.createContext("/api/search", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            if (query.contains("artist_name=")) {
                respond(exchange, 200, "[]");
                return;
            }

            String decoys = IntStream.range(0, 11)
                    .mapToObj(index -> resultJson("Other Artist " + index, 180 + index))
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            respond(exchange, 200, "[" + decoys + ","
                    + resultJson("Sơn Tùng M-TP & Tyga", 193) + "]");
        });
        server.start();

        LrclibLyricsProvider provider = new LrclibLyricsProvider(baseUrl(), "MelodyHub-Test/1.0");

        List<LyricsSearchResult> results = provider.search(
                "Come My Way", "Sơn Tùng MTP", null, 193);

        assertTrue(results.stream().anyMatch(result ->
                "Sơn Tùng M-TP & Tyga".equals(result.artistName())));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String resultJson(String artistName, int duration) {
        return """
                {
                  "trackName": "Come My Way",
                  "artistName": "%s",
                  "duration": %d,
                  "syncedLyrics": "[00:00.00]Lyrics"
                }
                """.formatted(artistName, duration);
    }
}
