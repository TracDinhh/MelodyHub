<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  CheckCircle2,
  ImagePlus,
  LoaderCircle,
  Music2,
  Send,
  ShieldAlert
} from '@lucide/vue';
import { useRouter } from 'vue-router';
import { artistApplicationService } from '../services/artistApplicationService';
import { uploadService } from '../services/uploadService';
import { useAuthStore } from '../stores/auth.store';

const MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const authStore = useAuthStore();
const router = useRouter();

const request = ref(null);
const isLoading = ref(true);
const isSubmitting = ref(false);
const avatarFile = ref(null);
const avatarPreview = ref('');
const error = ref('');

const fieldErrors = reactive({
  artistName: '',
  slug: '',
  avatar: ''
});

const form = reactive({
  artistName: '',
  slug: '',
  bio: ''
});

const status = computed(() => request.value?.status || 'NONE');
// Show the form when there's no request or the previous one was rejected.
const showForm = computed(() => status.value === 'NONE' || status.value === 'REJECTED');
const submittedDate = computed(() =>
  request.value?.createdAt
    ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(
        new Date(request.value.createdAt)
      )
    : ''
);

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

function onArtistNameInput() {
  fieldErrors.artistName = '';
  error.value = '';
  if (!form.slug || form.slug === toSlug(form.artistName.slice(0, -1))) {
    form.slug = toSlug(form.artistName);
  }
}

function clearFieldError(field) {
  fieldErrors[field] = '';
  error.value = '';
}

function validateForm() {
  fieldErrors.artistName = '';
  fieldErrors.slug = '';
  let valid = true;

  if (form.artistName.trim().length < 2) {
    fieldErrors.artistName = 'Artist name must be at least 2 characters.';
    valid = false;
  }
  const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
  if (!form.slug.trim() || !slugPattern.test(form.slug.trim())) {
    fieldErrors.slug = 'Slug must be lowercase letters, numbers, and hyphens (e.g. my-artist-name).';
    valid = false;
  }
  return valid;
}

function selectAvatar(event) {
  const [file] = event.target.files || [];
  avatarFile.value = null;
  avatarPreview.value = '';
  clearFieldError('avatar');

  if (!file) return;
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    fieldErrors.avatar = 'Choose a JPEG, PNG, or WebP image.';
    return;
  }
  if (file.size > MAX_AVATAR_SIZE_BYTES) {
    fieldErrors.avatar = 'Choose an image smaller than 2 MB.';
    return;
  }

  avatarFile.value = file;
  const reader = new FileReader();
  reader.onload = () => {
    avatarPreview.value = reader.result;
  };
  reader.readAsDataURL(file);
}

async function loadRequest() {
  isLoading.value = true;
  error.value = '';
  try {
    request.value = await artistApplicationService.getMyRequest();
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load your request.';
  } finally {
    isLoading.value = false;
  }
}

async function submit() {
  if (!validateForm()) return;

  isSubmitting.value = true;
  error.value = '';

  try {
    let imageUrl = null;
    if (avatarFile.value) {
      const uploaded = await uploadService.uploadImage(avatarFile.value);
      imageUrl = uploaded.imageUrl;
    }

    request.value = await artistApplicationService.submitRequest({
      artistName: form.artistName.trim(),
      slug: form.slug.trim(),
      bio: form.bio.trim() || null,
      imageUrl
    });
  } catch (requestError) {
    const code = requestError.code;
    if (code === 'ARTIST_SLUG_EXISTS') {
      fieldErrors.slug = 'This slug is already taken. Choose a different one.';
    } else if (code === 'INVALID_ARTIST_NAME') {
      fieldErrors.artistName = requestError.message;
    } else if (code === 'INVALID_ARTIST_SLUG') {
      fieldErrors.slug = requestError.message;
    } else if (code === 'INVALID_FILE' || code === 'UPLOAD_FAILED') {
      fieldErrors.avatar = requestError.message || 'Avatar upload failed. Try another image.';
    } else if (code === 'ARTIST_REQUEST_PENDING_EXISTS') {
      error.value = 'You already have a request under review.';
      await loadRequest();
    } else {
      error.value = requestError.message || 'Something went wrong. Please try again.';
    }
  } finally {
    isSubmitting.value = false;
  }
}

async function signInAgain() {
  await authStore.logout();
  router.push({ name: 'login' });
}

