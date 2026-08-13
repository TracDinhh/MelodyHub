<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft, Check, Mail } from '@lucide/vue';
import { authService } from '../services/authService';
import logoUrl from '../assets/styles/icons/logo.png';

const router = useRouter();
const step = ref('request'); // 'request' | 'success'
const isLoading = ref(false);
const error = ref('');
const email = reactive({
  value: '',
  error: ''
});

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateEmail() {
  email.error = '';
  if (!email.value.trim()) {
    email.error = 'Enter your email address.';
    return false;
  }
  if (!emailPattern.test(email.value)) {
    email.error = 'Enter a valid email address.';
    return false;
  }
  return true;
}

async function requestReset() {
  if (!validateEmail()) return;

  isLoading.value = true;
  error.value = '';

  try {
    await authService.requestPasswordReset(email.value.trim());
    step.value = 'success';
  } catch (err) {
    error.value = err.message || 'Something went wrong. Please try again.';
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <section class="w-full max-w-md">
    <div class="mb-8 text-center">
      <RouterLink :to="{ name: 'home' }" class="inline-flex items-center">
        <img :src="logoUrl" alt="MelodyHub logo" class="h-20 w-56 object-contain" />
      </RouterLink>
    </div>

    <div class="rounded-xl border border-white/10 bg-[#121212]/90 p-5 shadow-2xl shadow-black/40 backdrop-blur-xl sm:p-7">
      <!-- Back to login -->
      <RouterLink :to="{ name: 'login' }" class="mb-6 inline-flex items-center gap-1.5 text-xs text-[#777] hover:text-white transition">
        <ArrowLeft :size="14" />
        <span>Back to login</span>
      </RouterLink>

      <!-- Request form -->
      <template v-if="step === 'request'">
        <h1 class="mb-2 text-2xl font-black text-white">Forgot password?</h1>
        <p class="mb-6 text-sm text-[#858585]">
          Enter your email and we'll send you a link to reset your password.
        </p>

        <form class="space-y-4" novalidate @submit.prevent="requestReset">
          <label class="melodyhub-field">
            <span>Email</span>
            <div class="field-inline">
              <Mail :size="15" class="shrink-0 text-[#3A4A3E]" />
              <input
                v-model="email.value"
                type="email"
                autocomplete="email"
                placeholder="you@example.com"
                @input="email.error = ''"
              />
            </div>
            <small v-if="email.error" class="text-red-300">{{ email.error }}</small>
          </label>

          <p v-if="error" class="rounded-lg bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

          <button
            type="submit"
            class="h-11 w-full rounded-full bg-[#1DB954] text-xs font-black text-black transition hover:bg-[#20ca5c] disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isLoading"
          >
            <span v-if="isLoading">SENDING...</span>
            <span v-else>SEND RESET LINK</span>
          </button>
        </form>
      </template>

      <!-- Success -->
      <template v-else>
        <div class="flex flex-col items-center py-4 text-center">
          <span class="mb-4 grid size-14 place-items-center rounded-full bg-[#1DB954]/10 text-[#1DB954]">
            <Check :size="28" :stroke-width="3" />
          </span>
          <h1 class="mb-2 text-2xl font-black text-white">Check your email</h1>
          <p class="text-sm text-[#858585]">
            We sent a password reset link to <span class="text-white">{{ email.value }}</span>.
            Check your inbox and follow the instructions.
          </p>
        </div>

        <div class="mt-6 space-y-3">
          <RouterLink
            :to="{ name: 'login' }"
            class="flex h-11 w-full items-center justify-center rounded-full bg-[#252525] text-xs font-black text-white transition hover:bg-[#333] hover:text-white"
          >
            BACK TO LOGIN
          </RouterLink>
        </div>
      </template>
    </div>
  </section>
</template>
