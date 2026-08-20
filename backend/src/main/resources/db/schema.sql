-- =====================================================
-- MUSIC APP - SCHEMA for MySQL
-- =====================================================
CREATE DATABASE IF NOT EXISTS music_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE music_app;

-- =====================================================
-- 1. NGUOI DUNG (USER / ARTIST / ADMIN)
-- =====================================================
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,           -- BCrypt, khong luu plain text
    display_name  VARCHAR(100),
    phone         VARCHAR(20),
    avatar_url    VARCHAR(500),
    role          VARCHAR(10) NOT NULL DEFAULT 'USER'
                  CONSTRAINT chk_users_role CHECK (role IN ('USER','ADMIN')),
    status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                  CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','BANNED')),
    premium_until DATETIME(6) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

CREATE TABLE payment_orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    plan_code     VARCHAR(32) NOT NULL,
    amount        INT NOT NULL,
    currency      VARCHAR(8) NOT NULL DEFAULT 'VND',
    premium_days  INT NOT NULL,
    transfer_note VARCHAR(64) NOT NULL UNIQUE,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING','CONFIRMED','REJECTED','EXPIRED')),
    confirmed_by  INT NULL,                          -- NULL = nguoi dung tu kich hoat sau khi chuyen khoan
    confirmed_at  DATETIME(6) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_confirmer FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_payment_user (user_id),
    INDEX idx_payment_status (status)
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

CREATE TABLE refresh_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    token_hash   CHAR(64) NOT NULL,
    expires_at   DATETIME(6) NOT NULL,
    revoked_at   DATETIME(6) NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id, expires_at);

-- =====================================================
-- PASSWORD RESET TOKENS
-- =====================================================
CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    token_hash  CHAR(64) NOT NULL,                  -- SHA-256 of the actual token
    expires_at  DATETIME(6) NOT NULL,
    used_at     DATETIME(6) NULL,                   -- NULL = not yet used
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id, expires_at);

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
    lyrics_type  VARCHAR(10) NOT NULL DEFAULT 'PLAIN'
                 CHECK (lyrics_type IN ('PLAIN','SYNCED')),
    status       VARCHAR(10) NOT NULL DEFAULT 'DRAFT'
                 CONSTRAINT chk_songs_status
                 CHECK (status IN ('DRAFT','SUBMITTED','PUBLISHED','REJECTED','HIDDEN')),
    play_count   BIGINT NOT NULL DEFAULT 0,
    submitted_at DATETIME(6) NULL,                      -- Artist gui duyet
    review_note  VARCHAR(500) NULL,                     -- Admin ly do reject
    reviewed_by  INT NULL,                              -- Admin duyet/reject
    reviewed_at  DATETIME(6) NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   DATETIME(6) NULL,
    CONSTRAINT uk_songs_slug UNIQUE (slug),
    CONSTRAINT fk_songs_album FOREIGN KEY (album_id)
        REFERENCES albums(id) ON DELETE SET NULL,
    CONSTRAINT fk_songs_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_songs_album     ON songs(album_id, track_number);
CREATE INDEX idx_songs_playcount ON songs(play_count DESC);
-- Primary listener-facing browse: WHERE status = 'PUBLISHED' AND deleted_at IS NULL
-- ORDER BY created_at DESC, id DESC. Covers the catalog listing + its COUNT(*).
CREATE INDEX idx_songs_status_created ON songs(status, deleted_at, created_at DESC, id DESC);

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
    CONSTRAINT uk_genres_name UNIQUE (name),
    CONSTRAINT uk_genres_slug UNIQUE (slug)
);

