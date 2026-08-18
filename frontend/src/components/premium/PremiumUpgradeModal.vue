<script setup>
import { Check, Crown } from '@lucide/vue';
import { useRouter } from 'vue-router';
import BaseModal from '../ui/BaseModal.vue';

defineProps({
  open: { type: Boolean, default: false },
  feature: { type: String, required: true },
  description: { type: String, required: true }
});

const emit = defineEmits(['close']);
const router = useRouter();

function upgrade() {
  emit('close');
  router.push({ name: 'premium' });
}
</script>

<template>
  <BaseModal :open="open" title="Premium feature" @close="emit('close')">
    <div class="text-center">
      <span class="mx-auto grid size-12 place-items-center rounded-2xl bg-[#20E878] text-[#09090B]">
        <Crown :size="24" />
      </span>
      <h3 class="mt-4 text-xl font-black text-white">{{ feature }} is for Premium members</h3>
      <p class="mt-2 text-sm leading-6 text-[#A1A1AA]">{{ description }}</p>
      <ul class="mx-auto mt-5 max-w-sm space-y-2 text-left text-sm text-[#C4C4CC]">
        <li class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />Synced karaoke lyrics and lyric cards</li>
        <li class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />Unlimited playlists</li>
        <li class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />Premium badge and ad-free listening</li>
      </ul>
    </div>
    <template #footer>
      <button class="h-10 rounded-full px-4 text-sm font-bold text-[#A1A1AA] hover:text-white" @click="emit('close')">Not now</button>
      <button class="inline-flex h-10 items-center gap-2 rounded-full bg-[#20E878] px-5 text-sm font-black text-[#09090B] transition hover:bg-[#64F4A1]" @click="upgrade">
        <Crown :size="15" /> Upgrade to Premium
      </button>
    </template>
  </BaseModal>
</template>
