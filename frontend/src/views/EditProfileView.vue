<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  ArrowLeft,
  ImagePlus,
  LoaderCircle,
  Mail,
  Music2,
  Save,
  UserRound,
  X
} from '@lucide/vue';
import { uploadService } from '../services/uploadService';
import { userService } from '../services/userService';
import { useAuthStore } from '../stores/auth.store';

const MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const authStore = useAuthStore();
const router = useRouter();

const form = reactive({
  displayName: '',
  email: '',
  avatarUrl: ''
});

const original = reactive({
  displayName: '',
  email: '',
  avatarUrl: ''
});

const avatarFile = ref(null);
const avatarPreview = ref('');
const isSubmitting = ref(false);
const error = ref('');
const fieldErrors = reactive({
  displayName: '',
  email: '',
  avatar: ''
});

const hasChanges = computed(
  () =>
    form.displayName.trim() !== original.displayName ||
    form.email.trim() !== original.email ||
    avatarFile.value !== null
);

onMounted(() => {
  if (!authStore.user) return;
  form.displayName = authStore.user.displayName || '';
  form.email = authStore.user.email || '';
  form.avatarUrl = authStore.user.avatarUrl || '';
  original.displayName = form.displayName;
  original.email = form.email;
  original.avatarUrl = form.avatarUrl;
  if (form.avatarUrl) avatarPreview.value = form.avatarUrl;
});

function selectAvatar(event) {
  const [file] = event.target.files || [];
  avatarFile.value = null;
  avatarPreview.value = form.avatarUrl || '';
  fieldErrors.avatar = '';
  if (!file) return;
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    fieldErrors.avatar = 'Choose a JPEG, PNG, or WebP image.';
    return;
  }
  if (file.size > MAX_AVATAR_SIZE_BYTES) {
    fieldErrors.avatar = 'Avatar must be 2 MB or less.';
    return;
  }
  avatarFile.value = file;
  const reader = new FileReader();
  reader.onload = () => { avatarPreview.value = reader.result; };
  reader.readAsDataURL(file);
}

function clearAvatar() {
  avatarFile.value = null;
  avatarPreview.value = form.avatarUrl || '';
}

function validate() {
  fieldErrors.displayName = '';
  fieldErrors.email = '';
  let valid = true;

  const displayName = form.displayName.trim();
  if (displayName.length > 100) {
    fieldErrors.displayName = 'Display name must be 100 characters or less.';
    valid = false;
  }
  const emailPattern = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
  if (!form.email.trim() || !emailPattern.test(form.email.trim())) {
    fieldErrors.email = 'Enter a valid email address.';
    valid = false;
  }
  return valid;
}