onMounted(loadRequest);
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-8 max-w-2xl">
      <p class="sonix-kicker">ARTIST ACCESS</p>
      <h1 class="sonix-section-title">Become an Artist</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">
        Submit your artist details for review. An admin will approve your request before
        your account becomes an Artist.
      </p>
    </div>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#1DB954]" /> Loading
    </div>

    <!-- PENDING -->
    <div v-else-if="status === 'PENDING'" class="max-w-2xl border border-[#1DB954]/20 bg-[#1DB954]/5 p-6">
      <div class="flex items-start gap-4">
        <span class="grid size-11 shrink-0 place-items-center rounded-full bg-[#1DB954]/15 text-[#1DB954]">
          <LoaderCircle :size="21" class="animate-spin" />
        </span>
        <div>
          <p class="text-lg font-black text-white">Request pending</p>
          <p class="mt-2 text-sm leading-6 text-[#aaa]">
            Submitted {{ submittedDate }}. You'll stay a listener until an admin approves
            "{{ request.artistName }}".
          </p>
        </div>
      </div>
    </div>

    <!-- APPROVED -->
    <div v-else-if="status === 'APPROVED'" class="max-w-2xl border border-[#1DB954]/20 bg-[#1DB954]/5 p-6">
      <div class="flex items-start gap-4">
        <span class="grid size-11 shrink-0 place-items-center rounded-full bg-[#1DB954] text-black">
          <CheckCircle2 :size="22" />
        </span>
        <div>
          <p class="text-lg font-black text-white">Request approved</p>
          <p class="mt-2 text-sm leading-6 text-[#aaa]">
            Your account has been upgraded to Artist. Please sign in again to access your
            Artist dashboard.
          </p>
          <button
            class="mt-4 inline-flex h-10 items-center gap-2 rounded-full bg-[#1DB954] px-5 text-xs font-black text-black"
            @click="signInAgain()"
          >
            <Music2 :size="16" /> SIGN IN AGAIN
          </button>
        </div>
      </div>
    </div>

    <template v-else>
      <!-- Previous rejection note -->
      <div
        v-if="status === 'REJECTED'"
        class="mb-6 max-w-2xl border border-red-400/20 bg-red-400/5 p-5"
      >
        <div class="flex items-start gap-3">
          <ShieldAlert :size="20" class="mt-0.5 shrink-0 text-red-300" />
          <div>
            <p class="text-sm font-black text-white">Your previous request was not approved</p>
            <p v-if="request.reviewNote" class="mt-1 text-sm leading-6 text-[#bbb]">
              Reason: {{ request.reviewNote }}
            </p>
            <p class="mt-1 text-xs text-[#888]">You can adjust your details and submit again.</p>
          </div>
        </div>
      </div>

      <!-- FORM -->
      <form
        class="grid max-w-4xl gap-8 lg:grid-cols-[minmax(0,1fr)_220px]"
        novalidate
        @submit.prevent="submit"
      >
        <div class="space-y-5 border border-white/10 bg-[#121212] p-5 sm:p-6">
          <label class="sonix-field">
            <span>Artist name</span>
            <div>
              <Music2 :size="16" />
              <input
                v-model="form.artistName"
                maxlength="200"
                placeholder="Your artist name"
                @input="onArtistNameInput"
              />
            </div>
            <small v-if="fieldErrors.artistName" class="mt-1 block text-red-300">
              {{ fieldErrors.artistName }}
            </small>
          </label>

          <label class="sonix-field">
            <span>
              Slug
              <span class="font-normal text-[#666]">— URL: /artist/{{ form.slug || 'your-slug' }}</span>
            </span>
            <div>
              <span class="select-none text-xs text-[#666]">/</span>
              <input
                v-model="form.slug"
                maxlength="220"
                placeholder="your-artist-slug"
                @input="clearFieldError('slug')"
              />
            </div>
            <small v-if="fieldErrors.slug" class="mt-1 block text-red-300">{{ fieldErrors.slug }}</small>
          </label>

          <label class="sonix-field">
            <span>Bio <span class="font-normal text-[#666]">(optional)</span></span>
            <textarea
              v-model="form.bio"
              maxlength="1500"
              rows="5"
              class="w-full resize-y rounded-lg border border-white/10 bg-black/30 px-3 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-[#555] focus:border-[#1DB954]/70 focus:ring-2 focus:ring-[#1DB954]/10"
              placeholder="Tell listeners about your music."
            />
          </label>

          <p v-if="error" class="rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">
            {{ error }}
          </p>

          <button
            type="submit"
            class="inline-flex h-11 items-center gap-2 rounded-full bg-[#1DB954] px-6 text-xs font-black text-black transition hover:bg-[#20ca5c] disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isSubmitting"
          >
            <LoaderCircle v-if="isSubmitting" :size="16" class="animate-spin" />
            <Send v-else :size="16" />
            {{ isSubmitting ? 'SUBMITTING...' : 'SUBMIT REQUEST' }}
          </button>
        </div>

        <div>
          <p class="mb-3 text-xs font-black text-[#aaa]">
            Avatar <span class="font-normal text-[#666]">(optional)</span>
          </p>
          <label
            class="group flex aspect-square w-full cursor-pointer items-center justify-center overflow-hidden border border-dashed border-white/20 bg-white/[0.03] transition hover:border-[#1DB954]/70"
          >
            <img
              v-if="avatarPreview"
              :src="avatarPreview"
              alt="Selected artist avatar preview"
              class="h-full w-full object-cover"
            />
            <span v-else class="flex flex-col items-center gap-3 text-center text-xs font-bold text-[#777]">
              <ImagePlus :size="28" class="text-[#1DB954]" /> Choose image
            </span>
            <input class="sr-only" type="file" accept="image/*" @change="selectAvatar" />
          </label>
          <p class="mt-3 text-xs leading-5 text-[#777]">PNG, JPG, or WebP up to 2 MB.</p>
          <p v-if="fieldErrors.avatar" class="mt-2 text-xs text-red-300">{{ fieldErrors.avatar }}</p>
        </div>
      </form>
    </template>
  </div>
</template>
