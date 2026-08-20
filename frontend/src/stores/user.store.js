import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { useAuthStore } from './auth.store';

export const useUserStore = defineStore('user', () => {
  const authStore = useAuthStore();
  const isLoading = ref(false);
  const error = ref('');

  const user = computed(() => authStore.user);

  return {
    user,
    isLoading,
    error
  };
});