async function submit() {
  if (!validate()) return;
  if (!hasChanges.value) {
    router.push({ name: 'profile' });
    return;
  }

  isSubmitting.value = true;
  error.value = '';

  try {
    let avatarUrl = form.avatarUrl;
    if (avatarFile.value) {
      const upload = await uploadService.uploadImage(avatarFile.value);
      avatarUrl = upload.imageUrl;
    }

    const updated = await userService.updateMyProfile({
      displayName: form.displayName.trim(),
      email: form.email.trim(),
      avatarUrl: avatarUrl || null
    });

    window.dispatchEvent(new CustomEvent('melodyhub:profile-updated', { detail: updated }));
    router.push({ name: 'profile' });
  } catch (requestError) {
    const code = requestError.code;
    if (code === 'INVALID_DISPLAY_NAME') {
      fieldErrors.displayName = requestError.message;
    } else if (code === 'INVALID_EMAIL') {
      fieldErrors.email = requestError.message;
    } else if (code === 'INVALID_AVATAR_URL') {
      fieldErrors.avatar = requestError.message;
    } else if (code === 'EMAIL_EXISTS') {
      fieldErrors.email = 'This email is already in use by another account.';
    } else if (code === 'INVALID_FILE' || code === 'UPLOAD_FAILED') {
      fieldErrors.avatar = requestError.message || 'Upload failed. Try another file.';
    } else {
      error.value = requestError.message || 'Could not save your profile. Please try again.';
    }
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-5 py-8 pb-12 sm:px-8">
    <button
      class="mb-6 inline-flex items-center gap-2 text-xs font-bold text-[#8b8b8b] hover:text-white"
      @click="router.push({ name: 'profile' })"
    >
      <ArrowLeft :size="14" /> Back to profile
    </button>

    <div class="mb-8 max-w-2xl">
      <p class="melodyhub-kicker">ACCOUNT</p>
      <h1 class="melodyhub-section-title">Edit profile</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">
        Update your display name, email, or avatar. Changes are saved immediately.
      </p>
    </div>

    <form class="grid max-w-4xl gap-8 lg:grid-cols-[minmax(0,1fr)_220px]" novalidate @submit.prevent="submit">
      <div class="space-y-5 border border-white/10 bg-[#111827] p-5 sm:p-6">
        <label class="melodyhub-field">
          <span>Display name</span>
          <div class="field-inline">
            <UserRound :size="16" class="shrink-0 text-[#71717A]" />
            <input
              v-model="form.displayName"
              maxlength="100"
              placeholder="How you appear across MelodyHub"
              @input="fieldErrors.displayName = ''"
            />
          </div>
          <small v-if="fieldErrors.displayName" class="mt-1 block text-red-300">
            {{ fieldErrors.displayName }}
          </small>
        </label>

        <label class="melodyhub-field">
          <span>Email</span>
          <div class="field-inline">
            <Mail :size="16" class="shrink-0 text-[#71717A]" />
            <input
              v-model="form.email"
              type="email"
              maxlength="255"
              autocomplete="email"
              placeholder="you@example.com"
              @input="fieldErrors.email = ''"
            />
          </div>
          <small v-if="fieldErrors.email" class="mt-1 block text-red-300">
            {{ fieldErrors.email }}
          </small>
        </label>

        <p v-if="error" class="rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">
          {{ error }}
        </p>

        <button
          type="submit"
          class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isSubmitting || !hasChanges"
        >
          <LoaderCircle v-if="isSubmitting" :size="16" class="animate-spin" />
          <Save v-else :size="16" />
          {{ isSubmitting ? 'SAVING...' : 'SAVE CHANGES' }}
        </button>
      </div>

      <div>
        <p class="mb-3 text-xs font-black text-[#aaa]">Avatar</p>
        <label class="group relative flex aspect-square w-full cursor-pointer items-center justify-center overflow-hidden rounded-full border border-dashed border-white/20 bg-white/[0.03] transition hover:border-[#16C65A]/70">
          <img v-if="avatarPreview" :src="avatarPreview" alt="Avatar preview" class="h-full w-full object-cover" />
          <span v-else class="flex flex-col items-center gap-3 text-center text-xs font-bold text-[#777]">
            <ImagePlus :size="28" class="text-[#16C65A]" /> Choose image
          </span>
          <input class="sr-only" type="file" accept="image/*" @change="selectAvatar" />
          <button
            v-if="avatarFile"
            type="button"
            class="absolute right-2 top-2 grid size-7 place-items-center rounded-full bg-black/70 text-white hover:bg-black/90"
            title="Discard new image"
            @click.prevent="clearAvatar"
          >
            <X :size="14" />
          </button>
        </label>
        <p class="mt-3 text-center text-xs leading-5 text-[#777]">PNG, JPG, or WebP up to 2 MB.</p>
        <p v-if="fieldErrors.avatar" class="mt-2 text-center text-xs text-red-300">
          {{ fieldErrors.avatar }}
        </p>
        <p v-if="avatarFile" class="mt-2 text-center text-xs text-[#20E878]">
          New image ready to upload
        </p>
      </div>
    </form>
  </div>
</template>
