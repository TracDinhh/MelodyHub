<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import { Play, Pause, Plus, Trash2, ListMusic, Check, AlertTriangle, X } from '@lucide/vue';
import {
  convertLegacyLyricsTime,
  formatLyricsTime,
  looksLikeLegacyLyricsTimes,
  parseLyricsTime
} from '../../../utils/lyricsTime';
import { lyricsService } from '../../../services/lyricsService';

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  lyricsType: {
    type: String,
    default: 'PLAIN' // 'PLAIN' or 'SYNCED'
  },
  audioPreviewUrl: {
    type: String,
    default: ''
  },
  /** Song title — needed for auto-fetch. */
  songTitle: {
    type: String,
    default: ''
  },
  /** Artist name — needed for auto-fetch. */
  songArtist: {
    type: String,
    default: ''
  },
  /** Album name — optional for auto-fetch. */
  songAlbum: {
    type: String,
    default: ''
  },
  /** Duration in seconds — optional for auto-fetch. */
  songDuration: {
    type: Number,
    default: 0
  }
});

const emit = defineEmits(['update:modelValue', 'update:lyricsType']);

// Audio player for preview
const audioPreview = ref(null);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);

// Parse lyrics from JSON
const lines = ref([]);

// --- Auto-fetch lyrics state ---
const isFetching = ref(false);
const fetchResult = ref(null);  // LyricsLookupResponse from backend
const fetchError = ref('');
const selectedCandidate = ref(null);
const showConfirmReplace = ref(false);

const canAutoFetch = computed(() =>
  !!(props.songTitle && props.songTitle.trim() && props.songArtist && props.songArtist.trim())
);

const autoFetchDisabledReason = computed(() => {
  if (!props.songTitle || !props.songTitle.trim()) return 'Enter song title first.';
  if (!props.songArtist || !props.songArtist.trim()) return 'Enter artist name first.';
  return '';
});

const hasExistingLyrics = computed(() => {
  if (props.lyricsType === 'SYNCED') return lines.value.some(l => l.text.trim());
  return !!(props.modelValue && props.modelValue.trim());
});

// Tracks the exact JSON we last emitted so we can ignore the echo coming back
// through modelValue (otherwise every SET/keystroke rebuilds `lines` and the
// timestamps appear to jump/reset).
let lastEmitted = null;

// Parse incoming lyrics into editable lines — but only when the value did NOT
// originate from this editor.
watch(() => props.modelValue, (val) => {
  if (val === lastEmitted) return; // our own echo, keep local editing state
  if (val && props.lyricsType === 'SYNCED') {
    try {
      const parsed = JSON.parse(val);
      if (parsed.lines && Array.isArray(parsed.lines)) {
        const parsedLines = parsed.lines.map((l) => ({
          startTime: l.startTime || 0,
          endTime: l.endTime || 0,
          text: l.text || ''
        }));
        const legacyTimes = looksLikeLegacyLyricsTimes(parsedLines);
        lines.value = parsedLines.map((line) => legacyTimes
          ? {
              ...line,
              startTime: convertLegacyLyricsTime(line.startTime),
              endTime: convertLegacyLyricsTime(line.endTime)
            }
          : line
        );
      }
    } catch {
      lines.value = [];
    }
  }
}, { immediate: true });

function emitSynced() {
  const payload = JSON.stringify({
    lines: lines.value.filter((l) => l.text.trim()),
    language: 'en'
  });
  lastEmitted = payload;
  emit('update:modelValue', payload);
}

// Debounce the deep-watch emit so rapid typing doesn't rebuild + re-serialize
// the whole payload on every keystroke.
let emitTimer = null;
function scheduleEmitSynced() {
  if (emitTimer) clearTimeout(emitTimer);
  emitTimer = setTimeout(() => {
    emitTimer = null;
    emitSynced();
  }, 300);
}

// Sync lines back to modelValue
watch(lines, () => {
  if (props.lyricsType === 'SYNCED') {
    scheduleEmitSynced();
  }
}, { deep: true });

// Audio preview
function togglePlayPause() {
  const el = audioPreview.value;
  if (!el || !props.audioPreviewUrl) return; // no audio chosen yet
  if (el.paused) {
    el.play().catch(() => {});
  } else {
    el.pause();
  }
}

function onTimeUpdate() {
  if (audioPreview.value) {
    currentTime.value = audioPreview.value.currentTime;
  }
}

function onLoadedMetadata() {
  if (audioPreview.value && Number.isFinite(audioPreview.value.duration)) {
    duration.value = audioPreview.value.duration;
  }
}

