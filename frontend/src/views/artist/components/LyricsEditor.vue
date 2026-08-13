<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import { Play, Pause, Plus, Trash2, ListMusic } from '@lucide/vue';

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
        lines.value = parsed.lines.map((l) => ({
          startTime: l.startTime || 0,
          endTime: l.endTime || 0,
          text: l.text || ''
        }));
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

// Sync lines back to modelValue
watch(lines, () => {
  if (props.lyricsType === 'SYNCED') {
    emitSynced();
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

// Format time for display
function formatTime(seconds) {
  if (!seconds && seconds !== 0) return '0:00.0';
  const m = Math.floor(seconds / 60);
  const s = (seconds % 60).toFixed(1);
  return `${m}:${String(s).padStart(4, '0')}`;
}

// Parse time string back to seconds
function parseTime(str) {
  const match = str.match(/^(\d+):(\d+\.?\d*)$/);
  if (match) {
    return parseInt(match[1]) * 60 + parseFloat(match[2]);
  }
  return 0;
}

// Current active line index based on currentTime
const activeLineIndex = computed(() => {
  if (props.lyricsType !== 'SYNCED') return -1;
  // Latest line whose (set) start time has been reached. It stays highlighted
  // until the next line starts — standard karaoke behaviour.
  let active = -1;
  for (let i = 0; i < lines.value.length; i++) {
    const line = lines.value[i];
    if (line.startTime > 0 && currentTime.value >= line.startTime) {
      active = i;
    }
  }
  return active;
});

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
    emitSynced();
  } else {
    // SYNCED -> PLAIN: convert lines back to plain text so the textarea is clean.
    const plain = lines.value
      .filter((l) => l.text.trim())
      .map((l) => l.text)
      .join('\n');
    emit('update:lyricsType', newType);
    emit('update:modelValue', plain);
  }
}

onUnmounted(() => {
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
        class="inline-flex items-center gap-2 rounded-lg border border-white/20 bg-white/[0.03] px-3 py-2 text-xs font-bold transition hover:border-[#1DB954]/70 hover:text-[#1DB954]"
      >
        <ListMusic :size="14" />
        {{ lyricsType === 'PLAIN' ? 'Switch to Synced Lyrics' : 'Switch to Plain Lyrics' }}
      </button>
      <span class="text-xs text-[#666]">
        {{ lyricsType === 'PLAIN' ? 'Simple text lyrics' : 'Lyrics with timestamps' }}
      </span>
    </div>

    <!-- Plain Lyrics Mode -->
    <div v-if="lyricsType === 'PLAIN'">
      <textarea
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        rows="8"
        class="w-full resize-y rounded-lg border border-white/10 bg-black/30 px-3 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-[#555] focus:border-[#1DB954]/70 focus:ring-2 focus:ring-[#1DB954]/10"
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
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#1DB954] text-black transition hover:bg-[#20ca5c]"
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
          class="h-1.5 flex-1 cursor-pointer accent-[#1DB954]"
          aria-label="Seek preview audio"
        />
        <span class="min-w-[92px] text-right text-xs tabular-nums text-[#888]">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </span>
      </div>

      <!-- Lines Editor -->
      <div class="rounded-lg border border-white/10 bg-black/20">
        <!-- Header -->
        <div class="grid grid-cols-[60px_60px_1fr_32px] gap-2 border-b border-white/10 bg-white/[0.02] px-3 py-2 text-xs font-bold text-[#888]">
          <span>Start</span>
          <span>End</span>
          <span>Lyrics</span>
          <span></span>
        </div>

        <!-- Lines -->
        <div class="max-h-[400px] overflow-y-auto">
          <div
            v-for="(line, index) in lines"
            :key="index"
            class="grid grid-cols-[60px_60px_1fr_32px] items-center gap-2 border-b border-white/5 px-3 py-2 transition-colors"
            :class="{ 'bg-[#1DB954]/10': activeLineIndex === index }"
          >
            <input
              v-model.number="line.startTime"
              type="number"
              step="0.1"
              min="0"
              class="w-full rounded border border-white/10 bg-white/5 px-2 py-1 text-xs text-white outline-none transition focus:border-[#1DB954]/50"
              placeholder="0.0"
            />
            <input
              v-model.number="line.endTime"
              type="number"
              step="0.1"
              min="0"
              class="w-full rounded border border-white/10 bg-white/5 px-2 py-1 text-xs text-white outline-none transition focus:border-[#1DB954]/50"
              placeholder="0.0"
            />
            <div class="flex items-center gap-2">
              <input
                v-model="line.text"
                class="w-full rounded border border-white/10 bg-white/5 px-2 py-1 text-sm text-white outline-none transition focus:border-[#1DB954]/50"
                placeholder="Lyrics line..."
              />
              <button
                type="button"
                @click="captureTime(index)"
                class="shrink-0 rounded bg-[#1DB954]/20 px-2 py-1 text-xs font-bold text-[#1DB954] transition hover:bg-[#1DB954]/40"
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
          class="flex w-full items-center justify-center gap-2 py-3 text-xs font-bold text-[#666] transition hover:bg-white/5 hover:text-[#1DB954]"
        >
          <Plus :size="14" />
          Add Line
        </button>
      </div>

      <p class="text-xs text-[#666]">
        💡 Click <strong>SET</strong> while playing audio to capture the timestamp for that line.
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
                ? 'text-base font-black text-[#1DB954]'
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
