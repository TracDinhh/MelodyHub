<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { KeyRound, UserRound } from '@lucide/vue';
import BaseButton from '../components/ui/BaseButton.vue';
import BaseInput from '../components/ui/BaseInput.vue';
import { useAuth } from '../composables/useAuth';
import { minLength, required } from '../utils/validators';

const route = useRoute();
const router = useRouter();
const { login, isLoading } = useAuth();
const form = reactive({
  usernameOrEmail: '',
  password: ''
});
const errors = reactive({
  usernameOrEmail: '',
  password: ''
});
const submitError = ref('');

function validate() {
  errors.usernameOrEmail = required(form.usernameOrEmail, 'Username or email');
  errors.password =
    required(form.password, 'Password') || minLength(form.password, 6, 'Password');
  return !errors.usernameOrEmail && !errors.password;
}

async function submit() {
  submitError.value = '';
  if (!validate()) return;

  try {
    await login(form);
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
    await router.push(redirect);
  } catch (error) {
    submitError.value = error.message;
  }
}
</script>

<template>
  <section class="auth-panel" aria-labelledby="login-title">
    <div class="auth-panel__heading">
      <p class="eyebrow">Welcome back</p>
      <h1 id="login-title">Sign in to MelodyHub</h1>
      <p>Continue to your music workspace.</p>
    </div>

    <form novalidate @submit.prevent="submit">
      <div v-if="submitError" class="alert-message" role="alert">
        {{ submitError }}
      </div>

      <BaseInput
        v-model="form.usernameOrEmail"
        label="Username or email"
        placeholder="you@example.com"
        autocomplete="username"
        :error="errors.usernameOrEmail"
        required
      >
        <template #icon><UserRound :size="18" /></template>
      </BaseInput>

      <BaseInput
        v-model="form.password"
        label="Password"
        type="password"
        placeholder="Enter your password"
        autocomplete="current-password"
        :error="errors.password"
        required
      >
        <template #icon><KeyRound :size="18" /></template>
      </BaseInput>

      <BaseButton type="submit" :loading="isLoading" block>Sign in</BaseButton>
    </form>
  </section>
</template>
