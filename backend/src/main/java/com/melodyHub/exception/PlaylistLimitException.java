package com.melodyHub.exception;

public class PlaylistLimitException extends RuntimeException {
    public PlaylistLimitException() {
        super("Free accounts can create up to 3 playlists. Upgrade to Premium for unlimited playlists.");
    }
}
