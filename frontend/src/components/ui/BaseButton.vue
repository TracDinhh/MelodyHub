<script setup>
import { computed } from 'vue';
import { LoaderCircle } from '@lucide/vue';

const props = defineProps({
  type: {
    type: String,
    default: 'button'
  },
  variant: {
    type: String,
    default: 'primary'
  },
  loading: Boolean,
  disabled: Boolean,
  block: Boolean
});

const classes = computed(() => [
  'inline-flex min-h-10 items-center justify-center gap-2 rounded-full px-5 text-xs font-black transition disabled:opacity-50',
  props.variant === 'primary'
    ? 'bg-[#16C65A] text-black hover:bg-[#22C55E]'
    : 'border border-white/10 bg-white/5 text-white hover:bg-white/10',
  { 'w-full': props.block }
]);
</script>

<template>
  <button
    :type="type"
    :class="classes"
    :disabled="disabled || loading"
    :aria-busy="loading"
  >
    <LoaderCircle v-if="loading" :size="18" class="spin" aria-hidden="true" />
    <slot v-else name="icon" />
    <span><slot /></span>
  </button>
</template>
