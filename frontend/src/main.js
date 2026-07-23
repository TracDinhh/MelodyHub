import { createPinia } from 'pinia';
import { createApp } from 'vue';
import './assets/styles/main.css';
import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth.store';

const app = createApp(App);
const pinia = createPinia();

async function bootstrap() {
  app.use(pinia);
  await useAuthStore(pinia).initialize();
  app.use(router);
  window.addEventListener('melodyhub:unauthorized', () => {
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