CREATE TABLE song_genres (
    song_id  INT NOT NULL,
    genre_id SMALLINT NOT NULL,
    position TINYINT NOT NULL DEFAULT 0,   -- 0 = primary genre, 1/2 = secondary
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

-- =====================================================
-- =====================================================
-- 10b. ARTIST MEMBERSHIP (V2 artist architecture)
-- Replaces the 1:1 artists.user_id model with an N:N membership table.
-- MVP roles: OWNER, MANAGER. No status column: exists = active.
-- =====================================================
CREATE TABLE IF NOT EXISTS artist_members (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    artist_id  INT NOT NULL,
    user_id    INT NOT NULL,
    role       VARCHAR(10) NOT NULL DEFAULT 'OWNER'
               CHECK (role IN ('OWNER','MANAGER')),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_artist_members UNIQUE (artist_id, user_id),
    CONSTRAINT fk_am_artist FOREIGN KEY (artist_id)
        REFERENCES artists(id) ON DELETE CASCADE,
    CONSTRAINT fk_am_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_am_user   ON artist_members(user_id);
CREATE INDEX idx_am_artist ON artist_members(artist_id);

-- =====================================================
-- 10c. ARTIST ACCESS REQUESTS (V2 artist architecture)
-- Replaces artist_requests with richer semantics:
--   CLAIM_ARTIST: user claims an existing artist profile
--   CREATE_ARTIST: user requests creation of a new artist profile
-- MVP restriction: CREATE_ARTIST only allows relationship=ARTIST.
-- =====================================================
CREATE TABLE IF NOT EXISTS artist_access_requests (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    user_id               INT NOT NULL,
    artist_id             INT NULL,                              -- CLAIM_ARTIST only

    request_type          VARCHAR(20) NOT NULL
                          CHECK (request_type IN ('CLAIM_ARTIST','CREATE_ARTIST')),

    requested_artist_name VARCHAR(200) NULL,                     -- CREATE_ARTIST only
    requested_bio         TEXT NULL,                             -- CREATE_ARTIST only
    requested_image_url   VARCHAR(500) NULL,                     -- CREATE_ARTIST only

    relationship          VARCHAR(20) NOT NULL DEFAULT 'ARTIST'
                          CHECK (relationship IN ('ARTIST','MANAGER','LABEL','TEAM_MEMBER','OTHER')),

    website_url           VARCHAR(500) NULL,
    social_url            VARCHAR(500) NULL,
    message               TEXT NULL,

    status                VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    review_note           VARCHAR(500) NULL,
    reviewed_by           INT NULL,
    reviewed_at           DATETIME(6) NULL,
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_aar_user     FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_aar_artist   FOREIGN KEY (artist_id)   REFERENCES artists(id) ON DELETE SET NULL,
    CONSTRAINT fk_aar_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_aar_user_status   ON artist_access_requests(user_id, status);
CREATE INDEX idx_aar_artist_status ON artist_access_requests(artist_id, status);
CREATE INDEX idx_aar_status_date   ON artist_access_requests(status, created_at);

-- =====================================================
-- 11. SEED DATA
-- =====================================================
-- Tai khoan ADMIN mac dinh.
-- Dang nhap: username = admin  |  password = Admin@123456
-- Hash BCrypt (cost 12) tao bang PasswordUtil/jBCrypt cua backend.
-- LUU Y: doi mat khau nay ngay sau lan dang nhap dau tien tren moi truong that.
INSERT INTO users (username, email, password_hash, display_name, role, status)
VALUES (
    'admin',
    'admin@gmail.com',
    '$2a$12$OH.JXdKvdtxZ6SC5JY908uJzOmylnn/6vUIrQKvY3ZGB8jvqWV9hS',
    'MelodyHub Admin',
    'ADMIN',
    'ACTIVE'
);

-- -----------------------------------------------------
-- Tai khoan nghe si mau (password = Admin@123456)
-- -----------------------------------------------------
INSERT INTO users (username, email, password_hash, display_name, role, status) VALUES
('lena', 'lena@melodyhub.local', '$2a$12$OH.JXdKvdtxZ6SC5JY908uJzOmylnn/6vUIrQKvY3ZGB8jvqWV9hS', 'Lena Rivers', 'USER', 'ACTIVE'),
('eli',  'eli@melodyhub.local',  '$2a$12$OH.JXdKvdtxZ6SC5JY908uJzOmylnn/6vUIrQKvY3ZGB8jvqWV9hS', 'Eli Vale',    'USER', 'ACTIVE');

-- Ho so nghe si (lien ket qua artist_members)
INSERT INTO artists (name, slug, bio, image_url) VALUES
('Lena Rivers', 'lena-rivers',
 'Lena Rivers uon alternative R&B quanh synth dem, day dan song va giong hat mong.',
 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=600&q=85'),
('Eli Vale', 'eli-vale',
 'Eli Vale mang mau indie pop tuoi sang, giai dieu de nghe.',
 'https://images.unsplash.com/photo-1521337581100-8ca9a73a5f79?auto=format&fit=crop&w=600&q=85');

-- Seed memberships for the sample artist accounts.
-- Linked by username so the seed does not depend on AUTO_INCREMENT ids.
INSERT INTO artist_members (artist_id, user_id, role)
SELECT a.id, u.id, 'OWNER'
FROM artists a
JOIN users u ON u.username = 'lena'
WHERE a.slug = 'lena-rivers'
  AND NOT EXISTS (
      SELECT 1 FROM artist_members am
      WHERE am.artist_id = a.id AND am.user_id = u.id
  );

INSERT INTO artist_members (artist_id, user_id, role)
SELECT a.id, u.id, 'OWNER'
FROM artists a
JOIN users u ON u.username = 'eli'
WHERE a.slug = 'eli-vale'
  AND NOT EXISTS (
      SELECT 1 FROM artist_members am
      WHERE am.artist_id = a.id AND am.user_id = u.id
  );

-- -----------------------------------------------------
-- Genre master data (Artist chon tu danh sach nay, khong free-text)
-- -----------------------------------------------------
INSERT INTO genres (name, slug) VALUES
('Pop', 'pop'),
('Hip-Hop', 'hip-hop'),
('Rap', 'rap'),
('R&B', 'r-and-b'),
('Rock', 'rock'),
('Alternative', 'alternative'),
('Indie', 'indie'),
('Electronic', 'electronic'),
('EDM', 'edm'),
('House', 'house'),
('Dance', 'dance'),
('Ballad', 'ballad'),
('Jazz', 'jazz'),
('Classical', 'classical'),
('Country', 'country'),
('Folk', 'folk'),
('Metal', 'metal'),
('Reggae', 'reggae'),
('Soul', 'soul'),
('Funk', 'funk'),
('Lo-fi', 'lo-fi'),
('Acoustic', 'acoustic');

-- -----------------------------------------------------
-- 10 bai hat mau (audio dung file mp3 cong khai cua SoundHelix)
-- -----------------------------------------------------
INSERT INTO songs (title, slug, duration_sec, file_path, cover_url, lyrics, lyrics_type, status, play_count) VALUES
('Velvet Hours',  'velvet-hours',  238, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',  'https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=500&q=85', '{"lines":[{"startTime":0,"endTime":4,"text":"Velvet hours in the city glow"},{"startTime":4,"endTime":8,"text":"Hold the moment, let the speakers slow"},{"startTime":8,"endTime":12,"text":"We keep dancing through the neon blue"},{"startTime":12,"endTime":16,"text":"Every midnight leads me back to you"}]}', 'SYNCED', 'PUBLISHED', 84291),
('Afterglow',     'afterglow',     214, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',  'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 61830),
('Slow Motion',   'slow-motion',   201, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',  'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 44105),
('No Signal',     'no-signal',     189, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',  'https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 37826),
('Open Water',    'open-water',    246, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',  'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 29313),
('Midnight Bloom','midnight-bloom',220, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',  'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 25110),
('Paper Moons',   'paper-moons',   226, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',  'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 22108),
('Sunroom',       'sunroom',       196, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',  'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 19912),
('Frequency',     'frequency',     232, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',  'https://images.unsplash.com/photo-1524650359799-842906ca1c06?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 16709),
('Low Light',     'low-light',     208, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=500&q=85', NULL, 'PLAIN', 'PUBLISHED', 12044);

-- Gan nghe si cho tung bai (MAIN). Dung slug de khong phu thuoc ID.
INSERT INTO song_artists (song_id, artist_id, role, position) VALUES
((SELECT id FROM songs WHERE slug = 'velvet-hours'),   (SELECT id FROM artists WHERE slug = 'lena-rivers'), 'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'afterglow'),       (SELECT id FROM artists WHERE slug = 'lena-rivers'), 'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'slow-motion'),     (SELECT id FROM artists WHERE slug = 'lena-rivers'), 'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'no-signal'),       (SELECT id FROM artists WHERE slug = 'lena-rivers'), 'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'open-water'),      (SELECT id FROM artists WHERE slug = 'lena-rivers'), 'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'midnight-bloom'),  (SELECT id FROM artists WHERE slug = 'lena-rivers'), 'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'paper-moons'),     (SELECT id FROM artists WHERE slug = 'eli-vale'),    'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'sunroom'),         (SELECT id FROM artists WHERE slug = 'eli-vale'),    'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'frequency'),       (SELECT id FROM artists WHERE slug = 'eli-vale'),    'MAIN', 0),
((SELECT id FROM songs WHERE slug = 'low-light'),       (SELECT id FROM artists WHERE slug = 'eli-vale'),    'MAIN', 0);

-- Gan genre cho tung bai mau (position 0 = primary genre).
INSERT INTO song_genres (song_id, genre_id, position) VALUES
((SELECT id FROM songs WHERE slug = 'velvet-hours'),   (SELECT id FROM genres WHERE slug = 'alternative'), 0),
((SELECT id FROM songs WHERE slug = 'velvet-hours'),   (SELECT id FROM genres WHERE slug = 'r-and-b'),      1),
((SELECT id FROM songs WHERE slug = 'afterglow'),      (SELECT id FROM genres WHERE slug = 'electronic'),   0),
((SELECT id FROM songs WHERE slug = 'afterglow'),      (SELECT id FROM genres WHERE slug = 'dance'),        1),
((SELECT id FROM songs WHERE slug = 'slow-motion'),    (SELECT id FROM genres WHERE slug = 'r-and-b'),      0),
((SELECT id FROM songs WHERE slug = 'slow-motion'),    (SELECT id FROM genres WHERE slug = 'soul'),         1),
((SELECT id FROM songs WHERE slug = 'no-signal'),      (SELECT id FROM genres WHERE slug = 'electronic'),   0),
((SELECT id FROM songs WHERE slug = 'no-signal'),      (SELECT id FROM genres WHERE slug = 'edm'),          1),
((SELECT id FROM songs WHERE slug = 'open-water'),     (SELECT id FROM genres WHERE slug = 'indie'),        0),
((SELECT id FROM songs WHERE slug = 'open-water'),     (SELECT id FROM genres WHERE slug = 'folk'),         1),
((SELECT id FROM songs WHERE slug = 'midnight-bloom'), (SELECT id FROM genres WHERE slug = 'pop'),          0),
((SELECT id FROM songs WHERE slug = 'midnight-bloom'), (SELECT id FROM genres WHERE slug = 'r-and-b'),      1),
((SELECT id FROM songs WHERE slug = 'midnight-bloom'), (SELECT id FROM genres WHERE slug = 'ballad'),       2),
((SELECT id FROM songs WHERE slug = 'paper-moons'),    (SELECT id FROM genres WHERE slug = 'indie'),        0),
((SELECT id FROM songs WHERE slug = 'paper-moons'),    (SELECT id FROM genres WHERE slug = 'pop'),          1),
((SELECT id FROM songs WHERE slug = 'sunroom'),        (SELECT id FROM genres WHERE slug = 'acoustic'),     0),
((SELECT id FROM songs WHERE slug = 'sunroom'),        (SELECT id FROM genres WHERE slug = 'indie'),        1),
((SELECT id FROM songs WHERE slug = 'frequency'),      (SELECT id FROM genres WHERE slug = 'edm'),          0),
((SELECT id FROM songs WHERE slug = 'frequency'),      (SELECT id FROM genres WHERE slug = 'house'),        1),
((SELECT id FROM songs WHERE slug = 'low-light'),      (SELECT id FROM genres WHERE slug = 'jazz'),         0),
((SELECT id FROM songs WHERE slug = 'low-light'),      (SELECT id FROM genres WHERE slug = 'lo-fi'),        1);

-- Bai hat mau co lyrics dong bo de kiem thu Premium lyrics / lyric card.
INSERT INTO song_lyrics (song_id, line_number, start_time_ms, lyric_text) VALUES
((SELECT id FROM songs WHERE slug = 'velvet-hours'), 1,     0, 'Velvet hours in the city glow'),
((SELECT id FROM songs WHERE slug = 'velvet-hours'), 2,  4000, 'Hold the moment, let the speakers slow'),
((SELECT id FROM songs WHERE slug = 'velvet-hours'), 3,  8000, 'We keep dancing through the neon blue'),
((SELECT id FROM songs WHERE slug = 'velvet-hours'), 4, 12000, 'Every midnight leads me back to you');
