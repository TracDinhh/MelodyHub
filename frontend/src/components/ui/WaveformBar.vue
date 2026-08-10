<script setup>
import { computed } from 'vue';

const props = defineProps({
  /** Number of bars in the waveform strip */
  count: { type: Number, default: 28 },
  /** Current playback position 0–1 */
  progress: { type: Number, default: 0 },
  /** Bar width in px */
  barWidth: { type: Number, default: 2 },
  /** Gap between bars in px */
  gap: { type: Number, default: 2 },
  /** Max bar height in px */
  maxHeight: { type: Number, default: 20 },
  /** Min bar height in px */
  minHeight: { type: Number, default: 3 },
  /** Color of played bars */
  activeColor: { type: String, default: '#3DDE7C' },
  /** Color of unplayed bars */
  inactiveColor: { type: String, default: 'rgba(61, 222, 124, 0.22)' },
  /** Orientation */
  vertical: { type: Boolean, default: false },
  /** CSS class for wrapper */
  class: { type: String, default: '' }
});

/** Seed-based pseudo-random heights for visual variety */
function seededHeight(index, total, seed = 0.618) {
  const angle = (index / total) * Math.PI * 2 * seed;
  const wave = Math.sin(angle) * 0.5 + 0.5;
  const centerBoost = Math.pow(Math.sin((index / total) * Math.PI), 0.6);
  return wave * 0.4 + centerBoost * 0.6;
}

const bars = computed(() => {
  const result = [];
  const activeCount = Math.round(props.progress * props.count);
  for (let i = 0; i < props.count; i++) {
    const normalized = i / props.count;
    const rawHeight = seededHeight(i, props.count);
    const height = Math.round(props.minHeight + rawHeight * (props.maxHeight - props.minHeight));
    const isActive = i < activeCount;
    result.push({ index: i, height, isActive });
  }
  return result;
});

const containerStyle = computed(() => {
  if (props.vertical) {
    return {
      display: 'flex',
      'flex-direction': 'column',
      'align-items': 'center',
      gap: `${props.gap}px`,
      height: `${props.count * (props.barWidth + props.gap)}px`,
      width: `${props.barWidth}px`
    };
  }
  return {
    display: 'flex',
    'align-items': 'center',
    gap: `${props.gap}px`,
    width: `${props.count * (props.barWidth + props.gap)}px`,
    height: `${props.maxHeight}px`
  };
});
</script>

<template>
  <div :class="class" :style="containerStyle" aria-hidden="true">
    <span
      v-for="bar in bars"
      :key="bar.index"
      class="waveform-bar"
      :style="{
        height: `${bar.height}px`,
        width: `${barWidth}px`,
        background: bar.isActive ? activeColor : inactiveColor,
        boxShadow: bar.isActive ? `0 0 6px ${activeColor}55` : 'none',
        flexShrink: 0
      }"
    />
  </div>
</template>
