<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { AudioLines, ImagePlus, LoaderCircle, Music2, UploadCloud } from '@lucide/vue';
import { studioService } from '../../services/studioService';
import { uploadService } from '../../services/uploadService';
import { genreService } from '../../services/genreService';
import { useAuthStore } from '../../stores/auth.store';
import LyricsEditor from './components/LyricsEditor.vue';

const authStore = useAuthStore();

const MAX_COVER_BYTES = 2 * 1024 * 1024;
const MAX_AUDIO_BYTES = 30 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const ALLOWED_AUDIO_TYPES = [
  'audio/mpeg', 'audio/mp3', 'audio/mp4', 'audio/aac', 'audio/wav', 'audio/x-wav', 'audio/ogg', 'audio/flac'
];
const MAX_GENRES = 3;

const route = useRoute();
const router = useRouter();
const artistId = Number(route.params.artistId);

const form = reactive({ title: '', slug: '', lyrics: '' });
const lyricsType = ref('PLAIN');
const coverFile = ref(null);
const coverPreview = ref('');
const audioFile = ref(null);
const audioName = ref('');
const durationSec = ref(0);
const audioPreviewUrl = ref('');
const genres = ref([]);
const selectedGenres = ref([]);
const fieldErrors = reactive({ title: '', slug: '', cover: '', audio: '', genres: '' });

const isSubmitting = ref(false);
const progressText = ref('');
const error = ref('');

function toggleGenre(genre) {
  fieldErrors.genres = '';
  const index = selectedGenres.value.findIndex((id) => id === genre.id);
  if (index >= 0) {
    selectedGenres.value.splice(index, 1);
    return;
  }
  if (selectedGenres.value.length >= MAX_GENRES) {
    fieldErrors.genres = `You can pick up to ${MAX_GENRES} genres.`;
    return;
  }
  selectedGenres.value.push(genre.id);
}

function toSlug(value) {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');
}

