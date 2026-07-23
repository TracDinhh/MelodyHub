export function required(value, label) {
  return String(value || '').trim() ? '' : `${label} is required`;
}

export function minLength(value, length, label) {
  return String(value || '').length >= length
    ? ''
    : `${label} must be at least ${length} characters`;
}
