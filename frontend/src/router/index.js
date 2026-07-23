import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: 'Home',
        breadcrumb: 'Home'
      }
    },
    {
      path: '/explore',
      name: 'explore',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: 'Explore',
        breadcrumb: 'Home / Explore'
      }
    },
    {
      path: '/radio',
      name: 'radio',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: 'Radio',
        breadcrumb: 'Home / Radio'
      }
    },
    {
      path: '/artists',
      name: 'artists',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: 'Artists',
        breadcrumb: 'Home / Artists'
      }
    },
    {
      path: '/albums',
      name: 'albums',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: 'Albums',
        breadcrumb: 'Home / Albums'
      }
    },
    {
      path: '/podcasts',
      name: 'podcasts',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: 'Podcasts',
        breadcrumb: 'Home / Podcasts'
      }
    },
    {
      path: '/artist/:slug',
      name: 'artist-detail',
      component: () => import('../views/ArtistView.vue'),
      meta: {
        title: 'Featured Artist',
        breadcrumb: 'Home / Featured Artists'
      }
    },
    {
      path: '/library',
      name: 'library',
      component: () => import('../views/LibraryView.vue'),
      meta: {
        requiresAuth: true,
        title: 'My Library',
        breadcrumb: 'Home / My Library'
      }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: {
        guestOnly: true,
        layout: 'auth',
        title: 'Sign in'
      }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: {
        layout: 'auth',
        title: 'Page not found'
      }
    }
  ],
  scrollBehavior: () => ({ top: 0 })
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath
      }
    };
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'home' };
  }

  document.title = `${to.meta.title || 'MelodyHub'} | MelodyHub`;
  return true;
});

export default router;
