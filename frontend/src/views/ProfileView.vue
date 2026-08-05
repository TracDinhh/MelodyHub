<script setup>
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { AtSign, CheckCircle2, Crown, Mail, PenLine, ShieldAlert, UserRound } from '@lucide/vue';
import { useAuthStore } from '../stores/auth.store';

const authStore = useAuthStore();
const user = computed(() => authStore.user || {});
const displayName = computed(() => user.value.displayName || user.value.username || 'MelodyHub listener');
const email = computed(() => user.value.email || '');
const role = computed(() => user.value.role || 'USER');
const avatarUrl = computed(() => user.value.avatarUrl || '');
const memberSince = computed(() => {
  if (!user.value.createdAt) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(user.value.createdAt));
});
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-8 flex items-end justify-between gap-4">
      <div>
        <p class="melodyhub-kicker">ACCOUNT</p>
        <h1 class="melodyhub-section-title">Your profile</h1>
        <p class="mt-3 max-w-lg text-sm leading-6 text-[#999]">
          Manage how others see you across MelodyHub.
        </p>
      </div>
      <RouterLink
        :to="{ name: 'profile-edit' }"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#1DB954] px-5 text-xs font-black text-black shadow-[0_8px_22px_rgba(29,185,84,0.26)] transition hover:scale-[1.03]"
      >
        <PenLine :size="14" /> EDIT PROFILE
      </RouterLink>
    </div>

    <article class="overflow-hidden rounded-xl border border-white/10 bg-[#121719]">
      <div class="relative h-28 bg-gradient-to-br from-[#1DB954]/25 via-[#101417] to-[#101417]" />
      <div class="px-6 pb-6 sm:px-8">
        <div class="-mt-12 flex flex-col items-start gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div class="flex items-end gap-4">
            <span class="relative grid size-24 shrink-0 place-items-center overflow-hidden rounded-full bg-[#181818] ring-4 ring-[#121719]">
              <img
                v-if="avatarUrl"
                :src="avatarUrl"
                :alt="`${displayName} avatar`"
                class="h-full w-full object-cover"
              />
              <UserRound v-else :size="40" class="text-[#555]" />
            </span>
            <div class="pb-1">
              <h2 class="text-xl font-black text-white">{{ displayName }}</h2>
              <p class="mt-1 flex items-center gap-1 text-xs text-[#999]">
                <Mail :size="12" />
                <span>{{ email || 'No email set' }}</span>
              </p>
            </div>
          </div>
          <span class="inline-flex items-center gap-1 rounded-full bg-[#1DB954]/10 px-3 py-1 text-[10px] font-black tracking-wider text-[#1DB954]">
            <Crown :size="11" /> {{ role }}
          </span>
        </div>

        <dl class="mt-7 grid gap-4 border-t border-white/5 pt-6 sm:grid-cols-2">
          <div>
            <dt class="text-[10px] font-black uppercase tracking-wider text-[#666]">Display name</dt>
            <dd class="mt-1 text-sm font-bold text-white">{{ displayName }}</dd>
          </div>
          <div>
            <dt class="text-[10px] font-black uppercase tracking-wider text-[#666]">Username</dt>
            <dd class="mt-1 flex items-center gap-1.5 text-sm font-bold text-white">
              <AtSign :size="13" class="text-[#888]" /> {{ user.username || '—' }}
            </dd>
          </div>
          <div>
            <dt class="text-[10px] font-black uppercase tracking-wider text-[#666]">Email</dt>
            <dd class="mt-1 text-sm font-bold text-white">{{ email || '—' }}</dd>
          </div>
          <div>
            <dt class="text-[10px] font-black uppercase tracking-wider text-[#666]">Member since</dt>
            <dd class="mt-1 text-sm font-bold text-white">{{ memberSince || '—' }}</dd>
          </div>
        </dl>

        <div class="mt-7 flex items-start gap-3 rounded-lg border border-white/10 bg-white/[0.02] p-4 text-xs text-[#888]">
          <CheckCircle2 :size="16" class="mt-0.5 shrink-0 text-[#65e78c]" />
          <p>
            Your avatar is stored in your personal ImageKit folder (<code class="text-[#aaa]">/users/{{ user.id }}/uploads/</code>).
            Display name and email updates are immediately reflected across the app.
          </p>
        </div>
      </div>
    </article>
  </div>
</template>