function seek(event) {
  const time = Number(event.target.value);
  currentTime.value = time;
  if (audioPreview.value) {
    audioPreview.value.currentTime = time;
  }
}

function onAudioEnded() {
  isPlaying.value = false;
  currentTime.value = 0;
}

// Line management
function addLine() {
  lines.value.push({ startTime: 0, endTime: 0, text: '' });
}

function removeLine(index) {
  lines.value.splice(index, 1);
}

function captureTime(index) {
  const line = lines.value[index];
  if (!line) return;
  // Capture the current playback time for THIS line only.
  line.startTime = parseFloat(currentTime.value.toFixed(1));
  if (line.endTime <= line.startTime) {
    line.endTime = parseFloat((line.startTime + 3.5).toFixed(1));
  }
}

function updateLineTime(line, field, event) {
  const parsed = parseLyricsTime(event.target.value);
  if (parsed !== null) {
    line[field] = Number(parsed.toFixed(1));
  }
  event.target.value = formatLyricsTime(line[field]);
}

// Format time for display
function formatTime(seconds) {
  if (!seconds && seconds !== 0) return '0:00.0';
  const m = Math.floor(seconds / 60);
  const s = (seconds % 60).toFixed(1);
  return `${m}:${String(s).padStart(4, '0')}`;
}

