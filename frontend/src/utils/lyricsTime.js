export function formatLyricsTime(totalSeconds) {
  const safeSeconds = Number.isFinite(Number(totalSeconds))
    ? Math.max(0, Number(totalSeconds))
    : 0;
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds - minutes * 60;
  return `${String(minutes).padStart(2, '0')}:${seconds.toFixed(1).padStart(4, '0')}`;
}

/**
 * Parses the editor's MM:SS.d format. The old editor encouraged values such
 * as 0.37 for 00:37, so dotted values are accepted as legacy MM.SS input.
 */
export function parseLyricsTime(value) {
  const raw = String(value ?? '').trim();
  if (!raw) return 0;

  const clockMatch = raw.match(/^(\d+):([0-5]?\d(?:\.\d+)?)$/);
  if (clockMatch) {
    return Number(clockMatch[1]) * 60 + Number(clockMatch[2]);
  }

  const legacyMatch = raw.match(/^(\d+)(?:\.(\d{1,2}))?$/);
  if (legacyMatch) {
    const minutes = Number(legacyMatch[1]);
    const seconds = Number((legacyMatch[2] || '').padEnd(2, '0') || 0);
    if (seconds < 60) return minutes * 60 + seconds;
  }

  return null;
}

export function looksLikeLegacyLyricsTimes(lines) {
  const starts = (lines || [])
    .map((line) => Number(line?.startTime))
    .filter((time) => Number.isFinite(time) && time > 0);

  return starts.length >= 2 && Math.max(...starts) <= 1;
}

export function convertLegacyLyricsTime(value) {
  const raw = String(Number(value));
  const [minutesPart, secondsPart = ''] = raw.split('.');
  const seconds = Number(secondsPart.padEnd(2, '0') || 0);
  return Number(minutesPart) * 60 + seconds;
}
