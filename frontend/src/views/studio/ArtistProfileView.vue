<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { LoaderCircle, Save, UserCircle } from '@lucide/vue';
import { studioService } from '../../services/studioService';

const route = useRoute();
const artistId = Number(route.params.artistId);

const profile = ref(null);
const isLoading = ref(true);
const isSaving = ref(false);
const error = ref('');
const success = ref('');

const form = reactive({
  name: '',
  slug: '',
  bio: '',
  imageUrl: ''
});

async function loadProfile() {
  isLoading.value = true;
  error.value = '';
  try {
    const result = await studioService.getProfile(artistId);
    profile.value = result;
    form.name = result.name || '';
    form.slug = result.slug || '';
    form.bio = result.bio || '';
    form.imageUrl = result.imageUrl || '';
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load profile.';
  } finally {
    isLoading.value = false;
  }
}

async function saveProfile() {
  isSaving.value = true;
  error.value = '';
  success.value = '';
  try {
    const result = await studioService.updateProfile(artistId, {
      name: form.name.trim(),
      slug: form.slug.trim(),
      bio: form.bio.trim() || null,
      imageUrl: form.imageUrl.trim() || null
    });
    profile.value = result;
    success.value = 'Profile updated successfully.';
    setTimeout(() => { success.value = ''; }, 4000);
  } catch (requestError) {
    error.value = requestError.message || 'Unable to update profile.';
  } finally {
    isSaving.value = false;
  }
}

onMounted(loadProfile);
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex items-center gap-3">
      <UserCircle :size="28" class="text-[#16C65A]" />
      <div>
        <p class="melodyhub-kicker">STUDIO</p>
        <h1 class="melodyhub-section-title">Artist Profile</h1>
      </div>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>
    <p v-if="success" class="mb-4 rounded-md bg-[#16C65A]/10 px-3 py-2 text-xs text-[#16C65A]" role="status">{{ success }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading profile
    </div>

    <form v-else class="space-y-5" @submit.prevent="saveProfile">
      <!-- Avatar preview -->
      <div class="flex items-center gap-5">
        <div class="size-20 shrink-0 overflow-hidden rounded-full bg-white/[0.04] ring-2 ring-white/10">
          <img v-if="form.imageUrl" :src="form.imageUrl" alt="Artist avatar" class="h-full w-full object-cover" />
          <span v-else class="grid h-full w-full place-items-center text-[#555]"><UserCircle :size="32" /></span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="text-lg font-bold text-white">{{ form.name || 'Unnamed' }}</p>
          <p class="text-xs text-[#71717A]">@{{ form.slug || '...' }}</p>
        </div>
      </div>

      <!-- Name -->
      <label class="melodyhub-field">
        <span>Artist Name</span>
        <div class="field-inline">
          <input v-model="form.name" maxlength="200" placeholder="Your artist name" />
        </div>
      </label>

      <!-- Slug -->
      <label class="melodyhub-field">
        <span>Slug <span class="font-normal text-[#555]">(URL identifier)</span></span>
        <div class="field-inline">
          <input v-model="form.slug" maxlength="220" placeholder="your-artist-slug" />
        </div>
        <small class="text-[#555]">Used in URLs: /artist/{{ form.slug || '...' }}</small>
      </label>

      <!-- Bio -->
      <label class="melodyhub-field">
        <span>Bio <span class="font-normal text-[#555]">(optional)</span></span>
        <textarea
          v-model="form.bio"
          rows="4"
          maxlength="16000"
          class="mt-1 w-full rounded-md border border-white/10 bg-white/[0.04] px-3 py-2.5 text-sm text-white placeholder-[#555] outline-none transition focus:border-[#16C65A]/50"
          placeholder="Tell listeners about yourself..."
        />
      </label>

      <!-- Image URL -->
      <label class="melodyhub-field">
        <span>Image URL <span class="font-normal text-[#555]">(optional)</span></span>
        <div class="field-inline">
          <input v-model="form.imageUrl" maxlength="500" placeholder="https://..." />
        </div>
      </label>

      <!-- Created at (readonly) -->
      <div v-if="profile?.createdAt" class="text-xs text-[#555]">
        Member since: {{ new Date(profile.createdAt).toLocaleDateString(undefined, { dateStyle: 'medium' }) }}
      </div>

      <button
        type="submit"
        class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-7 text-xs font-black text-black transition hover:bg-[#22C55E] disabled:opacity-50"
        :disabled="isSaving"
      >
        <Save :size="15" />
        <span v-if="isSaving">Saving...</span>
        <span v-else>Save Changes</span>
      </button>
    </form>
  </div>
</template>