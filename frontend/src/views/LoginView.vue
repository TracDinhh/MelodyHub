<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { AtSign, Check, Eye, EyeOff, Mail, Music2, UserRound, KeyRound } from '@lucide/vue';
import { useAuthStore } from '../stores/auth.store';
import { usePlayerStore } from '../stores/player.store';
import { canAccessRoute, getRoleHomeRouteName } from '../utils/roleRouting';
import logoUrl from '../assets/styles/icons/logo.png';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const playerStore = usePlayerStore();
const mode = ref(route.query.mode === 'register' ? 'register' : 'login');
const passwordVisible = ref(false);
const confirmPasswordVisible = ref(false);
const loginForm = reactive({
  usernameOrEmail: '',
  password: '',
  rememberMe: false
});
const registerForm = reactive({
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
  terms: false
});
const errors = reactive({
  usernameOrEmail: '',
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
  terms: '',
  general: ''
});
const title = computed(() => (mode.value === 'login' ? 'Welcome back' : 'Create your account'));
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const currentPassword = computed({
  get: () => mode.value === 'login' ? loginForm.password : registerForm.password,
  set: (v) => { if (mode.value === 'login') loginForm.password = v; else registerForm.password = v; }
});

function clearErrors() {
  Object.keys(errors).forEach((key) => {
    errors[key] = '';
  });
}

function setMode(nextMode) {
  mode.value = nextMode;
  passwordVisible.value = false;
  confirmPasswordVisible.value = false;
  clearErrors();
}

function clearFieldError(field) {
  errors[field] = '';
  errors.general = '';
}

function validateLogin() {
  clearErrors();
  if (!loginForm.usernameOrEmail.trim()) {
    errors.usernameOrEmail = 'Enter your username or email.';
  }
  if (!loginForm.password) {
    errors.password = 'Enter your password.';
  }
  return !errors.usernameOrEmail && !errors.password;
}

function validateRegistration() {
  clearErrors();
  const username = registerForm.username.trim();
  const displayName = registerForm.displayName.trim();
  const email = registerForm.email.trim();

  if (username.length < 3 || username.length > 50) {
    errors.username = 'Username must be between 3 and 50 characters.';
  }
  if (displayName.length > 100) {
    errors.displayName = 'Display name must be 100 characters or fewer.';
  }
  if (!email || email.length > 255 || !emailPattern.test(email)) {
    errors.email = 'Enter a valid email address up to 255 characters.';
  }
  if (registerForm.password.length < 6) {
    errors.password = 'Password must be at least 6 characters.';
  }
  if (registerForm.confirmPassword !== registerForm.password) {
    errors.confirmPassword = 'Passwords do not match.';
  }
  if (!registerForm.terms) {
    errors.terms = 'You must accept the Terms of Service.';
  }

  return !Object.entries(errors).some(([key, value]) => key !== 'general' && value);
}

function mapBackendError(error) {
  const code = error.code || 'REQUEST_FAILED';
  const message = error.message || 'Something went wrong. Please try again.';

  if (mode.value === 'login') {
    if (code === 'INVALID_CREDENTIALS') {
      errors.general = 'The username, email, or password is incorrect.';
      return;
    }
    if (code === 'USER_BANNED') {
      errors.general = 'This account has been suspended.';
      return;
    }
  }

  const fieldByCode = {
    USERNAME_EXISTS: 'username',
    INVALID_USERNAME: 'username',
    EMAIL_EXISTS: 'email',
    INVALID_EMAIL: 'email',
    INVALID_PASSWORD: 'password',
    INVALID_DISPLAY_NAME: 'displayName'
  };
  const field = fieldByCode[code];
  if (field) {
    errors[field] = message;
    return;
  }

  errors.general =
    code === 'NETWORK_ERROR'
      ? 'Unable to reach Melody Hub. Check that the backend is running.'
      : message;
}

function safeRedirectTarget(role) {
  const redirect = route.query.redirect;
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return { name: getRoleHomeRouteName(role) };
  }

  const target = router.resolve(redirect);
  if (target.name === 'not-found' || !canAccessRoute(role, target.meta.allowedRoles)) {
    return { name: getRoleHomeRouteName(role) };
  }

  return redirect;
}

