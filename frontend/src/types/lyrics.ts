/**
 * Represents a single line of synced lyrics.
 * startTime and endTime are in seconds (e.g., 4.5 = 4.5 seconds).
 */
export interface SyncedLyricLine {
  startTime: number;
  endTime: number;
  text: string;
}

/**
 * Container for synced lyrics data.
 */
export interface SyncedLyrics {
  lines: SyncedLyricLine[];
  language?: string;
}

/**
 * Lyrics types supported by the system.
 */
export type LyricsType = 'PLAIN' | 'SYNCED';
