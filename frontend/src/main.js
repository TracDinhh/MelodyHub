import { createPinia } from 'pinia';
import { createApp } from 'vue';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
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
  app.mount('#app');
}

bootstrap();
