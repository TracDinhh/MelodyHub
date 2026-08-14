<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ImagePlus, LoaderCircle, Music2, Save } from '@lucide/vue';
import { songService } from '../../services/songService';
import { uploadService } from '../../services/uploadService';
import LyricsEditor from './components/LyricsEditor.vue';

const MAX_COVER_BYTES = 2 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const route = useRoute();
const router = useRouter();
const songId = route.params.id;

const isLoading = ref(true);
const isSaving = ref(false);
const error = ref('');
const fieldErrors = reactive({ title: '', cover: '' });

const form = reactive({ title: '', slug: '', lyrics: '', audioUrl: '' });
const lyricsType = ref('PLAIN');
const currentCover = ref('');
const coverFile = ref(null);
const coverPreview = ref('');

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const song = await songService.getMine(songId);
    form.title = song.title || '';
    form.slug = song.slug || '';
    form.audioUrl = song.audioUrl || '';
    lyricsType.value = song.lyricsType || 'PLAIN';
    currentCover.value = song.coverUrl || '';

    // For synced songs, load the authoritative lines from song_lyrics and rebuild
    // the editor JSON — songs.lyrics alone is not the source of truth.
    if (lyricsType.value === 'SYNCED') {
      try {
        const data = await songService.getSyncedLyrics(song.slug);
        form.lyrics = data?.lines?.length
          ? JSON.stringify({ lines: data.lines, language: 'en' })
          : (song.lyrics || '');
      } catch {
        form.lyrics = song.lyrics || '';
      }
    } else {
      form.lyrics = song.lyrics || '';
    }
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load the song.';
  } finally {
    isLoading.value = false;
  }
}

function selectCover(event) {
  const [file] = event.target.files || [];
  coverFile.value = null;
  coverPreview.value = '';
  fieldErrors.cover = '';
  if (!file) return;
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    fieldErrors.cover = 'Choose a JPEG, PNG, or WebP image.';
    return;
  }
  if (file.size > MAX_COVER_BYTES) {
    fieldErrors.cover = 'Cover must be 2 MB or less.';
    return;
  }
  coverFile.value = file;
  const reader = new FileReader();
  reader.onload = () => { coverPreview.value = reader.result; };
  reader.readAsDataURL(file);
}

async function save() {
  fieldErrors.title = '';
  if (form.title.trim().length < 1) {
    fieldErrors.title = 'Title is required.';
    return;
  }

  isSaving.value = true;
  error.value = '';
  try {
    let coverUrl = currentCover.value || null;
    if (coverFile.value) {
      const uploaded = await uploadService.uploadImage(coverFile.value);
      coverUrl = uploaded.imageUrl;
    }

    // Pass the lyrics payload as-is (JSON for SYNCED, plain text otherwise).
    // The backend stores songs.lyrics AND persists parsed lines into song_lyrics.
    await songService.updateMine(songId, {
      title: form.title.trim(),
      coverUrl,
      lyrics: form.lyrics.trim() || null,
      lyricsType: lyricsType.value
    });

    router.push({ name: 'artist-dashboard' });
  } catch (requestError) {
    const code = requestError.code;
    if (code === 'INVALID_SONG_TITLE') {
      fieldErrors.title = requestError.message;
    } else if (code === 'INVALID_FILE' || code === 'UPLOAD_FAILED' || code === 'INVALID_COVER_URL') {
      fieldErrors.cover = requestError.message || 'Cover upload failed.';
    } else {
      error.value = requestError.message || 'Unable to save changes.';
    }
  } finally {
    isSaving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-8 max-w-2xl">
      <p class="melodyhub-kicker">ARTIST</p>
      <h1 class="melodyhub-section-title">Edit Song</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">Update the title, cover art, or lyrics.</p>
    </div>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading
    </div>

    <form v-else class="grid max-w-4xl gap-8 lg:grid-cols-[minmax(0,1fr)_220px]" novalidate @submit.prevent="save">
      <div class="space-y-5 border border-white/10 bg-[#111827] p-5 sm:p-6">
        <label class="melodyhub-field">
          <span>Title</span>
          <div class="field-inline">
            <Music2 :size="16" class="shrink-0 text-[#71717A]" />
            <input v-model="form.title" maxlength="255" placeholder="Song title" @input="fieldErrors.title = ''" />
          </div>
          <small v-if="fieldErrors.title" class="mt-1 block text-red-300">{{ fieldErrors.title }}</small>
        </label>

        <label class="melodyhub-field">
          <span>Slug <span class="font-normal text-[#666]">(not editable)</span></span>
          <div class="field-inline">
            <span class="select-none text-xs text-[#666]">/</span>
            <input :value="form.slug" disabled class="opacity-60" />
          </div>
        </label>

        <label class="melodyhub-field">
          <span>Lyrics <span class="font-normal text-[#666]">(optional)</span></span>
          <LyricsEditor
            v-model="form.lyrics"
            v-model:lyricsType="lyricsType"
            :audio-preview-url="form.audioUrl"
          />
        </label>

        <p v-if="error" class="rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

        <button
          type="submit"
          class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isSaving"
        >
          <LoaderCircle v-if="isSaving" :size="16" class="animate-spin" />
          <Save v-else :size="16" />
          {{ isSaving ? 'SAVING...' : 'SAVE CHANGES' }}
        </button>
      </div>

      <div>
        <p class="mb-3 text-xs font-black text-[#aaa]">Cover art</p>
        <label class="group flex aspect-square w-full cursor-pointer items-center justify-center overflow-hidden border border-dashed border-white/20 bg-white/[0.03] transition hover:border-[#16C65A]/70">
          <img v-if="coverPreview || currentCover" :src="coverPreview || currentCover" alt="Cover" class="h-full w-full object-cover" />
          <span v-else class="flex flex-col items-center gap-3 text-center text-xs font-bold text-[#777]">
            <ImagePlus :size="28" class="text-[#16C65A]" /> Choose image
          </span>
          <input class="sr-only" type="file" accept="image/*" @change="selectCover" />
        </label>
        <p class="mt-3 text-xs leading-5 text-[#777]">Click to replace. PNG, JPG, WebP up to 2 MB.</p>
        <p v-if="fieldErrors.cover" class="mt-2 text-xs text-red-300">{{ fieldErrors.cover }}</p>
      </div>
    </form>
  </div>
</template>
