import { createPinia } from 'pinia';
import { createApp } from 'vue';
import VueApexCharts from 'vue3-apexcharts';
import './assets/styles/main.css';
import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth.store';
import { usePlayerStore } from './stores/player.store';
import { useFollowStore } from './stores/follow.store';

const app = createApp(App);
const pinia = createPinia();

async function bootstrap() {
  app.use(pinia);
  app.use(VueApexCharts);
  await useAuthStore(pinia).initialize();
  app.use(router);

  const player = usePlayerStore(pinia);
  const follow = useFollowStore(pinia);
  // Load liked-song ids so the heart state is correct on first paint. Fire and
  // forget — a slow likes request must not block mounting the app.
  void player.hydrateLikes();
  void follow.hydrateFollows();
  window.addEventListener('melodyhub:token-refreshed', () => {
    player.hydrateLikes();
    follow.hydrateFollows();
  });

  window.addEventListener('melodyhub:unauthorized', () => {
    player.likedIds = new Set();
    follow.followedArtistIds = new Set();
    const currentRoute = router.currentRoute.value;
    if (currentRoute.name === 'login') return;

    router.push({
      name: 'login',
      query: {
        redirect: currentRoute.fullPath || '/'
      }
    });
  });
  app.mount('#app');
}

bootstrap();
