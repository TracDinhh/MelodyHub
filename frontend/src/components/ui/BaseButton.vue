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
  'base-button',
  `base-button--${props.variant}`,
  { 'base-button--block': props.block }
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
