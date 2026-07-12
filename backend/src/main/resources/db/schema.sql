-- =====================================================
-- MUSIC APP - SCHEMA for MySQL
-- =====================================================
CREATE DATABASE IF NOT EXISTS music_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE music_app;

-- =====================================================
-- 1. NGUOI DUNG (co role ADMIN)
-- =====================================================
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,           -- BCrypt, khong luu plain text
    display_name  VARCHAR(100),
    avatar_url    VARCHAR(500),
    role          VARCHAR(10) NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER','ADMIN')),
    status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE','BANNED')),
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

-- =====================================================
-- 2. NGHE SI
-- =====================================================
CREATE TABLE artists (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(220) NOT NULL,              -- URL: /artist/son-tung-mtp
    bio         TEXT,
    image_url   VARCHAR(500),
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6) NULL,                   -- soft delete: admin an thay vi xoa
    CONSTRAINT uk_artists_slug UNIQUE (slug)
);
CREATE INDEX idx_artists_name ON artists(name);

-- =====================================================
-- 3. ALBUM
-- =====================================================
CREATE TABLE albums (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    slug         VARCHAR(280) NOT NULL,
    artist_id    INT NOT NULL,
    album_type   VARCHAR(10) NOT NULL DEFAULT 'ALBUM'
                 CHECK (album_type IN ('ALBUM','EP','SINGLE')),
    cover_url    VARCHAR(500),
    release_date DATE,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   DATETIME(6) NULL,
    CONSTRAINT uk_albums_slug UNIQUE (slug),
    CONSTRAINT fk_albums_artist FOREIGN KEY (artist_id)
        REFERENCES artists(id) ON DELETE RESTRICT
);
CREATE INDEX idx_albums_artist ON albums(artist_id);

-- =====================================================
-- 4. BAI HAT
-- =====================================================
CREATE TABLE songs (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    slug         VARCHAR(280) NOT NULL,
    album_id     INT NULL,                          -- NULL = single le
    track_number SMALLINT NULL,
    duration_sec INT NOT NULL DEFAULT 0,
    file_path    VARCHAR(500) NOT NULL,
    cover_url    VARCHAR(500),
    lyrics       TEXT,
    status       VARCHAR(10) NOT NULL DEFAULT 'PUBLISHED'
                 CHECK (status IN ('DRAFT','PUBLISHED','HIDDEN')),  -- admin duyet/an bai
    play_count   BIGINT NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   DATETIME(6) NULL,
    CONSTRAINT uk_songs_slug UNIQUE (slug),
    CONSTRAINT fk_songs_album FOREIGN KEY (album_id)
        REFERENCES albums(id) ON DELETE SET NULL
);
CREATE INDEX idx_songs_album     ON songs(album_id, track_number);
CREATE INDEX idx_songs_playcount ON songs(play_count DESC);

-- =====================================================
-- 5. LOI BAI HAT
-- =====================================================
CREATE TABLE song_lyrics (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id        INT NOT NULL,
    line_number    INT NOT NULL,
    start_time_ms  INT NOT NULL,
    lyric_text     VARCHAR(500) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_song_lyrics_song
        FOREIGN KEY (song_id)
        REFERENCES songs(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_song_lyrics_song
ON song_lyrics(song_id, start_time_ms);

-- Nghe si cua bai hat (n-n: ho tro feat.)
CREATE TABLE song_artists (
    song_id    INT NOT NULL,
    artist_id  INT NOT NULL,
    role       VARCHAR(10) NOT NULL DEFAULT 'MAIN'
               CHECK (role IN ('MAIN','FEATURED')),
    position   TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (song_id, artist_id),
    CONSTRAINT fk_sa_song   FOREIGN KEY (song_id)
        REFERENCES songs(id)   ON DELETE CASCADE,
    CONSTRAINT fk_sa_artist FOREIGN KEY (artist_id)
        REFERENCES artists(id) ON DELETE RESTRICT
);
CREATE INDEX idx_sa_artist ON song_artists(artist_id);

-- =====================================================
-- 6. THE LOAI
-- =====================================================
CREATE TABLE genres (
    id   SMALLINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    CONSTRAINT uk_genres_slug UNIQUE (slug)
);

CREATE TABLE song_genres (
    song_id  INT NOT NULL,
    genre_id SMALLINT NOT NULL,
    PRIMARY KEY (song_id, genre_id),
    CONSTRAINT fk_sg_song  FOREIGN KEY (song_id)
        REFERENCES songs(id)  ON DELETE CASCADE,
    CONSTRAINT fk_sg_genre FOREIGN KEY (genre_id)
        REFERENCES genres(id) ON DELETE RESTRICT
);
CREATE INDEX idx_sg_genre ON song_genres(genre_id);

-- =====================================================
-- 7. PLAYLIST
-- =====================================================
CREATE TABLE playlists (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    user_id     INT NOT NULL,
    cover_url   VARCHAR(500),
    is_public   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_playlists_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_playlists_user   ON playlists(user_id);
CREATE INDEX idx_playlists_public ON playlists(is_public, updated_at DESC);

CREATE TABLE playlist_songs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id INT NOT NULL,
    song_id     INT NOT NULL,
    position    INT NOT NULL,                        -- danh cach quang 1000, 2000... de chen giua
    added_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ps_playlist FOREIGN KEY (playlist_id)
        REFERENCES playlists(id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_song     FOREIGN KEY (song_id)
        REFERENCES songs(id)     ON DELETE CASCADE
);
CREATE INDEX idx_ps_playlist ON playlist_songs(playlist_id, position);
CREATE INDEX idx_ps_song     ON playlist_songs(song_id);

-- =====================================================
-- 8. TUONG TAC: LIKE / FOLLOW
-- =====================================================
CREATE TABLE song_likes (
    user_id    INT NOT NULL,
    song_id    INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, song_id),
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_song FOREIGN KEY (song_id)
        REFERENCES songs(id) ON DELETE CASCADE
);
CREATE INDEX idx_likes_song      ON song_likes(song_id);
CREATE INDEX idx_likes_user_time ON song_likes(user_id, created_at DESC);

CREATE TABLE artist_follows (
    user_id    INT NOT NULL,
    artist_id  INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, artist_id),
    CONSTRAINT fk_follows_user   FOREIGN KEY (user_id)
        REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_follows_artist FOREIGN KEY (artist_id)
        REFERENCES artists(id)  ON DELETE CASCADE
);
CREATE INDEX idx_follows_artist ON artist_follows(artist_id);

-- =====================================================
-- 9. LICH SU NGHE
-- =====================================================
CREATE TABLE listen_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    song_id     INT NOT NULL,
    played_sec  INT NOT NULL DEFAULT 0,              -- nghe >=30s moi tinh 1 play
    listened_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_lh_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_lh_song FOREIGN KEY (song_id)
        REFERENCES songs(id) ON DELETE CASCADE
);
CREATE INDEX idx_lh_user_time ON listen_history(user_id, listened_at DESC);
CREATE INDEX idx_lh_song_time ON listen_history(song_id, listened_at);