function onTitleInput() {
  fieldErrors.title = '';
  error.value = '';
  // Auto-generate slug from title (frontend fills the slug input hidden from user)
  form.slug = toSlug(form.title);
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

function selectAudio(event) {
  const [file] = event.target.files || [];
  audioFile.value = null;
  audioName.value = '';
  durationSec.value = 0;
  if (audioPreviewUrl.value) {
    URL.revokeObjectURL(audioPreviewUrl.value);
    audioPreviewUrl.value = '';
  }
  fieldErrors.audio = '';
  if (!file) return;
  if (!ALLOWED_AUDIO_TYPES.includes(file.type)) {
    fieldErrors.audio = 'Choose an MP3, WAV, AAC, OGG, or FLAC file.';
    return;
  }
  if (file.size > MAX_AUDIO_BYTES) {
    fieldErrors.audio = 'Audio must be 30 MB or less.';
    return;
  }
  audioFile.value = file;
  audioName.value = file.name;

  // Read duration from the file locally.
  const audioEl = document.createElement('audio');
  audioEl.preload = 'metadata';
  audioEl.onloadedmetadata = () => {
    durationSec.value = Number.isFinite(audioEl.duration) ? Math.round(audioEl.duration) : 0;
  };
  audioEl.src = URL.createObjectURL(file);

  // Store preview URL for lyrics editor
  audioPreviewUrl.value = URL.createObjectURL(file);
}

function validate() {
  fieldErrors.title = '';
  fieldErrors.slug = '';
  fieldErrors.audio = '';
  fieldErrors.genres = '';
  let valid = true;

  if (form.title.trim().length < 1) {
    fieldErrors.title = 'Title is required.';
    valid = false;
  }
  const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
  if (!form.slug.trim() || !slugPattern.test(form.slug.trim())) {
    fieldErrors.slug = 'Slug must be lowercase letters, numbers, and hyphens.';
    valid = false;
  }
  if (!audioFile.value) {
    fieldErrors.audio = 'An audio file is required.';
    valid = false;
  }
  if (selectedGenres.value.length < 1) {
    fieldErrors.genres = 'Pick at least one genre.';
    valid = false;
  }
  return valid;
}

async function submit() {
  if (!validate()) return;

  isSubmitting.value = true;
  error.value = '';
  try {
    progressText.value = 'Uploading audio...';
    const audioUpload = await uploadService.uploadAudio(audioFile.value);

    let coverUrl = null;
    if (coverFile.value) {
      progressText.value = 'Uploading cover...';
      const coverUpload = await uploadService.uploadImage(coverFile.value);
      coverUrl = coverUpload.imageUrl;
    }

    progressText.value = 'Saving song...';
    await studioService.createSong(artistId, {
      title: form.title.trim(),
      slug: form.slug.trim(),
      audioUrl: audioUpload.imageUrl,
      coverUrl,
      durationSec: durationSec.value || 0,
      lyrics: form.lyrics.trim() || null,
      lyricsType: lyricsType.value,
      genreIds: selectedGenres.value
    });

    router.push({ name: 'studio-artist-music', params: { artistId } });
  } catch (requestError) {
    const code = requestError.code;
    if (code === 'SONG_SLUG_EXISTS') {
      fieldErrors.slug = 'A song with this slug already exists. Choose a different one.';
    } else if (code === 'INVALID_SONG_TITLE') {
      fieldErrors.title = requestError.message;
    } else if (code === 'INVALID_SONG_SLUG') {
      fieldErrors.slug = requestError.message;
    } else if (code === 'SONG_GENRE_REQUIRED' || code === 'SONG_GENRE_LIMIT_EXCEEDED' || code === 'INVALID_GENRE_IDS') {
      fieldErrors.genres = requestError.message;
    } else if (code === 'INVALID_FILE' || code === 'UPLOAD_FAILED') {
      fieldErrors.audio = requestError.message || 'Upload failed. Try another file.';
    } else {
      error.value = requestError.message || 'Something went wrong. Please try again.';
    }
  } finally {
    isSubmitting.value = false;
    progressText.value = '';
  }
}

async function loadGenres() {
  try {
    genres.value = (await genreService.listGenres())?.items || [];
  } catch {
    genres.value = [];
  }
}

onMounted(loadGenres);

function formatDuration(seconds) {
  if (!seconds) return '';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-8 max-w-2xl">
      <p class="melodyhub-kicker">STUDIO</p>
      <h1 class="melodyhub-section-title">Upload a Song</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">Add your track, cover art, and lyrics. New songs start as drafts.</p>
    </div>

    <form class="grid max-w-4xl gap-8 lg:grid-cols-[minmax(0,1fr)_220px]" novalidate @submit.prevent="submit">
      <div class="space-y-5 border border-white/10 bg-[#111827] p-5 sm:p-6">
        <!-- Audio -->
        <div>
          <p class="mb-2 text-xs font-black text-[#aaa]">Audio file</p>
          <label class="flex cursor-pointer items-center gap-3 rounded-lg border border-dashed border-white/20 bg-white/[0.03] px-4 py-4 transition hover:border-[#16C65A]/70">
            <AudioLines :size="22" class="shrink-0 text-[#16C65A]" />
            <span class="min-w-0 flex-1">
              <span v-if="audioName" class="block truncate text-sm text-white">{{ audioName }}</span>
              <span v-else class="block text-sm text-[#888]">Choose an MP3/WAV/OGG file</span>
              <span v-if="durationSec" class="text-xs text-[#666]">Duration {{ formatDuration(durationSec) }}</span>
            </span>
            <input class="sr-only" type="file" accept="audio/*" @change="selectAudio" />
          </label>
          <p v-if="fieldErrors.audio" class="mt-1 text-xs text-red-300">{{ fieldErrors.audio }}</p>
        </div>

        <label class="melodyhub-field">
          <span>Title</span>
          <div class="field-inline">
            <Music2 :size="16" class="shrink-0 text-[#71717A]" />
            <input v-model="form.title" maxlength="255" placeholder="Song title" @input="onTitleInput" />
          </div>
          <small v-if="fieldErrors.title" class="mt-1 block text-red-300">{{ fieldErrors.title }}</small>
        </label>

        <div>
          <p class="mb-2 text-xs font-black text-[#aaa]">
            Genres <span class="font-normal text-[#666]">(1–{{ MAX_GENRES }})</span>
          </p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="genre in genres"
              :key="genre.id"
              type="button"
              class="rounded-full border px-3 py-1.5 text-xs font-bold transition"
              :class="selectedGenres.includes(genre.id)
                ? 'border-[#16C65A] bg-[#16C65A] text-black'
                : 'border-white/15 text-[#bbb] hover:border-[#16C65A]/70 hover:text-white'"
              @click="toggleGenre(genre)"
            >{{ genre.name }}</button>
          </div>
          <p v-if="fieldErrors.genres" class="mt-1 text-xs text-red-300">{{ fieldErrors.genres }}</p>
        </div>

        <label class="melodyhub-field">
          <span>Lyrics <span class="font-normal text-[#666]">(optional)</span></span>
          <LyricsEditor
            v-model="form.lyrics"
            v-model:lyricsType="lyricsType"
            :audioPreviewUrl="audioPreviewUrl"
            :songTitle="form.title"
            :songArtist="authStore.displayName"
            :songDuration="durationSec"
          />
        </label>

        <p v-if="error" class="rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

        <button
          type="submit"
          class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isSubmitting"
        >
          <LoaderCircle v-if="isSubmitting" :size="16" class="animate-spin" />
          <UploadCloud v-else :size="16" />
          {{ isSubmitting ? (progressText || 'UPLOADING...') : 'SAVE AS DRAFT' }}
        </button>
      </div>

      <!-- Cover -->
      <div>
        <p class="mb-3 text-xs font-black text-[#aaa]">Cover art <span class="font-normal text-[#666]">(optional)</span></p>
        <label class="group flex aspect-square w-full cursor-pointer items-center justify-center overflow-hidden border border-dashed border-white/20 bg-white/[0.03] transition hover:border-[#16C65A]/70">
          <img v-if="coverPreview" :src="coverPreview" alt="Cover preview" class="h-full w-full object-cover" />
          <span v-else class="flex flex-col items-center gap-3 text-center text-xs font-bold text-[#777]">
            <ImagePlus :size="28" class="text-[#16C65A]" /> Choose image
          </span>
          <input class="sr-only" type="file" accept="image/*" @change="selectCover" />
        </label>
        <p class="mt-3 text-xs leading-5 text-[#777]">PNG, JPG, or WebP up to 2 MB.</p>
        <p v-if="fieldErrors.cover" class="mt-2 text-xs text-red-300">{{ fieldErrors.cover }}</p>
      </div>
    </form>
  </div>
</template>