// Maps an API song payload into the shape the player store expects.
// Views differ only in how the artist label is derived, so callers may pass
// an explicit `artist` string; otherwise it is joined from `song.artists`.

export function joinArtistNames(song, separator = ', ') {
  return (song?.artists || [])
    .map((artist) => artist?.name)
    .filter(Boolean)
    .join(separator);
}

export function toPlayerTrack(song, { artist } = {}) {
  return {
    id: song?.id,
    title: song?.title,
    cover: song?.coverUrl,
    artist: artist ?? joinArtistNames(song),
    album: '',
    duration: song?.durationSec || 0,
    audioUrl: song?.audioUrl,
    lyricsType: song?.lyricsType || 'PLAIN',
    slug: song?.slug
  };
}
