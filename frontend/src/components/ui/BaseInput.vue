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
  <div class="base-input">
    <label :for="id" class="base-input__label">{{ label }}</label>
    <div class="base-input__control" :class="{ 'base-input__control--invalid': error }">
      <span v-if="$slots.icon" class="base-input__icon" aria-hidden="true">
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
        @input="emit('update:modelValue', $event.target.value)"
      />
    </div>
    <p v-if="error" :id="errorId" class="base-input__error">{{ error }}</p>
  </div>
</template>