async function submit() {
  const isValid = mode.value === 'login' ? validateLogin() : validateRegistration();
  if (!isValid) return;

  try {
    if (mode.value === 'login') {
      await authStore.login({
        usernameOrEmail: loginForm.usernameOrEmail.trim(),
        password: loginForm.password
      }, loginForm.rememberMe);
    } else {
      await authStore.register({
        username: registerForm.username.trim(),
        email: registerForm.email.trim(),
        password: registerForm.password,
        displayName: registerForm.displayName.trim() || null
      });
    }
    // Load this user's liked songs so hearts are correct after login.
    void playerStore.hydrateLikes();
    await router.push(safeRedirectTarget(authStore.user?.role));
  } catch (error) {
    mapBackendError(error);
  }
}

watch(() => loginForm.usernameOrEmail, () => clearFieldError('usernameOrEmail'));
watch(() => loginForm.password, () => clearFieldError('password'));
watch(() => registerForm.username, () => clearFieldError('username'));
watch(() => registerForm.displayName, () => clearFieldError('displayName'));
watch(() => registerForm.email, () => clearFieldError('email'));
watch(() => registerForm.password, () => clearFieldError('password'));
watch(() => registerForm.confirmPassword, () => clearFieldError('confirmPassword'));
watch(() => registerForm.terms, () => clearFieldError('terms'));
</script>

