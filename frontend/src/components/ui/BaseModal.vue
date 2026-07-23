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
      class="base-modal"
      role="dialog"
      aria-modal="true"
      :aria-label="title"
      @mousedown.self="emit('close')"
    >
      <section class="base-modal__panel">
        <header class="base-modal__header">
          <h2>{{ title }}</h2>
          <button type="button" class="icon-button" title="Close" @click="emit('close')">
            <X :size="20" aria-hidden="true" />
            <span class="visually-hidden">Close</span>
          </button>
        </header>
        <div class="base-modal__body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="base-modal__footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>
