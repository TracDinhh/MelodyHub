<script setup>
import { onBeforeUnmount, watch } from 'vue';
import { X } from '@lucide/vue';

const props = defineProps({
  open: Boolean,
  title: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['close']);

function onKeydown(event) {
  if (event.key === 'Escape') emit('close');
}

watch(
  () => props.open,
  (open) => {
    document.body.classList.toggle('modal-open', open);
    if (open) {
      window.addEventListener('keydown', onKeydown);
    } else {
      window.removeEventListener('keydown', onKeydown);
    }
  }
);

onBeforeUnmount(() => {
  document.body.classList.remove('modal-open');
  window.removeEventListener('keydown', onKeydown);
});
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-[90] grid place-items-center bg-black/70 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      :aria-label="title"
      @mousedown.self="emit('close')"
    >
      <section class="max-h-[calc(100vh-2rem)] w-full max-w-lg overflow-auto rounded-lg border border-white/10 bg-[#151515] shadow-2xl">
        <header class="flex items-center justify-between border-b border-white/5 px-5 py-4">
          <h2 class="text-sm font-black text-white">{{ title }}</h2>
          <button type="button" class="sonix-icon-btn" title="Close" @click="emit('close')">
            <X :size="20" aria-hidden="true" />
            <span class="visually-hidden">Close</span>
          </button>
        </header>
        <div class="p-5">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="flex items-center justify-end gap-3 border-t border-white/5 px-5 py-4">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>
