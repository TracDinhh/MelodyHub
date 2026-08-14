<script setup>
import { computed, useId } from 'vue';

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  },
  autocomplete: {
    type: String,
    default: 'off'
  },
  required: Boolean
});

const emit = defineEmits(['update:modelValue']);
const id = useId();
const errorId = computed(() => `${id}-error`);
</script>

<template>
  <div class="mb-4">
    <label :for="id" class="mb-2 block text-xs font-bold text-[#aaa]">{{ label }}</label>
    <div
      class="flex h-11 items-center gap-2 rounded-lg border bg-black/30 px-3 transition focus-within:ring-2 focus-within:ring-[#16C65A]/10"
      :class="error ? 'border-red-400/70' : 'border-white/10 focus-within:border-[#16C65A]/70'"
    >
      <span v-if="$slots.icon" class="grid text-[#666]" aria-hidden="true">
        <slot name="icon" />
      </span>
      <input
        :id="id"
        :value="modelValue"
        :type="type"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :required="required"
        :aria-invalid="Boolean(error)"
        :aria-describedby="error ? errorId : undefined"
        class="h-full min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-[#555]"
        @input="emit('update:modelValue', $event.target.value)"
      />
    </div>
    <p v-if="error" :id="errorId" class="mt-1.5 text-xs text-red-300">{{ error }}</p>
  </div>
</template>
