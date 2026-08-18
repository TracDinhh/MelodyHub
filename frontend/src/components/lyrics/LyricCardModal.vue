<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { Download, Loader2, Share2, X } from '@lucide/vue';
import { ASPECTS, renderLyricCard } from '../../utils/lyricCard';

const props = defineProps({
  open: Boolean,
  lines: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  artist: { type: String, default: '' },
  coverUrl: { type: String, default: '' }
});
const emit = defineEmits(['close']);

const aspect = ref('9:16');
const theme = ref('cover');
const rendering = ref(false);
const error = ref('');
const previewUrl = ref('');
const canShare = ref(false);
let currentBlob = null;
let renderRequestId = 0;

const aspectOptions = Object.keys(ASPECTS);

const fileName = computed(() => {
  const base = (props.title || 'melodyhub-lyric')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
  return `melodyhub-${base || 'lyric'}.png`;
});

function revokePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
    previewUrl.value = '';
  }
}

function supportsFileShare(blob) {
  if (
    !blob
    || typeof navigator === 'undefined'
    || typeof navigator.share !== 'function'
    || typeof navigator.canShare !== 'function'
  ) {
    return false;
  }
  const file = new File([blob], fileName.value, { type: 'image/png' });
  return navigator.canShare({ files: [file] });
}

async function renderPreview() {
  if (!props.lines.length) return;
  const requestId = ++renderRequestId;
  rendering.value = true;
  error.value = '';
  try {
    const blob = await renderLyricCard({
      lines: props.lines,
      title: props.title,
      artist: props.artist,
      coverUrl: props.coverUrl,
      aspect: aspect.value,
      theme: theme.value
    });
    if (requestId !== renderRequestId || !props.open) return;
    revokePreview();
    currentBlob = blob;
    canShare.value = supportsFileShare(blob);
    previewUrl.value = URL.createObjectURL(blob);
  } catch {
    if (requestId !== renderRequestId) return;
    error.value = 'Could not build the lyric card. Please try again.';
    currentBlob = null;
    canShare.value = false;
  } finally {
    if (requestId === renderRequestId) rendering.value = false;
  }
}

function download() {
  if (!currentBlob) return;
  const link = document.createElement('a');
  link.href = URL.createObjectURL(currentBlob);
  link.download = fileName.value;
  link.click();
  URL.revokeObjectURL(link.href);
}

async function share() {
  if (!currentBlob || !canShare.value) return;
  const file = new File([currentBlob], fileName.value, { type: 'image/png' });
  try {
    await navigator.share({ files: [file], title: props.title, text: props.lines.join(' / ') });
  } catch {
    // User cancelled or share failed — nothing to recover.
  }
}

function onKeydown(event) {
  if (event.key === 'Escape') emit('close');
}

// Rebuild whenever the modal opens or the user changes a setting.
watch(
  () => [props.open, props.lines.join('\n'), aspect.value, theme.value],
  ([open]) => {
    if (open) void renderPreview();
  }
);

watch(
  () => props.open,
  (open) => {
    document.body.classList.toggle('modal-open', open);
    if (open) {
      window.addEventListener('keydown', onKeydown);
    } else {
      renderRequestId += 1;
      window.removeEventListener('keydown', onKeydown);
      revokePreview();
      currentBlob = null;
      canShare.value = false;
      error.value = '';
      rendering.value = false;
    }
  }
);

onBeforeUnmount(() => {
  document.body.classList.remove('modal-open');
  window.removeEventListener('keydown', onKeydown);
  revokePreview();
});
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-[90] grid place-items-center bg-black/75 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="Create lyric card"
      @mousedown.self="emit('close')"
    >
      <section class="flex max-h-[calc(100vh-2rem)] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-white/10 bg-[#151515] shadow-2xl sm:flex-row">
        <!-- Preview -->
        <div class="grid flex-1 place-items-center bg-black/40 p-6">
          <div
            class="relative w-full overflow-hidden rounded-xl bg-black/60 ring-1 ring-white/10"
            :class="aspect === '9:16' ? 'max-w-[240px] aspect-[9/16]' : 'max-w-[320px] aspect-square'"
          >
            <img v-if="previewUrl" :src="previewUrl" alt="Lyric card preview" class="h-full w-full object-contain" />
            <div v-if="rendering" class="absolute inset-0 grid place-items-center bg-black/50">
              <Loader2 :size="26" class="animate-spin text-[#20E878]" />
            </div>
          </div>
        </div>

        <!-- Controls -->
        <div class="flex w-full flex-col gap-5 border-t border-white/5 p-6 sm:w-72 sm:border-l sm:border-t-0">
          <div class="flex items-center justify-between">
            <h2 class="text-sm font-black text-white">Create lyric card</h2>
            <button type="button" class="melodyhub-icon-btn" title="Close" @click="emit('close')">
              <X :size="18" />
            </button>
          </div>

          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-widest text-[#A1A1AA]">Format</p>
            <div class="grid grid-cols-2 gap-2">
              <button
                v-for="option in aspectOptions"
                :key="option"
                class="h-9 rounded-lg border text-xs font-bold transition"
                :class="aspect === option ? 'border-[#20E878] bg-[#20E878]/10 text-[#20E878]' : 'border-white/10 text-[#A1A1AA] hover:border-white/25'"
                @click="aspect = option"
              >
                {{ option === '9:16' ? 'Story 9:16' : 'Square 1:1' }}
              </button>
            </div>
          </div>

          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-widest text-[#A1A1AA]">Background</p>
            <div class="grid grid-cols-2 gap-2">
              <button
                class="h-9 rounded-lg border text-xs font-bold transition"
                :class="theme === 'cover' ? 'border-[#20E878] bg-[#20E878]/10 text-[#20E878]' : 'border-white/10 text-[#A1A1AA] hover:border-white/25'"
                @click="theme = 'cover'"
              >
                Cover art
              </button>
              <button
                class="h-9 rounded-lg border text-xs font-bold transition"
                :class="theme === 'gradient' ? 'border-[#20E878] bg-[#20E878]/10 text-[#20E878]' : 'border-white/10 text-[#A1A1AA] hover:border-white/25'"
                @click="theme = 'gradient'"
              >
                Gradient
              </button>
            </div>
          </div>

          <p v-if="error" class="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs text-red-200">{{ error }}</p>

          <div class="mt-auto flex flex-col gap-2">
            <button
              class="inline-flex h-11 items-center justify-center gap-2 rounded-full bg-[#20E878] text-sm font-bold text-[#09090B] transition hover:bg-[#64F4A1] disabled:opacity-50"
              :disabled="rendering || !previewUrl"
              @click="download"
            >
              <Download :size="16" /> Download PNG
            </button>
            <button
              v-if="canShare"
              class="inline-flex h-11 items-center justify-center gap-2 rounded-full border border-white/15 text-sm font-bold text-[#F4FFF7] transition hover:border-white/30 disabled:opacity-50"
              :disabled="rendering || !previewUrl"
              @click="share"
            >
              <Share2 :size="16" /> Share
            </button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>
