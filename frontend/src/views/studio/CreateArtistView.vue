<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ImagePlus, LoaderCircle, Music2, Send } from '@lucide/vue';
import { artistAccessService } from '../../services/artistAccessService';
import { uploadService } from '../../services/uploadService';

const MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const router = useRouter();

const form = reactive({
  name: '',
  bio: '',
  message: ''
});
const imageFile = ref(null);
const imagePreview = ref('');
const fieldErrors = reactive({ name: '', image: '' });
const error = ref('');
const isSubmitting = ref(false);
const success = ref('');

function clearFieldError(field) {
  fieldErrors[field] = '';
  error.value = '';
}

function selectImage(event) {
  const [file] = event.target.files || [];
  imageFile.value = null;
  imagePreview.value = '';
  clearFieldError('image');

  if (!file) return;
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    fieldErrors.image = 'Choose a JPEG, PNG, or WebP image.';
    return;
  }
  if (file.size > MAX_AVATAR_SIZE_BYTES) {
    fieldErrors.image = 'Choose an image smaller than 2 MB.';
    return;
  }

  imageFile.value = file;
  const reader = new FileReader();
  reader.onload = () => { imagePreview.value = reader.result; };
  reader.readAsDataURL(file);
}

async function submit() {
  fieldErrors.name = '';
  error.value = '';
  const name = form.name.trim();
  if (name.length < 2) {
    fieldErrors.name = 'Artist name must be at least 2 characters.';
    return;
  }

  isSubmitting.value = true;
  try {
    let requestedImageUrl = null;
    if (imageFile.value) {
      const uploaded = await uploadService.uploadImage(imageFile.value);
      requestedImageUrl = uploaded.imageUrl;
    }

    await artistAccessService.submitRequest({
      requestType: 'CREATE_ARTIST',
      requestedArtistName: name,
      requestedBio: form.bio.trim() || null,
      requestedImageUrl,
      relationship: 'ARTIST',
      message: form.message.trim() || null
    });

    success.value = `Creation request submitted for "${name}".`;
    setTimeout(() => router.push({ name: 'studio-requests' }), 900);
  } catch (requestError) {
    const code = requestError.code;
    if (code === 'ARTIST_NAME_REQUIRED' || code === 'ARTIST_NAME_TOO_LONG') {
      fieldErrors.name = requestError.message;
    } else if (code === 'INVALID_ARTIST_IMAGE_URL' || code === 'INVALID_FILE' || code === 'UPLOAD_FAILED') {
      fieldErrors.image = requestError.message || 'Image upload failed. Try another image.';
    } else {
      error.value = requestError.message || 'Something went wrong. Please try again.';
    }
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="mx-auto w-full max-w-4xl px-5 py-12 pb-20 sm:px-8">
    <div class="mb-8">
      <p class="melodyhub-kicker">ARTIST STUDIO</p>
      <h1 class="melodyhub-section-title">Create a New Artist</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">
        Submit the details for a brand-new artist profile. An admin will review it before
        you get access.
      </p>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>
    <p v-if="success" class="mb-4 rounded-md bg-[#16C65A]/10 px-3 py-2 text-xs text-[#16C65A]" role="status">{{ success }}</p>

    <form class="grid max-w-4xl gap-8 lg:grid-cols-[minmax(0,1fr)_220px]" novalidate @submit.prevent="submit">
      <div class="space-y-5 border border-white/10 bg-[#111827] p-5 sm:p-6">
        <label class="melodyhub-field">
          <span>Artist name</span>
          <div class="field-inline">
            <Music2 :size="16" class="shrink-0 text-[#71717A]" />
            <input
              v-model="form.name"
              maxlength="200"
              placeholder="Your artist name"
              @input="clearFieldError('name')"
            />
          </div>
          <small v-if="fieldErrors.name" class="mt-1 block text-red-300">{{ fieldErrors.name }}</small>
        </label>

        <label class="melodyhub-field">
          <span>Bio <span class="font-normal text-[#555]">(optional)</span></span>
          <textarea
            v-model="form.bio"
            rows="5"
            maxlength="16000"
            class="mt-1 w-full rounded-md border border-white/10 bg-white/[0.04] px-3 py-2.5 text-sm text-white placeholder-[#555] outline-none transition focus:border-[#16C65A]/50"
            placeholder="Tell listeners about your music."
          />
        </label>

        <label class="melodyhub-field">
          <span>Message to the reviewer <span class="font-normal text-[#555]">(optional)</span></span>
          <textarea
            v-model="form.message"
            rows="3"
            maxlength="2000"
            class="mt-1 w-full rounded-md border border-white/10 bg-white/[0.04] px-3 py-2.5 text-sm text-white placeholder-[#555] outline-none transition focus:border-[#16C65A]/50"
            placeholder="Anything the admin should know..."
          />
        </label>

        <div class="rounded-md border border-white/[0.06] bg-white/[0.02] px-4 py-3 text-xs leading-6 text-[#777]">
          You will be granted <span class="font-bold text-[#16C65A]">OWNER</span> access to this artist
          once an admin approves the request.
        </div>

        <button
          type="submit"
          class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isSubmitting"
        >
          <LoaderCircle v-if="isSubmitting" :size="16" class="animate-spin" />
          <Send v-else :size="16" />
          {{ isSubmitting ? 'SUBMITTING...' : 'SUBMIT REQUEST' }}
        </button>
      </div>

      <div>
        <p class="mb-3 text-xs font-black text-[#aaa]">Image <span class="font-normal text-[#666]">(optional)</span></p>
        <label class="group flex aspect-square w-full cursor-pointer items-center justify-center overflow-hidden border border-dashed border-white/20 bg-white/[0.03] transition hover:border-[#16C65A]/70">
          <img v-if="imagePreview" :src="imagePreview" alt="Artist image preview" class="h-full w-full object-cover" />
          <span v-else class="flex flex-col items-center gap-3 text-center text-xs font-bold text-[#777]">
            <ImagePlus :size="28" class="text-[#16C65A]" /> Choose image
          </span>
          <input class="sr-only" type="file" accept="image/*" @change="selectImage" />
        </label>
        <p class="mt-3 text-xs leading-5 text-[#777]">PNG, JPG, or WebP up to 2 MB.</p>
        <p v-if="fieldErrors.image" class="mt-2 text-xs text-red-300">{{ fieldErrors.image }}</p>
      </div>
    </form>
  </div>
</template>