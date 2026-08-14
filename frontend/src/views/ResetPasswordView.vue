<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Check, Eye, EyeOff, KeyRound } from '@lucide/vue';
import { authService } from '../services/authService';
import logoUrl from '../assets/styles/icons/logo.png';

const router = useRouter();
const route = useRoute();
const token = route.query.token;

const isLoading = ref(false);
const error = ref('');
const success = ref(false);
const passwordVisible = ref(false);

const form = reactive({
  password: '',
  confirmPassword: '',
  errors: {
    password: '',
    confirmPassword: ''
  }
});

const passwordStrength = computed(() => {
  const p = form.password;
  if (!p) return { level: 0, label: '' };
  let score = 0;
  if (p.length >= 6) score++;
  if (p.length >= 8) score++;
  if (/[A-Z]/.test(p)) score++;
  if (/[0-9]/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  if (score <= 2) return { level: 1, label: 'Weak' };
  if (score <= 3) return { level: 2, label: 'Fair' };
  if (score <= 4) return { level: 3, label: 'Good' };
  return { level: 4, label: 'Strong' };
});

const strengthColors = ['', 'bg-red-400', 'bg-yellow-400', 'bg-green-400', 'bg-[#16C65A]'];

function clearErrors() {
  form.errors.password = '';
  form.errors.confirmPassword = '';
  error.value = '';
}

function validate() {
  clearErrors();
  if (form.password.length < 6) {
    form.errors.password = 'Password must be at least 6 characters.';
    return false;
  }
  if (form.confirmPassword !== form.password) {
    form.errors.confirmPassword = 'Passwords do not match.';
    return false;
  }
  return true;
}

async function submit() {
  if (!validate()) return;
  if (!token) {
    error.value = 'Reset token is missing. Please request a new password reset.';
    return;
  }

  isLoading.value = true;
  clearErrors();

  try {
    await authService.resetPassword(token, form.password);
    success.value = true;
  } catch (err) {
    error.value = err.message || 'Failed to reset password. The link may have expired.';
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

    <div class="rounded-xl border border-white/10 bg-[#111827]/90 p-5 shadow-2xl shadow-black/40 backdrop-blur-xl sm:p-7">
      <!-- Back to login -->
      <RouterLink :to="{ name: 'login' }" class="mb-6 inline-flex items-center gap-1.5 text-xs text-[#777] hover:text-white transition">
        <ArrowLeft :size="14" />
        <span>Back to login</span>
      </RouterLink>

      <template v-if="!success">
        <h1 class="mb-2 text-2xl font-black text-white">Set new password</h1>
        <p class="mb-6 text-sm text-[#858585]">Create a strong password for your account.</p>

        <form class="space-y-4" novalidate @submit.prevent="submit">
          <label class="melodyhub-field">
            <span>New password</span>
            <div class="field-inline">
              <KeyRound :size="15" class="shrink-0 text-[#71717A]" />
              <input
                v-model="form.password"
                :type="passwordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="At least 6 characters"
                @input="form.errors.password = ''"
              />
              <button type="button" class="ml-auto shrink-0 text-[#71717A] hover:text-[#F4FFF7]" @click="passwordVisible = !passwordVisible">
                <EyeOff v-if="passwordVisible" :size="16" /><Eye v-else :size="16" />
              </button>
            </div>
            <small v-if="form.errors.password" class="text-red-300">{{ form.errors.password }}</small>
          </label>

          <!-- Password strength indicator -->
          <div v-if="form.password" class="space-y-1.5">
            <div class="flex gap-1">
              <div v-for="i in 4" :key="i" class="h-1 flex-1 rounded-full transition-colors" :class="i <= passwordStrength.level ? strengthColors[passwordStrength.level] : 'bg-white/10'" />
            </div>
            <p class="text-[11px]" :class="{
              'text-red-300': passwordStrength.level <= 1,
              'text-yellow-300': passwordStrength.level === 2,
              'text-green-300': passwordStrength.level >= 3
            }">{{ passwordStrength.label }}</p>
          </div>

          <label class="melodyhub-field">
            <span>Confirm password</span>
            <div class="field-inline">
              <KeyRound :size="15" class="shrink-0 text-[#71717A]" />
              <input
                v-model="form.confirmPassword"
                :type="passwordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="Repeat your password"
                @input="form.errors.confirmPassword = ''"
              />
            </div>
            <small v-if="form.errors.confirmPassword" class="text-red-300">{{ form.errors.confirmPassword }}</small>
          </label>

          <p v-if="error" class="rounded-lg bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

          <button
            type="submit"
            class="h-11 w-full rounded-full bg-[#16C65A] text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isLoading"
          >
            <span v-if="isLoading">RESETTING...</span>
            <span v-else>RESET PASSWORD</span>
          </button>
        </form>
      </template>

      <!-- Success -->
      <template v-else>
        <div class="flex flex-col items-center py-4 text-center">
          <span class="mb-4 grid size-14 place-items-center rounded-full bg-[#16C65A]/10 text-[#16C65A]">
            <Check :size="28" :stroke-width="3" />
          </span>
          <h1 class="mb-2 text-2xl font-black text-white">Password reset!</h1>
          <p class="text-sm text-[#858585]">
            Your password has been updated. You can now sign in with your new password.
          </p>
        </div>

        <div class="mt-6">
          <RouterLink
            :to="{ name: 'login' }"
            class="flex h-11 w-full items-center justify-center rounded-full bg-[#16C65A] text-xs font-black text-black transition hover:bg-[#22C55E]"
          >
            SIGN IN
          </RouterLink>
        </div>
      </template>
    </div>
  </section>
</template>
