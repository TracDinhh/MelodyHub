const image = (id, width = 900) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=85`;

export const featuredArtist = {
  id: 1,
  name: 'Lena Rivers',
  slug: 'lena-rivers',
  verified: true,
  genre: 'Alternative R&B',
  monthlyListeners: '8.6M',
  followers: '2.4M',
  hero: image('photo-1493225457124-a3eb161ffa5f', 1800),
  avatar: image('photo-1493225457124-a3eb161ffa5f', 500),
  bio: 'Lena Rivers bends alternative R&B around nocturnal synths, live strings, and close-mic vocals. Her latest record, Afterglow, moves between quiet confession and widescreen release.'
};

export const tracks = [
  {
    id: 1,
    title: 'Velvet Hours',
    artist: 'Lena Rivers',
    featured: 'Milo Grey',
    album: 'Afterglow',
    cover: image('photo-1516280440614-37939bbacd81', 500),
    plays: '84,291,002',
    duration: 238,
    released: '2026',
    lyrics: [
      'Streetlights folding into gold',
      'We let the quiet take control',
      'Your shadow dancing next to mine',
      'We lose the hours, lose the time',
      'Stay with me through velvet hours',
      'While the city closes down'
    ]
  },
  {
    id: 2,
    title: 'Afterglow',
    artist: 'Lena Rivers',
    album: 'Afterglow',
    cover: image('photo-1506157786151-b8491531f063', 500),
    plays: '61,830,422',
    duration: 214,
    released: '2026',
    lyrics: [
      'I found your name in the afterglow',
      'Written where the wild winds blow',
      'All of the colors we used to know',
      'Still burn bright in the afterglow'
    ]
  },
  {
    id: 3,
    title: 'Slow Motion',
    artist: 'Lena Rivers',
    album: 'Glasshouse',
    cover: image('photo-1524368535928-5b5e00ddc76b', 500),
    plays: '44,105,981',
    duration: 201,
    released: '2025',
    lyrics: [
      'Every second falls like rain',
      'Slow motion running through my veins',
      'Turn the noise down, say my name',
      'We can start it all again'
    ]
  },
  {
    id: 4,
    title: 'No Signal',
    artist: 'Lena Rivers',
    featured: 'Juno Park',
    album: 'Glasshouse',
    cover: image('photo-1511379938547-c1f69419868d', 500),
    plays: '37,826,110',
    duration: 189,
    released: '2025',
    lyrics: [
      'No signal under neon skies',
      'Just static blooming in your eyes',
      'We cut the line and disappear',
      'The silence says what we can hear'
    ]
  },
  {
    id: 5,
    title: 'Open Water',
    artist: 'Lena Rivers',
    album: 'Afterglow',
    cover: image('photo-1501386761578-eac5c94b800a', 500),
    plays: '29,313,620',
    duration: 246,
    released: '2026',
    lyrics: [
      'Meet me out in open water',
      'Past the edge of what we know',
      'Every wave becomes an answer',
      'Every tide can take us home'
    ]
  },
  {
    id: 6,
    title: 'Paper Moons',
    artist: 'Eli Vale',
    album: 'Low Light',
    cover: image('photo-1514525253161-7a46d19cd819', 500),
    plays: '22,108,441',
    duration: 226,
    released: '2026',
    lyrics: ['Paper moons above the avenue', 'Every little lie still looks like truth']
  },
  {
    id: 7,
    title: 'Sunroom',
    artist: 'Iris Bloom',
    album: 'Soft Focus',
    cover: image('photo-1470225620780-dba8ba36b745', 500),
    plays: '19,912,083',
    duration: 196,
    released: '2026',
    lyrics: ['Morning on the floor', 'Light through every open door']
  },
  {
    id: 8,
    title: 'Frequency',
    artist: 'North Arcade',
    album: 'Parallel',
    cover: image('photo-1524650359799-842906ca1c06', 500),
    plays: '16,709,870',
    duration: 232,
    released: '2026',
    lyrics: ['Find me on another frequency', 'Somewhere between memory and dream']
  }
];

export const playlists = [
  {
    id: 1,
    title: 'Night Drive',
    subtitle: 'Synthwave, alt R&B and neon pop',
    cover: image('photo-1506157786151-b8491531f063', 600),
    tracks: 42
  },
  {
    id: 2,
    title: 'Fresh Frequencies',
    subtitle: 'The week in independent music',
    cover: image('photo-1524368535928-5b5e00ddc76b', 600),
    tracks: 36
  },
  {
    id: 3,
    title: 'Late Night Jazz',
    subtitle: 'Modern jazz after midnight',
    cover: image('photo-1511192336575-5a79af67a629', 600),
    tracks: 58
  },
  {
    id: 4,
    title: 'Deep Focus',
    subtitle: 'Minimal sound for maximum flow',
    cover: image('photo-1511379938547-c1f69419868d', 600),
    tracks: 64
  }
];

export const artists = [
  featuredArtist,
  {
    id: 2,
    name: 'Eli Vale',
    slug: 'eli-vale',
    genre: 'Indie Pop',
    avatar: image('photo-1521337581100-8ca9a73a5f79', 500)
  },
  {
    id: 3,
    name: 'Iris Bloom',
    slug: 'iris-bloom',
    genre: 'Dream Pop',
    avatar: image('photo-1529139574466-a303027c1d8b', 500)
  },
  {
    id: 4,
    name: 'North Arcade',
    slug: 'north-arcade',
    genre: 'Electronic',
    avatar: image('photo-1501386761578-eac5c94b800a', 500)
  },
  {
    id: 5,
    name: 'Milo Grey',
    slug: 'milo-grey',
    genre: 'Neo Soul',
    avatar: image('photo-1500648767791-00dcc994a43e', 500)
  }
];

export const podcasts = [
  {
    id: 1,
    title: 'The Listening Room',
    host: 'Nadia Chen',
    cover: image('photo-1590602847861-f357a9332bbc', 500),
    length: '48 min'
  },
  {
    id: 2,
    title: 'Signal Path',
    host: 'Marcus Bell',
    cover: image('photo-1478737270239-2f02b77fc618', 500),
    length: '36 min'
  },
  {
    id: 3,
    title: 'Behind the Chorus',
    host: 'Melody Hub Studios',
    cover: image('photo-1589903308904-1010c2294adc', 500),
    length: '54 min'
  }
];