<template>
  <section class="w-full max-w-md">
    <div class="mb-8 text-center">
      <RouterLink :to="{ name: 'home' }" class="inline-flex items-center">
        <img :src="logoUrl" alt="MelodyHub logo" class="h-20 w-56 object-contain" />
      </RouterLink>
      <h1 class="mt-7 text-3xl font-black text-white">{{ title }}</h1>
      <p class="mt-2 text-sm text-[#858585]">Your next favorite sound is one sign-in away.</p>
    </div>

    <div class="rounded-xl border border-white/10 bg-[#111827]/90 p-5 shadow-2xl shadow-black/40 backdrop-blur-xl sm:p-7">
      <div class="mb-6 grid grid-cols-2 rounded-lg bg-black/40 p-1">
        <button type="button" class="h-9 rounded-md text-xs font-black transition" :class="mode === 'login' ? 'bg-[#252525] text-white' : 'text-[#777]'" @click="setMode('login')">LOGIN</button>
        <button type="button" class="h-9 rounded-md text-xs font-black transition" :class="mode === 'register' ? 'bg-[#252525] text-white' : 'text-[#777]'" @click="setMode('register')">REGISTER</button>
      </div>

      <form class="space-y-4" novalidate @submit.prevent="submit">
        <label v-if="mode === 'login'" class="melodyhub-field">
          <span>Username or email</span>
          <div class="field-inline">
            <AtSign :size="15" class="shrink-0 text-[#71717A]" />
            <input
              v-model="loginForm.usernameOrEmail"
              autocomplete="username"
              placeholder="Username or you@example.com"
              @input="clearFieldError('usernameOrEmail')"
            />
          </div>
          <small v-if="errors.usernameOrEmail" class="text-red-300">{{ errors.usernameOrEmail }}</small>
        </label>

        <template v-else>
          <label class="melodyhub-field">
            <span>Username</span>
            <div class="field-inline">
              <UserRound :size="15" class="shrink-0 text-[#71717A]" />
              <input v-model="registerForm.username" autocomplete="username" maxlength="50" placeholder="alexmorgan" />
            </div>
            <small v-if="errors.username" class="text-red-300">{{ errors.username }}</small>
          </label>
          <label class="melodyhub-field">
            <span>Display name <span class="font-normal text-[#555]">(optional)</span></span>
            <div class="field-inline">
              <Music2 :size="15" class="shrink-0 text-[#71717A]" />
              <input v-model="registerForm.displayName" autocomplete="name" maxlength="100" placeholder="Alex Morgan" />
            </div>
            <small v-if="errors.displayName" class="text-red-300">{{ errors.displayName }}</small>
          </label>
          <label class="melodyhub-field">
            <span>Email</span>
            <div class="field-inline">
              <Mail :size="15" class="shrink-0 text-[#71717A]" />
              <input v-model="registerForm.email" type="email" autocomplete="email" maxlength="255" placeholder="you@example.com" />
            </div>
            <small v-if="errors.email" class="text-red-300">{{ errors.email }}</small>
          </label>
        </template>

        <label class="melodyhub-field">
          <span>Password</span>
          <div class="field-inline">
            <Music2 :size="15" class="shrink-0 text-[#71717A]" />
            <input
              v-model="currentPassword"
              :type="passwordVisible ? 'text' : 'password'"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              :placeholder="mode === 'login' ? 'Your password' : 'At least 6 characters'"
            />
            <button type="button" class="ml-auto shrink-0 text-[#71717A] hover:text-[#F4FFF7]" title="Toggle password visibility" @click="passwordVisible = !passwordVisible">
              <EyeOff v-if="passwordVisible" :size="16" /><Eye v-else :size="16" />
            </button>
          </div>
          <small v-if="errors.password" class="text-red-300">{{ errors.password }}</small>
        </label>

        <!-- Remember me & Forgot password -->
        <div v-if="mode === 'login'" class="flex items-center justify-between">
          <label class="flex cursor-pointer items-center gap-2.5 text-xs text-[#777]">
            <input v-model="loginForm.rememberMe" type="checkbox" class="peer sr-only" />
            <span class="grid size-4 shrink-0 place-items-center rounded border border-white/15 peer-checked:border-[#16C65A] peer-checked:bg-[#16C65A] peer-checked:text-black">
              <Check v-if="loginForm.rememberMe" :size="10" :stroke-width="4" />
            </span>
            <span>Remember me</span>
          </label>
          <RouterLink :to="{ name: 'forgot-password' }" class="text-xs text-[#16C65A] hover:underline">Forgot password?</RouterLink>
        </div>

        <template v-if="mode === 'register'">
          <label class="melodyhub-field">
            <span>Confirm password</span>
            <div class="field-inline">
              <Music2 :size="15" class="shrink-0 text-[#71717A]" />
              <input
                v-model="registerForm.confirmPassword"
                :type="confirmPasswordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="Enter your password again"
              />
              <button type="button" class="ml-auto shrink-0 text-[#71717A] hover:text-[#F4FFF7]" title="Toggle confirm password visibility" @click="confirmPasswordVisible = !confirmPasswordVisible">
                <EyeOff v-if="confirmPasswordVisible" :size="16" /><Eye v-else :size="16" />
              </button>
            </div>
            <small v-if="errors.confirmPassword" class="text-red-300">{{ errors.confirmPassword }}</small>
          </label>

          <div>
            <label class="flex cursor-pointer items-start gap-3 text-xs leading-5 text-[#777]">
              <input v-model="registerForm.terms" type="checkbox" class="peer sr-only" />
              <span class="mt-0.5 grid size-4 shrink-0 place-items-center rounded border border-white/15 peer-checked:border-[#16C65A] peer-checked:bg-[#16C65A] peer-checked:text-black">
                <Check v-if="registerForm.terms" :size="12" :stroke-width="4" />
              </span>
              <span>I agree to the Terms of Service and Privacy Policy.</span>
            </label>
            <small v-if="errors.terms" class="mt-1 block text-red-300">{{ errors.terms }}</small>
          </div>
        </template>

        <p v-if="errors.general" class="rounded-lg bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ errors.general }}</p>
        <button type="submit" class="h-11 w-full rounded-full bg-[#16C65A] text-xs font-black text-black transition hover:bg-[#22C55E] disabled:cursor-not-allowed disabled:opacity-60" :disabled="authStore.isLoading">
          <span v-if="authStore.isLoading">PLEASE WAIT...</span>
          <span v-else>{{ mode === 'login' ? 'LOGIN TO MELODY HUB' : 'CREATE ACCOUNT' }}</span>
        </button>
      </form>
    </div>
  </section>
</template>
