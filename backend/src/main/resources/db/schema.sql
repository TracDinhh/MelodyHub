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
    avatar_url    VARCHAR(500),
    role          VARCHAR(10) NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER','ARTIST','ADMIN')),
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
    user_id     INT NULL,                           -- NULL = artist profile chua lien ket tai khoan
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(220) NOT NULL,              -- URL: /artist/son-tung-mtp
    bio         TEXT,
    image_url   VARCHAR(500),
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6) NULL,                   -- soft delete: admin an thay vi xoa
    CONSTRAINT uk_artists_slug UNIQUE (slug),
    CONSTRAINT uk_artists_user UNIQUE (user_id),
    CONSTRAINT fk_artists_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
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

DELIMITER //

CREATE TRIGGER trg_artists_user_must_be_artist_before_insert
BEFORE INSERT ON artists
FOR EACH ROW
BEGIN
    IF NEW.user_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM users
           WHERE id = NEW.user_id
             AND role = 'ARTIST'
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Artist profile user_id must reference an ARTIST user';
    END IF;
END//

CREATE TRIGGER trg_artists_user_must_be_artist_before_update
BEFORE UPDATE ON artists
FOR EACH ROW
BEGIN
    IF NEW.user_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM users
           WHERE id = NEW.user_id
             AND role = 'ARTIST'
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Artist profile user_id must reference an ARTIST user';
    END IF;
END//

CREATE TRIGGER trg_users_artist_link_role_before_update
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    IF OLD.role = 'ARTIST'
       AND NEW.role <> 'ARTIST'
       AND EXISTS (
           SELECT 1
           FROM artists
           WHERE user_id = OLD.id
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Linked Artist accounts must keep the ARTIST role';
    END IF;
END//

DELIMITER ;

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

-- =====================================================
-- 10. YEU CAU TRO THANH NGHE SI (ARTIST REQUEST)
-- =====================================================
-- User gui yeu cau -> PENDING. Admin duyet -> APPROVED (nang role + tao artist)
-- hoac tu choi -> REJECTED. Moi user chi co 1 yeu cau PENDING tai mot thoi diem
-- (rang buoc o tang service).
CREATE TABLE artist_requests (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    artist_name  VARCHAR(200) NOT NULL,
    slug         VARCHAR(220) NOT NULL,
    bio          TEXT,
    image_url    VARCHAR(500),
    status       VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    review_note  VARCHAR(500),                       -- ly do tu choi (tuy chon)
    reviewed_by  INT NULL,                           -- admin da xu ly
    reviewed_at  DATETIME(6) NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ar_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_ar_user   ON artist_requests(user_id, created_at DESC);
CREATE INDEX idx_ar_status ON artist_requests(status, created_at);

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
-- Tai khoan ARTIST mau (password = Admin@123456)
-- -----------------------------------------------------
INSERT INTO users (username, email, password_hash, display_name, role, status) VALUES
('lena', 'lena@melodyhub.local', '$2a$12$OH.JXdKvdtxZ6SC5JY908uJzOmylnn/6vUIrQKvY3ZGB8jvqWV9hS', 'Lena Rivers', 'ARTIST', 'ACTIVE'),
('eli',  'eli@melodyhub.local',  '$2a$12$OH.JXdKvdtxZ6SC5JY908uJzOmylnn/6vUIrQKvY3ZGB8jvqWV9hS', 'Eli Vale',    'ARTIST', 'ACTIVE');

-- Ho so nghe si (user_id lay theo username de khong phu thuoc AUTO_INCREMENT)
INSERT INTO artists (user_id, name, slug, bio, image_url) VALUES
((SELECT id FROM users WHERE username = 'lena'),
 'Lena Rivers', 'lena-rivers',
 'Lena Rivers uon alternative R&B quanh synth dem, day dan song va giong hat mong.',
 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=600&q=85'),
((SELECT id FROM users WHERE username = 'eli'),
 'Eli Vale', 'eli-vale',
 'Eli Vale mang mau indie pop tuoi sang, giai dieu de nghe.',
 'https://images.unsplash.com/photo-1521337581100-8ca9a73a5f79?auto=format&fit=crop&w=600&q=85');

-- -----------------------------------------------------
-- 10 bai hat mau (audio dung file mp3 cong khai cua SoundHelix)
-- -----------------------------------------------------
INSERT INTO songs (title, slug, duration_sec, file_path, cover_url, status, play_count) VALUES
('Velvet Hours',  'velvet-hours',  238, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',  'https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 84291),
('Afterglow',     'afterglow',     214, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',  'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 61830),
('Slow Motion',   'slow-motion',   201, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',  'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 44105),
('No Signal',     'no-signal',     189, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',  'https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 37826),
('Open Water',    'open-water',    246, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',  'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 29313),
('Midnight Bloom','midnight-bloom',220, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',  'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 25110),
('Paper Moons',   'paper-moons',   226, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',  'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 22108),
('Sunroom',       'sunroom',       196, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',  'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 19912),
('Frequency',     'frequency',     232, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',  'https://images.unsplash.com/photo-1524650359799-842906ca1c06?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 16709),
('Low Light',     'low-light',     208, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=500&q=85', 'PUBLISHED', 12044);

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
