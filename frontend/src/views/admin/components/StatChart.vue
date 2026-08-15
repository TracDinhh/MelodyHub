<script setup>
import { computed } from 'vue';

/**
 * Thin wrapper around <apexchart> that applies the shared dark-theme defaults
 * (transparent background, muted gridlines/labels, accent palette, toolbar off)
 * so each dashboard chart only supplies its own type, series, and extras.
 */
const props = defineProps({
  type: {
    type: String,
    required: true // 'area' | 'bar' | 'donut' | ...
  },
  series: {
    type: Array,
    required: true
  },
  // Category labels for axis charts (area/bar).
  categories: {
    type: Array,
    default: () => []
  },
  // Slice labels for donut/pie charts.
  labels: {
    type: Array,
    default: () => []
  },
  height: {
    type: [Number, String],
    default: 300
  },
  // Per-chart overrides deep-merged over the shared theme defaults.
  extraOptions: {
    type: Object,
    default: () => ({})
  }
});

const ACCENT = '#16C65A';
const PALETTE = [ACCENT, '#38BDF8', '#E879F9', '#FBBF24', '#F87171', '#A78BFA'];
const MUTED = '#888';
const GRID = 'rgba(255,255,255,0.08)';

function mergeDeep(base, override) {
  const out = { ...base };
  for (const key of Object.keys(override)) {
    const value = override[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      out[key] = mergeDeep(base[key] || {}, value);
    } else {
      out[key] = value;
    }
  }
  return out;
}

const chartOptions = computed(() => {
  const base = {
    chart: {
      type: props.type,
      background: 'transparent',
      toolbar: { show: false },
      zoom: { enabled: false },
      fontFamily: 'inherit',
      foreColor: MUTED
    },
    theme: { mode: 'dark' },
    colors: PALETTE,
    grid: { borderColor: GRID, strokeDashArray: 4 },
    dataLabels: { enabled: false },
    stroke: { curve: 'smooth', width: props.type === 'area' ? 2 : 0 },
    fill: {
      type: props.type === 'area' ? 'gradient' : 'solid',
      gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 90] }
    },
    tooltip: { theme: 'dark' },
    legend: { labels: { colors: MUTED }, position: 'bottom' },
    xaxis: {
      categories: props.categories,
      labels: { style: { colors: MUTED } },
      axisBorder: { color: GRID },
      axisTicks: { color: GRID }
    },
    yaxis: { labels: { style: { colors: MUTED } } },
    plotOptions: { bar: { borderRadius: 4, columnWidth: '55%' } }
  };

  if (props.labels.length) {
    base.labels = props.labels;
  }

  return mergeDeep(base, props.extraOptions);
});
</script>

<template>
  <apexchart :type="type" :height="height" :series="series" :options="chartOptions" />
</template>
