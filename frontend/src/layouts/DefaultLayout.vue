<script setup>
import { computed } from 'vue';
import { useRouter, RouterLink } from 'vue-router';
import { Disc3, LayoutDashboard, LogOut } from '@lucide/vue';
import { useAuth } from '../composables/useAuth';

const router = useRouter();
const { user, displayName, logout } = useAuth();
const initials = computed(() =>
  displayName.value
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
);

async function signOut() {
  await logout();
  await router.push({ name: 'login' });
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <RouterLink class="brand" :to="{ name: 'home' }">
          <span class="brand__mark"><Disc3 :size="22" aria-hidden="true" /></span>
          <span>MelodyHub</span>
        </RouterLink>

        <nav class="primary-nav" aria-label="Main navigation">
          <RouterLink :to="{ name: 'home' }">
            <LayoutDashboard :size="17" aria-hidden="true" />
            <span>Overview</span>
          </RouterLink>
        </nav>

        <div class="account-menu">
          <div class="account-menu__avatar" aria-hidden="true">{{ initials }}</div>
          <div class="account-menu__copy">
            <strong>{{ displayName }}</strong>
            <span>{{ user?.role || 'USER' }}</span>
          </div>
          <button type="button" class="icon-button" title="Sign out" @click="signOut">
            <LogOut :size="19" aria-hidden="true" />
            <span class="visually-hidden">Sign out</span>
          </button>
        </div>
      </div>
    </header>

    <main class="app-main">
      <slot />
    </main>
  </div>
</template>
