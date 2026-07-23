<script setup>
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import AuthLayout from './layouts/AuthLayout.vue';
import DefaultLayout from './layouts/DefaultLayout.vue';

const route = useRoute();
const layout = computed(() => (route.meta.layout === 'auth' ? AuthLayout : DefaultLayout));
</script>

<template>
  <component :is="layout">
    <RouterView />
  </component>
</template>
