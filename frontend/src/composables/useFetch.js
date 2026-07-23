import { onBeforeUnmount, ref } from 'vue';

export function useFetch(fetcher) {
  const data = ref(null);
  const error = ref('');
  const isLoading = ref(false);
  let controller;

  async function execute(...args) {
    controller?.abort();
    controller = new AbortController();
    isLoading.value = true;
    error.value = '';

    try {
      data.value = await fetcher(...args, controller.signal);
      return data.value;
    } catch (requestError) {
      if (requestError.name !== 'AbortError') {
        error.value = requestError.message;
      }
      return null;
    } finally {
      isLoading.value = false;
    }
  }

  onBeforeUnmount(() => controller?.abort());

  return { data, error, isLoading, execute };
}