function formatDurationDisplay(sec) {
  if (!sec) return '';
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

// Current active line index based on currentTime
const activeLineIndex = computed(() => {
  if (props.lyricsType !== 'SYNCED') return -1;
  for (let i = lines.value.length - 1; i >= 0; i--) {
    const line = lines.value[i];
    const startsAt = Number(line.startTime || 0);
    const endsAt = Number(line.endTime || 0);
    if (currentTime.value >= startsAt && (!endsAt || currentTime.value < endsAt)) {
      return i;
    }
  }
  return -1;
});

function flushSynced() {
  if (emitTimer) {
    clearTimeout(emitTimer);
    emitTimer = null;
  }
  emitSynced();
}

function toggleLyricsType() {
  const newType = props.lyricsType === 'PLAIN' ? 'SYNCED' : 'PLAIN';

  if (newType === 'SYNCED') {
    // Seed synced lines from any existing plain text (each line -> a lyric line).
    if (lines.value.length === 0) {
      const plain = (props.modelValue || '').trim();
      if (plain && !plain.startsWith('{')) {
        lines.value = plain
          .split('\n')
          .map((text) => ({ startTime: 0, endTime: 0, text }));
      } else {
        lines.value = [{ startTime: 0, endTime: 0, text: '' }];
      }
    }
    emit('update:lyricsType', newType);
    flushSynced();
  } else {
    // SYNCED -> PLAIN: convert lines back to plain text so the textarea is clean.
    if (emitTimer) {
      clearTimeout(emitTimer);
      emitTimer = null;
    }
    const plain = lines.value
      .filter((l) => l.text.trim())
      .map((l) => l.text)
      .join('\n');
    emit('update:lyricsType', newType);
    emit('update:modelValue', plain);
  }
}

// --- Auto-fetch lyrics ---
async function autoFetchLyrics() {
  if (isFetching.value || !canAutoFetch.value) return;

  isFetching.value = true;
  fetchError.value = '';
  fetchResult.value = null;
  selectedCandidate.value = null;

  try {
    const result = await lyricsService.searchLyrics({
      title: props.songTitle.trim(),
      artist: props.songArtist.trim(),
      album: props.songAlbum ? props.songAlbum.trim() : undefined,
      duration: props.songDuration || undefined
    });
    fetchResult.value = result;

    // Auto-select if single high-confidence result
    if (result.found && result.candidates?.length === 1 && result.candidates[0].score >= 90) {
      selectedCandidate.value = result.candidates[0];
    }
  } catch (err) {
    const code = err?.code;
    if (code === 'INVALID_LYRICS_SEARCH') {
      fetchError.value = err.message || 'Invalid search parameters.';
    } else if (code === 'NETWORK_ERROR') {
      fetchError.value = 'Unable to reach the server. Check your connection.';
    } else {
      fetchError.value = err.message || 'An error occurred while searching for lyrics.';
    }
  } finally {
    isFetching.value = false;
  }
}

function selectCandidate(candidate) {
  selectedCandidate.value = candidate;
}

function applySelectedLyrics() {
  if (!selectedCandidate.value) return;

  if (hasExistingLyrics.value) {
    showConfirmReplace.value = true;
    return;
  }

  doApplyLyrics(selectedCandidate.value);
}

function confirmReplace() {
  showConfirmReplace.value = false;
  if (selectedCandidate.value) {
    doApplyLyrics(selectedCandidate.value);
  }
}

function cancelReplace() {
  showConfirmReplace.value = false;
}

function doApplyLyrics(candidate) {
  if (candidate.lyricsType === 'SYNCED' && candidate.lyrics?.lines?.length) {
    // Apply synced lyrics
    lines.value = candidate.lyrics.lines.map(l => ({
      startTime: l.startTime,
      endTime: l.endTime,
      text: l.text
    }));
    emit('update:lyricsType', 'SYNCED');
    flushSynced();
  } else if (candidate.lyricsType === 'PLAIN' && candidate.plainLyrics) {
    // Apply plain lyrics
    emit('update:lyricsType', 'PLAIN');
    emit('update:modelValue', candidate.plainLyrics);
    // Also seed lines for potential synced switch
    lines.value = [];
  }

  // Clear the result panel after applying
  fetchResult.value = null;
  selectedCandidate.value = null;
}

function dismissResults() {
  fetchResult.value = null;
  fetchError.value = '';
  selectedCandidate.value = null;
}

function providerErrorMessage(response) {
  if (!response) return '';
  const code = response.errorCode;
  if (code === 'RATE_LIMITED') return 'Lyrics provider is temporarily rate limited. Try again later.';
  if (code === 'PROVIDER_UNAVAILABLE') return 'Lyrics provider is currently unavailable. Try again later.';
  return 'No synced lyrics were found. You can continue manually.';
}

onUnmounted(() => {
  if (emitTimer) {
    clearTimeout(emitTimer);
    emitTimer = null;
    emitSynced();
  }
  if (audioPreview.value) {
    audioPreview.value.pause();
  }
});
</script>

<template>
  <div class="space-y-4">
    <!-- Lyrics Type Toggle -->
    <div class="flex items-center gap-3">
      <button
        type="button"
        @click="toggleLyricsType"
        class="inline-flex items-center gap-2 rounded-lg border border-white/20 bg-white/[0.03] px-3 py-2 text-xs font-bold transition hover:border-[#16C65A]/70 hover:text-[#16C65A]"
      >
        <ListMusic :size="14" />
        {{ lyricsType === 'PLAIN' ? 'Switch to Synced Lyrics' : 'Switch to Plain Lyrics' }}
      </button>
      <span class="text-xs text-[#666]">
        {{ lyricsType === 'PLAIN' ? 'Simple text lyrics' : 'Lyrics with timestamps' }}
      </span>
    </div>

    <!-- ✨ Auto Find Lyrics Button -->
    <div class="rounded-lg border border-white/10 bg-gradient-to-r from-[#16C65A]/5 to-transparent p-4">
      <div class="flex flex-wrap items-center gap-3">
        <button
          id="auto-find-lyrics-btn"
          type="button"
          :disabled="!canAutoFetch || isFetching"
          @click="autoFetchLyrics"
          class="inline-flex items-center gap-2 rounded-lg border border-[#16C65A]/40 bg-[#16C65A]/10 px-4 py-2.5 text-sm font-bold text-[#16C65A] transition hover:bg-[#16C65A]/20 hover:border-[#16C65A]/70 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-[#16C65A]/10 disabled:hover:border-[#16C65A]/40"
        >
          {{ isFetching ? 'Finding lyrics...' : '✨ Find Lyrics Automatically' }}
        </button>
        <span v-if="!canAutoFetch" class="text-xs text-[#666]">
          {{ autoFetchDisabledReason }}
        </span>
      </div>

      <!-- Error message -->
      <div v-if="fetchError" class="mt-3 flex items-start gap-2 rounded-md bg-red-500/10 px-3 py-2">
        <AlertTriangle :size="14" class="mt-0.5 shrink-0 text-red-400" />
        <p class="text-xs text-red-300">{{ fetchError }}</p>
        <button type="button" @click="fetchError = ''" class="ml-auto shrink-0 text-red-400 hover:text-red-300">
          <X :size="14" />
        </button>
      </div>

      <!-- Results: Not Found -->
      <div v-if="fetchResult && !fetchResult.found" class="mt-3 flex items-start gap-2 rounded-md bg-yellow-500/10 px-3 py-2">
        <AlertTriangle :size="14" class="mt-0.5 shrink-0 text-yellow-400" />
        <p class="text-xs text-yellow-300">{{ providerErrorMessage(fetchResult) }}</p>
        <button type="button" @click="dismissResults" class="ml-auto shrink-0 text-yellow-400 hover:text-yellow-300">
          <X :size="14" />
        </button>
      </div>

      <!-- Results: Found candidates -->
      <div v-if="fetchResult && fetchResult.found && fetchResult.candidates?.length" class="mt-3 space-y-3">
        <div class="flex items-center gap-2">
          <Check :size="14" class="text-[#16C65A]" />
          <span class="text-xs font-bold text-[#16C65A]">
            {{ fetchResult.candidates.length === 1 ? 'Lyrics found' : `Found ${fetchResult.candidates.length} possible matches` }}
          </span>
          <span class="text-xs text-[#666]">Source: {{ fetchResult.source }}</span>
          <button type="button" @click="dismissResults" class="ml-auto text-[#666] hover:text-white">
            <X :size="14" />
          </button>
        </div>

        <!-- Candidate list -->
        <div class="max-h-[280px] space-y-2 overflow-y-auto">
          <button
            v-for="(candidate, idx) in fetchResult.candidates"
            :key="idx"
            type="button"
            @click="selectCandidate(candidate)"
            class="flex w-full items-start gap-3 rounded-lg border px-3 py-2.5 text-left transition"
            :class="selectedCandidate === candidate
              ? 'border-[#16C65A]/70 bg-[#16C65A]/10'
              : 'border-white/10 bg-white/[0.02] hover:border-white/20 hover:bg-white/[0.04]'"
          >
            <div class="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full border"
              :class="selectedCandidate === candidate ? 'border-[#16C65A] bg-[#16C65A]' : 'border-white/30'"
            >
              <div v-if="selectedCandidate === candidate" class="h-1.5 w-1.5 rounded-full bg-black" />
            </div>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-bold text-white">{{ candidate.trackName }}</p>
              <p class="truncate text-xs text-[#999]">{{ candidate.artistName }}</p>
              <div class="mt-1 flex flex-wrap items-center gap-2">
                <span v-if="candidate.albumName" class="text-xs text-[#666]">{{ candidate.albumName }}</span>
                <span v-if="candidate.durationSec" class="text-xs text-[#666]">{{ formatDurationDisplay(candidate.durationSec) }}</span>
                <span class="rounded-full px-1.5 py-0.5 text-[10px] font-bold"
                  :class="candidate.lyricsType === 'SYNCED'
                    ? 'bg-[#16C65A]/20 text-[#16C65A]'
                    : 'bg-blue-500/20 text-blue-400'"
                >
                  {{ candidate.lyricsType === 'SYNCED' ? '✓ Synced' : 'Plain' }}
                </span>
                <span class="text-xs text-[#666]">Match: {{ candidate.score }}%</span>
              </div>
            </div>
          </button>
        </div>

        <!-- Use selected button -->
        <button
          v-if="selectedCandidate"
          type="button"
          @click="applySelectedLyrics"
          class="inline-flex items-center gap-2 rounded-lg bg-[#16C65A] px-4 py-2 text-xs font-black text-black transition hover:bg-[#22C55E]"
        >
          <Check :size="14" />
          Use selected lyrics
        </button>
      </div>

      <!-- Confirm replace dialog -->
      <div v-if="showConfirmReplace" class="mt-3 rounded-lg border border-yellow-500/40 bg-yellow-500/10 p-3">
        <p class="mb-3 text-xs text-yellow-200">Replace the current lyrics with the selected result?</p>
        <div class="flex gap-2">
          <button
            type="button"
            @click="confirmReplace"
            class="rounded-lg bg-[#16C65A] px-3 py-1.5 text-xs font-bold text-black transition hover:bg-[#22C55E]"
          >
            Replace
          </button>
          <button
            type="button"
            @click="cancelReplace"
            class="rounded-lg border border-white/20 bg-white/5 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-white/10"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>

    <!-- Plain Lyrics Mode -->
    <div v-if="lyricsType === 'PLAIN'">
      <textarea
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        rows="8"
        class="w-full resize-y rounded-lg border border-white/10 bg-black/30 px-3 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-[#555] focus:border-[#16C65A]/70 focus:ring-2 focus:ring-[#16C65A]/10"
        placeholder="Enter plain lyrics here..."
      />
      <p class="mt-1 text-xs text-[#666]">Enter each line of lyrics on a new line.</p>
    </div>

    <!-- Synced Lyrics Mode -->
    <div v-else class="space-y-4">
      <!-- Audio Preview Controls -->
      <div class="flex items-center gap-3 rounded-lg border border-white/10 bg-black/20 p-3">
        <audio
          ref="audioPreview"
          :src="audioPreviewUrl"
          preload="metadata"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onLoadedMetadata"
          @ended="onAudioEnded"
          @pause="isPlaying = false"
          @play="isPlaying = true"
          class="hidden"
        />
        <button
          type="button"
          @click="togglePlayPause"
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#16C65A] text-black transition hover:bg-[#22C55E]"
        >
          <Pause v-if="isPlaying" :size="16" />
          <Play v-else :size="16" class="ml-0.5" />
        </button>
        <input
          type="range"
          min="0"
          :max="duration || 0"
          step="0.1"
          :value="currentTime"
          @input="seek"
          class="h-1.5 flex-1 cursor-pointer accent-[#16C65A]"
          aria-label="Seek preview audio"
          :aria-valuetext="`${formatTime(currentTime)} of ${formatTime(duration)}`"
        />
        <span class="min-w-[92px] text-right text-xs tabular-nums text-[#888]">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </span>
      </div>

      <!-- Lines Editor -->
      <div class="rounded-lg border border-white/10 bg-black/20">
        <!-- Header -->
        <div class="grid grid-cols-[78px_78px_1fr_32px] gap-2 border-b border-white/10 bg-white/[0.02] px-3 py-2 text-xs font-bold text-[#888]">
          <span>Start (MM:SS)</span>
          <span>End (MM:SS)</span>
          <span>Lyrics</span>
          <span></span>
        </div>

        <!-- Lines -->
        <div class="max-h-[400px] overflow-y-auto">
          <div
            v-for="(line, index) in lines"
            :key="index"
            class="grid grid-cols-[78px_78px_1fr_32px] items-center gap-2 border-b border-white/5 px-3 py-2 transition-colors"
            :class="{ 'bg-[#16C65A]/10': activeLineIndex === index }"
          >
            <input
              :value="formatLyricsTime(line.startTime)"
              type="text"
              inputmode="decimal"
              @change="updateLineTime(line, 'startTime', $event)"
              class="w-full rounded border border-white/10 bg-white/5 px-2 py-1 text-xs text-white outline-none transition focus:border-[#16C65A]/50"
              placeholder="00:00.0"
            />
            <input
              :value="formatLyricsTime(line.endTime)"
              type="text"
              inputmode="decimal"
              @change="updateLineTime(line, 'endTime', $event)"
              class="w-full rounded border border-white/10 bg-white/5 px-2 py-1 text-xs text-white outline-none transition focus:border-[#16C65A]/50"
              placeholder="00:00.0"
            />
            <div class="flex items-center gap-2">
              <input
                v-model="line.text"
                class="w-full rounded border border-white/10 bg-white/5 px-2 py-1 text-sm text-white outline-none transition focus:border-[#16C65A]/50"
                placeholder="Lyrics line..."
              />
              <button
                type="button"
                @click="captureTime(index)"
                class="shrink-0 rounded bg-[#16C65A]/20 px-2 py-1 text-xs font-bold text-[#16C65A] transition hover:bg-[#16C65A]/40"
                title="Capture current time"
              >
                SET
              </button>
            </div>
            <button
              type="button"
              @click="removeLine(index)"
              class="flex h-6 w-6 items-center justify-center rounded text-[#666] transition hover:text-red-400"
            >
              <Trash2 :size="14" />
            </button>
          </div>
        </div>

        <!-- Add Line Button -->
        <button
          type="button"
          @click="addLine"
          class="flex w-full items-center justify-center gap-2 py-3 text-xs font-bold text-[#666] transition hover:bg-white/5 hover:text-[#16C65A]"
        >
          <Plus :size="14" />
          Add Line
        </button>
      </div>

      <p class="text-xs text-[#666]">
        💡 Time uses <strong>MM:SS</strong> (for example, 00:37 means 37 seconds). Click <strong>SET</strong> while playing audio to capture it automatically.
      </p>

      <!-- Preview (karaoke — highlights the line that matches current playback time) -->
      <div v-if="lines.length > 0" class="rounded-lg border border-white/10 bg-black/20 p-4">
        <p class="mb-3 text-xs font-bold text-[#888]">Preview</p>
        <div class="space-y-1 text-center">
          <template v-for="(line, index) in lines" :key="index">
            <div
              v-if="line.text.trim()"
              class="py-1 transition-all duration-300"
              :class="activeLineIndex === index
                ? 'text-base font-black text-[#16C65A]'
                : 'text-sm text-[#888]'"
            >
              {{ line.text }}
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
