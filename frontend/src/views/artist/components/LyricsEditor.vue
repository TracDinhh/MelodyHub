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

// Parse lyrics from JSON
const lines = ref([]);

// Parse existing lyrics when modelValue changes
watch(() => props.modelValue, (val) => {
  if (val && props.lyricsType === 'SYNCED') {
    try {
      const parsed = JSON.parse(val);
      if (parsed.lines && Array.isArray(parsed.lines)) {
        lines.value = parsed.lines.map(l => ({
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

// Update audio source when preview URL changes
watch(() => props.audioPreviewUrl, (url) => {
  if (audioPreview.value && url) {
    audioPreview.value.src = url;
    audioPreview.value.load();
  }
});

// Sync lines back to modelValue
watch(lines, () => {
  if (props.lyricsType === 'SYNCED') {
    const syncedLyrics = {
      lines: lines.value.filter(l => l.text.trim()),
      language: 'en'
    };
    emit('update:modelValue', JSON.stringify(syncedLyrics));
  }
}, { deep: true });

// Audio preview
function togglePlayPause() {
  if (!audioPreview.value) return;
  if (isPlaying.value) {
    audioPreview.value.pause();
    isPlaying.value = false;
  } else {
    audioPreview.value.play();
    isPlaying.value = true;
  }
}

function onTimeUpdate() {
  if (audioPreview.value) {
    currentTime.value = audioPreview.value.currentTime;
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
  if (lines.value[index]) {
    lines.value[index].startTime = parseFloat(currentTime.value.toFixed(1));
    // Auto-continue: set endTime = next line's startTime, or current + 3.5s if last
    if (index === lines.value.length - 1) {
      lines.value[index].endTime = lines.value[index].startTime + 3.5;
    } else if (index < lines.value.length - 1) {
      lines.value[index].endTime = lines.value[index + 1].startTime;
    }
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
  for (let i = lines.value.length - 1; i >= 0; i--) {
    if (currentTime.value >= lines.value[i].startTime) {
      return i;
    }
  }
  return -1;
});

function toggleLyricsType() {
  const newType = props.lyricsType === 'PLAIN' ? 'SYNCED' : 'PLAIN';
  emit('update:lyricsType', newType);
  
  if (newType === 'SYNCED' && lines.value.length === 0) {
    addLine();
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
          @timeupdate="onTimeUpdate"
          @ended="onAudioEnded"
          class="hidden"
        />
        <button
          type="button"
          @click="togglePlayPause"
          class="flex h-8 w-8 items-center justify-center rounded-full bg-[#1DB954] text-black transition hover:bg-[#20ca5c]"
        >
          <Pause v-if="isPlaying" :size="16" />
          <Play v-else :size="16" class="ml-0.5" />
        </button>
        <div class="flex-1">
          <div class="h-1.5 overflow-hidden rounded-full bg-white/10">
            <div
              class="h-full rounded-full bg-[#1DB954] transition-all"
              :style="{ width: audioPreview ? `${(currentTime / audioPreview.duration) * 100 || 0}%` : '0%' }"
            />
          </div>
        </div>
        <span class="min-w-[70px] text-right text-xs text-[#888]">
          {{ formatTime(currentTime) }}
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

      <!-- Preview -->
      <div v-if="lines.length > 0" class="rounded-lg border border-white/10 bg-black/20 p-4">
        <p class="mb-3 text-xs font-bold text-[#888]">Preview</p>
        <div class="space-y-1 text-center">
          <div
            v-for="(line, index) in lines.filter(l => l.text.trim())"
            :key="index"
            class="py-1 text-sm transition-all"
            :class="{
              'text-white': activeLineIndex === lines.findIndex(l => l.text === line.text && l.startTime === line.startTime),
              'text-[#888]': activeLineIndex !== lines.findIndex(l => l.text === line.text && l.startTime === line.startTime)
            }"
          >
            {{ line.text }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
