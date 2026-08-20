<script setup>
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import AdminLayout from './layouts/AdminLayout.vue';
import AuthLayout from './layouts/AuthLayout.vue';
import DefaultLayout from './layouts/DefaultLayout.vue';
import StudioLayout from './layouts/StudioLayout.vue';

const route = useRoute();
const layout = computed(() => {
  if (route.meta.layout === 'auth') return AuthLayout;
  if (route.meta.layout === 'admin') return AdminLayout;
  if (route.meta.layout === 'studio') return StudioLayout;
  return DefaultLayout;
});
</script>

<template>
  <component :is="layout">
    <RouterView />
  </component>
</template>