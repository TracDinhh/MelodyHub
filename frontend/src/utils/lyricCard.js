// Renders a shareable "lyric card" image entirely on the client with the Canvas
// API — no backend, no external libraries. Given one or more lyric lines plus
// track metadata, it produces a PNG Blob the caller can download or share.
//
// The cover art is usually cross-origin (ImageKit / SoundHelix). We load it with
// crossOrigin='anonymous' so the canvas is not tainted and can export; if that
// fails (image blocks CORS, or fails to load) we fall back to a brand gradient
// so the feature never breaks.

const BRAND_GREEN = '#20E878';
const BRAND_GREEN_DARK = '#0B7B3E';
const INK = '#F4FFF7';

export const ASPECTS = {
  '9:16': { width: 1080, height: 1920 },
  '1:1': { width: 1080, height: 1080 }
};

export const THEMES = ['cover', 'gradient'];

function loadImage(url) {
  return new Promise((resolve, reject) => {
    if (!url) {
      reject(new Error('No cover URL'));
      return;
    }
    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('Cover image failed to load'));
    image.src = url;
  });
}

// Draws the cover scaled to *cover* the whole canvas (like CSS background-size:
// cover), centered.
function drawCoverFill(ctx, image, width, height) {
  const scale = Math.max(width / image.width, height / image.height);
  const drawWidth = image.width * scale;
  const drawHeight = image.height * scale;
  const dx = (width - drawWidth) / 2;
  const dy = (height - drawHeight) / 2;
  ctx.drawImage(image, dx, dy, drawWidth, drawHeight);
}

function drawGradient(ctx, width, height) {
  const gradient = ctx.createLinearGradient(0, 0, width, height);
  gradient.addColorStop(0, '#12351F');
  gradient.addColorStop(0.5, BRAND_GREEN_DARK);
  gradient.addColorStop(1, '#0A0A0C');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);
}

// Wraps text into lines that fit maxWidth at the given font size, returning the
// wrapped rows. Used by fitText to pick a size that fits vertically too.
function wrapText(ctx, text, maxWidth) {
  const words = String(text).split(/\s+/).filter(Boolean);
  const rows = [];
  let current = '';
  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word;
    if (ctx.measureText(candidate).width > maxWidth && current) {
      rows.push(current);
      current = word;
    } else {
      current = candidate;
    }
  }
  if (current) rows.push(current);
  return rows.length ? rows : [''];
}

// Picks the largest font size (down to a floor) at which the combined lyric text
// wraps within the available box, so long passages shrink instead of overflow.
function fitLyric(ctx, lines, maxWidth, maxHeight, fontFamily, maxFont, minFont) {
  const text = lines.join('\n');
  for (let size = maxFont; size >= minFont; size -= 4) {
    ctx.font = `800 ${size}px ${fontFamily}`;
    const rows = text
      .split('\n')
      .flatMap((paragraph) => wrapText(ctx, paragraph, maxWidth));
    const lineHeight = size * 1.24;
    if (rows.length * lineHeight <= maxHeight) {
      return { size, rows, lineHeight };
    }
  }
  ctx.font = `800 ${minFont}px ${fontFamily}`;
  const rows = text
    .split('\n')
    .flatMap((paragraph) => wrapText(ctx, paragraph, maxWidth));
  return { size: minFont, rows, lineHeight: minFont * 1.24 };
}

/**
 * Renders a lyric card to a PNG Blob.
 *
 * @param {Object} options
 * @param {string[]} options.lines   Lyric line(s) to feature.
 * @param {string}   options.title   Song title.
 * @param {string}   options.artist  Artist name(s).
 * @param {string}   [options.coverUrl] Cover image URL (may be cross-origin).
 * @param {string}   [options.aspect='9:16'] Key of ASPECTS.
 * @param {string}   [options.theme='cover'] 'cover' | 'gradient'.
 * @returns {Promise<Blob>} PNG blob.
 */
