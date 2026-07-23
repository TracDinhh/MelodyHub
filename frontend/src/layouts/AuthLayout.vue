<template>
  <main
    class="relative h-screen overflow-y-auto bg-cover bg-center px-4 py-8"
    style="background-image: url('https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=1800&q=85')"
  >
    <div class="absolute inset-0 bg-black/80 backdrop-blur-[2px]" />
    <div class="relative grid min-h-[calc(100vh-4rem)] place-items-center">
      <slot />
    </div>
  </main>
</template>
