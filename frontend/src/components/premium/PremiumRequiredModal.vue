<script setup>
import { Check, Crown, Lock } from '@lucide/vue';
import { useRouter } from 'vue-router';
import BaseModal from '../ui/BaseModal.vue';

defineProps({
  open: { type: Boolean, default: false }
});

const emit = defineEmits(['close']);
const router = useRouter();

function viewPlans() {
  emit('close');
  router.push({ name: 'premium' });
}
</script>

<template>
  <BaseModal :open="open" title="Premium required" @close="emit('close')">
    <div class="text-center">
      <span class="mx-auto grid size-14 place-items-center rounded-2xl bg-[#20E878] text-[#09090B]">
        <Crown :size="27" />
      </span>
      <Lock :size="20" class="mx-auto mt-5 text-[#20E878]" />
      <h3 class="mt-3 text-xl font-black text-white">Fullscreen lyrics are for Premium members</h3>
      <p class="mx-auto mt-2 max-w-sm text-sm leading-6 text-[#A1A1AA]">
        Upgrade to follow lyrics in fullscreen while your music keeps playing.
      </p>
      <ul class="mx-auto mt-5 max-w-xs space-y-2 text-left text-sm text-[#C4C4CC]">
        <li class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />Fullscreen synced lyrics</li>
        <li class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />Create and share lyric cards</li>
        <li class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />Unlimited playlists</li>
      </ul>
    </div>

    <template #footer>
      <button class="h-10 rounded-full px-4 text-sm font-bold text-[#A1A1AA] hover:text-white" @click="emit('close')">Not now</button>
      <button class="inline-flex h-10 items-center gap-2 rounded-full bg-[#20E878] px-5 text-sm font-black text-[#09090B] transition hover:bg-[#64F4A1]" @click="viewPlans">
        <Crown :size="15" /> View Premium plans
      </button>
    </template>
  </BaseModal>
</template>
