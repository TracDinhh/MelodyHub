<script setup>
import { computed, onMounted, ref } from 'vue';
import { Check, Crown, ShieldCheck } from '@lucide/vue';
import { paymentService } from '../services/paymentService';
import { useAuthStore } from '../stores/auth.store';
import { formatDate } from '../utils/formatDate';

const authStore = useAuthStore();
const plans = [
  { code: 'MONTHLY', name: 'Monthly', amount: 29000, days: 30 },
  { code: 'QUARTERLY', name: 'Quarterly', amount: 79000, days: 90, popular: true }
];
const order = ref(null);
const busy = ref(false);
const loading = ref(true);
const error = ref('');

const money = computed(() => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }));
const isPremium = computed(() => authStore.isPremium);
const premiumUntil = computed(() => authStore.premiumUntil);
const confirmed = computed(() => order.value?.status === 'CONFIRMED');
// A live order is one that still needs the transfer flow (not yet resolved).
const pendingOrder = computed(() => order.value?.status === 'PENDING');

async function choosePlan(plan) {
  busy.value = true; error.value = '';
  try { order.value = await paymentService.createOrder(plan.code); }
  catch (caught) {
    if (caught.code === 'PREMIUM_ALREADY_ACTIVE') {
      try {
        await authStore.refreshUser();
        order.value = null;
        if (isPremium.value) return;
      } catch {
        // Fall through to the API error if the user refresh also fails.
      }
    }
    error.value = caught.message || 'Could not create payment order.';
  }
  finally { busy.value = false; }
}

async function activatePremium() {
  if (!pendingOrder.value || busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    order.value = await paymentService.markPaid(order.value.id);
    await authStore.refreshUser();
  } catch (caught) {
    error.value = caught.message || 'Could not activate Premium.';
  } finally {
    busy.value = false;
  }
}

// Resume an in-flight transfer if the user reloads before marking it paid, so
// we never create a second order for the same person.
async function resumePendingOrder() {
  try {
    const response = await paymentService.listMine({ page: 1, size: 5 });
    const items = response?.items || [];
    const pending = items.find((item) => item.status === 'PENDING');
    if (pending) order.value = pending;
  } catch {
    // Non-fatal: user can still start a fresh order from the plans.
  }
}

function startOver() {
  order.value = null;
  error.value = '';
}

onMounted(async () => {
  // Already-premium users never see the payment flow — only their status.
  if (!isPremium.value) await resumePendingOrder();
  loading.value = false;
});
</script>

