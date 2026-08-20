<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { CheckCircle2, LoaderCircle, Mic2, Music2, Search, Send } from '@lucide/vue';
import { artistAccessService } from '../../services/artistAccessService';
import { studioService } from '../../services/studioService';

const RELATIONSHIPS = [
  { value: 'ARTIST', label: 'I am this artist' },
  { value: 'MANAGER', label: 'I manage this artist' },
  { value: 'LABEL', label: 'I represent the label' },
  { value: 'TEAM_MEMBER', label: 'I am a team member' },
  { value: 'OTHER', label: 'Other relationship' }
];

const router = useRouter();

const query = ref('');
const searching = ref(false);
const searched = ref(false);
const results = ref([]);
const error = ref('');

const selected = ref(null);
const relationship = ref('ARTIST');
const message = ref('');
const isSubmitting = ref(false);
const success = ref('');

const hasSearched = computed(() => searched.value && !searching.value);

let debounceTimer = null;
watch(query, (value) => {
  clearTimeout(debounceTimer);
  if (!value.trim()) {
    results.value = [];
    searched.value = false;
    return;
  }
  debounceTimer = setTimeout(() => search(value), 350);
});

async function search(value) {
  const q = value.trim();
  if (!q) return;
  searching.value = true;
  error.value = '';
  try {
    results.value = await studioService.searchArtists(q);
  } catch (requestError) {
    error.value = requestError.message || 'Unable to search artists.';
    results.value = [];
  } finally {
    searching.value = false;
    searched.value = true;
  }
}

function selectArtist(artist) {
  selected.value = artist;
  error.value = '';
}

async function submit() {
  if (!selected.value) return;
  isSubmitting.value = true;
  error.value = '';
  success.value = '';
  try {
    await artistAccessService.submitRequest({
      requestType: 'CLAIM_ARTIST',
      artistId: selected.value.id,
      relationship: relationship.value,
      message: message.value.trim() || null
    });
    success.value = `Claim request submitted for "${selected.value.name}".`;
    setTimeout(() => router.push({ name: 'studio-requests' }), 900);
  } catch (requestError) {
    const code = requestError.code;
    if (code === 'ALREADY_A_MEMBER') {
      success.value = 'You are already a member of this artist.';
      setTimeout(() => router.push({ name: 'studio-entry' }), 900);
    } else if (code === 'CLAIM_REQUEST_ALREADY_PENDING') {
      error.value = 'You already have a pending claim for this artist.';
    } else {
      error.value = requestError.message || 'Something went wrong. Please try again.';
    }
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-5 py-12 pb-20 sm:px-8">
    <div class="mb-8">
      <p class="melodyhub-kicker">ARTIST STUDIO</p>
      <h1 class="melodyhub-section-title">Claim an Artist</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">
        Find your existing artist page in the catalog, then request access to manage it.
      </p>
    </div>

    <!-- Search -->
    <label class="melodyhub-field mb-6">
      <span>Search the artist catalog</span>
      <div class="field-inline">
        <Search :size="16" class="shrink-0 text-[#71717A]" />
        <input v-model="query" maxlength="200" placeholder="Artist name or slug" />
        <LoaderCircle v-if="searching" :size="16" class="shrink-0 animate-spin text-[#16C65A]" />
      </div>
    </label>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>
    <p v-if="success" class="mb-4 rounded-md bg-[#16C65A]/10 px-3 py-2 text-xs text-[#16C65A]" role="status">{{ success }}</p>

    <!-- Results -->
    <div v-if="hasSearched && results.length === 0" class="rounded-md border border-white/10 bg-[#111827] p-5 text-sm text-[#888]">
      No artists match "{{ query }}". You can also
      <RouterLink :to="{ name: 'studio-create' }" class="font-bold text-[#16C65A] hover:underline">create a new artist</RouterLink>.
    </div>

    <ul v-if="results.length > 0" class="mb-8 space-y-2">
      <li v-for="artist in results" :key="artist.id">
        <button
          type="button"
          class="flex w-full items-center gap-4 border border-white/10 bg-[#111827] p-3 text-left transition hover:border-[#16C65A]/40"
          :class="selected?.id === artist.id ? 'border-[#16C65A]/60' : ''"
          @click="selectArtist(artist)"
        >
          <span class="grid size-11 shrink-0 place-items-center overflow-hidden rounded-lg bg-white/[0.04] text-[#16C65A]">
            <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover" />
            <Mic2 v-else :size="20" />
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm font-bold text-white">{{ artist.name }}</span>
            <span class="block text-xs text-[#71717A]">@{{ artist.slug }}</span>
          </span>
          <CheckCircle2 v-if="selected?.id === artist.id" :size="18" class="shrink-0 text-[#16C65A]" />
        </button>
      </li>
    </ul>

    <!-- Claim form -->
    <form v-if="selected" class="space-y-5 border border-white/10 bg-[#111827] p-6" @submit.prevent="submit">
      <div class="flex items-center gap-3">
        <span class="grid size-12 shrink-0 place-items-center overflow-hidden rounded-lg bg-[#16C65A]/15 text-[#16C65A]">
          <img v-if="selected.imageUrl" :src="selected.imageUrl" :alt="selected.name" class="h-full w-full object-cover" />
          <Music2 v-else :size="22" />
        </span>
        <div class="min-w-0">
          <p class="truncate text-lg font-bold text-white">{{ selected.name }}</p>
          <p class="text-xs text-[#71717A]">Claiming this artist</p>
        </div>
      </div>

      <label class="melodyhub-field">
        <span>Your relationship to this artist</span>
        <div class="field-inline">
          <select v-model="relationship" class="w-full bg-transparent text-sm text-white outline-none">
            <option v-for="option in RELATIONSHIPS" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <small class="text-[#555]">Approval grants OWNER access for "I am this artist", MANAGER otherwise.</small>
      </label>

      <label class="melodyhub-field">
        <span>Message to the reviewer <span class="font-normal text-[#555]">(optional)</span></span>
        <textarea
          v-model="message"
          rows="3"
          maxlength="2000"
          class="mt-1 w-full rounded-md border border-white/10 bg-white/[0.04] px-3 py-2.5 text-sm text-white placeholder-[#555] outline-none transition focus:border-[#16C65A]/50"
          placeholder="Anything the admin should know..."
        />
      </label>

      <button
        type="submit"
        class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="isSubmitting"
      >
        <LoaderCircle v-if="isSubmitting" :size="16" class="animate-spin" />
        <Send v-else :size="16" />
        {{ isSubmitting ? 'SUBMITTING...' : 'SUBMIT CLAIM REQUEST' }}
      </button>
    </form>
  </div>
</template>