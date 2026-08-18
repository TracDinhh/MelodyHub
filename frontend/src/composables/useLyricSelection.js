import { computed, ref } from 'vue';

// Manages selecting a contiguous run of 1..MAX lyric lines for the lyric-card
// feature. Clicking a line toggles it; clicking a line adjacent to the current
// selection extends it; clicking elsewhere starts a fresh single-line pick.
const MAX_LINES = 4;

function sortIndices(indices) {
  return [...indices].sort((a, b) => a - b);
}

export function useLyricSelection() {
  // Set of selected line indices (always contiguous).
  const selected = ref(new Set());

  const sortedIndices = computed(() => sortIndices(selected.value));
  const hasSelection = computed(() => selected.value.size > 0);

  function isSelected(index) {
    return selected.value.has(index);
  }

  function toggle(index) {
    const current = new Set(selected.value);
    if (current.has(index)) {
      // Only allow removing an end of the run so the selection stays contiguous.
      const indices = sortIndices(current);
      if (index === indices[0] || index === indices[indices.length - 1]) {
        current.delete(index);
      } else {
        // Clicking a middle line collapses to just that line.
        selected.value = new Set([index]);
        return;
      }
      selected.value = current;
      return;
    }

    if (current.size === 0) {
      selected.value = new Set([index]);
      return;
    }

    const indices = sortIndices(current);
    const min = indices[0];
    const max = indices[indices.length - 1];
    const adjacent = index === min - 1 || index === max + 1;
    if (adjacent && current.size < MAX_LINES) {
      current.add(index);
      selected.value = current;
    } else {
      // Non-adjacent, or at the max — start a fresh single-line selection.
      selected.value = new Set([index]);
    }
  }

  function clear() {
    selected.value = new Set();
  }

  return { selected, sortedIndices, hasSelection, isSelected, toggle, clear, MAX_LINES };
}