export async function renderLyricCard({
  lines,
  title,
  artist,
  coverUrl,
  aspect = '9:16',
  theme = 'cover'
}) {
  const { width, height } = ASPECTS[aspect] || ASPECTS['9:16'];
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  const fontFamily = getComputedStyle(document.body).fontFamily || 'sans-serif';

  // Background: cover art (preferred) or brand gradient fallback.
  let coverImage = null;
  if (coverUrl) {
    try {
      coverImage = await loadImage(coverUrl);
    } catch {
      coverImage = null;
    }
  }
  const usedCover = theme === 'cover' && coverImage;
  if (usedCover) drawCoverFill(ctx, coverImage, width, height);
  if (!usedCover) {
    drawGradient(ctx, width, height);
  } else {
    // Darkening overlay so white text stays legible over any cover.
    const overlay = ctx.createLinearGradient(0, 0, 0, height);
    overlay.addColorStop(0, 'rgba(9,9,11,0.35)');
    overlay.addColorStop(0.55, 'rgba(9,9,11,0.55)');
    overlay.addColorStop(1, 'rgba(9,9,11,0.9)');
    ctx.fillStyle = overlay;
    ctx.fillRect(0, 0, width, height);
  }

  const margin = Math.round(width * 0.09);
  const contentWidth = width - margin * 2;

  // Accent tick above the lyric.
  ctx.fillStyle = BRAND_GREEN;
  ctx.fillRect(margin, Math.round(height * 0.34), Math.round(width * 0.12), 8);

  // Lyric text — the hero. Reserve the middle band for it.
  const lyricTop = Math.round(height * 0.4);
  const lyricMaxHeight = Math.round(height * 0.36);
  const maxFont = aspect === '1:1' ? 88 : 96;
  const { rows, lineHeight } = fitLyric(
    ctx,
    lines,
    contentWidth,
    lyricMaxHeight,
    fontFamily,
    maxFont,
    40
  );
  ctx.fillStyle = INK;
  ctx.textBaseline = 'top';
  ctx.textAlign = 'left';
  ctx.shadowColor = 'rgba(0,0,0,0.45)';
  ctx.shadowBlur = 12;
  ctx.shadowOffsetY = 2;
  let y = lyricTop;
  for (const row of rows) {
    ctx.fillText(row, margin, y);
    y += lineHeight;
  }
  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.shadowOffsetY = 0;

  // Footer meta: cover thumbnail, song title + artist, and the brand mark.
  const footerY = height - margin;
  const thumbnailSize = coverImage ? 112 : 0;
  if (coverImage) {
    const thumbnailX = margin;
    const thumbnailY = footerY - thumbnailSize;
    const scale = Math.max(thumbnailSize / coverImage.width, thumbnailSize / coverImage.height);
    const drawWidth = coverImage.width * scale;
    const drawHeight = coverImage.height * scale;
    ctx.save();
    ctx.beginPath();
    ctx.roundRect(thumbnailX, thumbnailY, thumbnailSize, thumbnailSize, 18);
    ctx.clip();
    ctx.drawImage(
      coverImage,
      thumbnailX + (thumbnailSize - drawWidth) / 2,
      thumbnailY + (thumbnailSize - drawHeight) / 2,
      drawWidth,
      drawHeight
    );
    ctx.restore();
  }

  const metaX = margin + (thumbnailSize ? thumbnailSize + 30 : 0);
  const metaWidth = width - margin - metaX;
  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.font = `800 40px ${fontFamily}`;
  ctx.fillStyle = INK;
  ctx.fillText(truncate(ctx, title || 'Unknown song', metaWidth), metaX, footerY - 52);

  const brandLabel = 'MelodyHub';
  ctx.font = `900 30px ${fontFamily}`;
  const brandWidth = ctx.measureText(brandLabel).width;
  ctx.font = `500 32px ${fontFamily}`;
  ctx.fillStyle = 'rgba(244,255,247,0.7)';
  const artistWidth = Math.max(80, metaWidth - brandWidth - 36);
  ctx.fillText(truncate(ctx, artist || 'Unknown artist', artistWidth), metaX, footerY - 8);

  // Brand mark bottom-right.
  ctx.textAlign = 'right';
  ctx.font = `900 30px ${fontFamily}`;
  ctx.fillStyle = BRAND_GREEN;
  ctx.fillText(brandLabel, width - margin, footerY - 8);

  return await new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob);
      else reject(new Error('Could not export lyric card'));
    }, 'image/png');
  });
}

function truncate(ctx, text, maxWidth) {
  if (ctx.measureText(text).width <= maxWidth) return text;
  let result = text;
  while (result.length > 1 && ctx.measureText(`${result}…`).width > maxWidth) {
    result = result.slice(0, -1);
  }
  return `${result}…`;
}