<template>
  <main class="mx-auto max-w-5xl px-5 py-10 sm:px-8">
    <section class="rounded-3xl border border-[#20E878]/20 bg-[radial-gradient(circle_at_18%_0%,rgba(32,232,120,0.14),transparent_40%),linear-gradient(110deg,#181116,#121214)] px-7 py-10 text-center sm:px-12">
      <span class="mx-auto grid size-12 place-items-center rounded-2xl bg-[#20E878] text-[#09090B]"><Crown :size="24" /></span>
      <p class="mt-5 text-xs font-bold uppercase tracking-[0.22em] text-[#20E878]">MelodyHub Premium</p>
      <h1 class="mt-3 text-3xl font-black text-[#F4FFF7] sm:text-5xl">Listen without limits.</h1>
      <p class="mx-auto mt-4 max-w-xl text-sm leading-6 text-[#C4C4CC]">Unlock synced karaoke lyrics, unlimited playlists, a Premium badge, and an ad-free listening experience.</p>
    </section>

    <p v-if="error" class="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ error }}</p>

    <div v-if="loading" class="mt-10 text-center text-sm text-[#A1A1AA]">Loading…</div>

    <!-- Already Premium: no payment flow, just status. -->
    <section v-else-if="isPremium" class="mx-auto mt-8 max-w-2xl rounded-2xl border border-[#20E878]/30 bg-[#121214] p-8 text-center">
      <ShieldCheck :size="48" class="mx-auto text-[#20E878]" />
      <h2 class="mt-4 text-2xl font-bold text-[#F4FFF7]">You're a Premium member</h2>
      <p class="mt-2 text-sm text-[#C4C4CC]">
        <template v-if="premiumUntil">Your Premium is active until <span class="font-semibold text-[#F4FFF7]">{{ formatDate(premiumUntil) }}</span>.</template>
        <template v-else>Enjoy the full MelodyHub experience.</template>
      </p>
      <ul class="mx-auto mt-6 max-w-sm space-y-3 text-left text-sm text-[#C4C4CC]">
        <li v-for="benefit in ['Synced karaoke lyrics', 'Unlimited playlists', 'Premium badge and no ads']" :key="benefit" class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />{{ benefit }}</li>
      </ul>
    </section>

    <!-- Plan selection (free users, no active order). -->
    <section v-else-if="!order" class="mt-8 grid gap-5 md:grid-cols-2">
      <article v-for="plan in plans" :key="plan.code" class="relative rounded-2xl border border-white/[0.10] bg-[#121214] p-6" :class="plan.popular ? 'ring-1 ring-[#20E878]/60' : ''">
        <span v-if="plan.popular" class="absolute -top-3 right-5 rounded-full bg-[#20E878] px-3 py-1 text-[10px] font-black uppercase tracking-wider text-[#09090B]">Best value</span>
        <h2 class="text-xl font-bold text-[#F4FFF7]">{{ plan.name }}</h2>
        <p class="mt-2 text-3xl font-black text-[#F4FFF7]">{{ money.format(plan.amount) }}</p>
        <p class="mt-1 text-sm text-[#A1A1AA]">{{ plan.days }} days of Premium</p>
        <ul class="mt-6 space-y-3 text-sm text-[#C4C4CC]"><li v-for="benefit in ['Synced karaoke lyrics', 'Unlimited playlists', 'Premium badge and no ads']" :key="benefit" class="flex gap-2"><Check :size="16" class="mt-0.5 shrink-0 text-[#20E878]" />{{ benefit }}</li></ul>
        <button class="mt-7 h-11 w-full rounded-full bg-[#20E878] text-sm font-bold text-[#09090B] transition hover:bg-[#64F4A1] disabled:opacity-50" :disabled="busy" @click="choosePlan(plan)">{{ busy ? 'Preparing…' : `Choose ${plan.name}` }}</button>
      </article>
    </section>

    <!-- Active order: confirmed success or pending transfer. -->
    <section v-else class="mx-auto mt-8 max-w-2xl rounded-2xl border border-white/[0.10] bg-[#121214] p-6 text-center">
      <template v-if="confirmed"><ShieldCheck :size="48" class="mx-auto text-[#20E878]" /><h2 class="mt-4 text-2xl font-bold text-[#F4FFF7]">Premium is active!</h2><p class="mt-2 text-sm text-[#C4C4CC]">Your account has been upgraded. Enjoy the full experience.</p></template>
      <template v-else-if="pendingOrder">
        <h2 class="text-xl font-bold text-[#F4FFF7]">Scan to transfer {{ money.format(order.amount) }}</h2>
        <p class="mt-2 text-sm text-[#A1A1AA]">Scan the QR code with your banking app to complete the transfer.</p>
        <img v-if="order.qrImageUrl" :src="order.qrImageUrl" alt="Bank transfer QR code" class="mx-auto mt-5 size-72 rounded-2xl bg-white p-2 shadow-[0_16px_36px_rgba(0,0,0,0.35)] sm:size-80" />
        <p class="mt-4 text-sm text-[#C4C4CC]">Transfer note: <span class="font-mono font-bold text-[#20E878]">{{ order.transferNote }}</span></p>
        <button class="mt-6 inline-flex h-11 items-center rounded-full bg-[#20E878] px-6 text-sm font-black text-[#09090B] transition hover:bg-[#64F4A1] disabled:opacity-50" :disabled="busy" @click="activatePremium">
          <ShieldCheck :size="16" class="mr-2" />{{ busy ? 'Activating…' : "I've completed the transfer" }}
        </button>
        <p class="mt-3 text-xs text-[#71717A]">After completing the transfer, activate Premium here to unlock your benefits immediately.</p>
      </template>
      <template v-else>
        <h2 class="text-xl font-bold text-[#F4FFF7]">This order was {{ (order.status || '').toLowerCase() }}</h2>
        <p class="mt-2 text-sm text-[#C4C4CC]">You can start a new upgrade if you'd like.</p>
        <button class="mt-5 h-11 rounded-full bg-[#20E878] px-6 text-sm font-bold text-[#09090B] transition hover:bg-[#64F4A1]" @click="startOver">Choose a plan</button>
      </template>
    </section>
  </main>
</template>
