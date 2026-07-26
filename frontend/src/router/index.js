import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';
import { canAccessRoute, getRoleHomeRouteName } from '../utils/roleRouting';

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
      path: '/become-an-artist',
      name: 'become-an-artist',
      component: () => import('../views/BecomeArtistView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['USER'],
        title: 'Become an Artist',
        breadcrumb: 'Home / Become an Artist'
      }
    },
    {
      path: '/artist/dashboard',
      name: 'artist-dashboard',
      component: () => import('../views/artist/ArtistDashboardView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ARTIST'],
        workspace: 'artist',
        title: 'Artist Dashboard',
        breadcrumb: 'Artist Dashboard'
      }
    },
    {
      path: '/artist/songs/new',
      name: 'artist-song-upload',
      component: () => import('../views/artist/ArtistSongUploadView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ARTIST'],
        workspace: 'artist',
        title: 'Upload Song',
        breadcrumb: 'Artist / Upload Song'
      }
    },
    {
      path: '/artist/songs/:id/edit',
      name: 'artist-song-edit',
      component: () => import('../views/artist/ArtistSongEditView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ARTIST'],
        workspace: 'artist',
        title: 'Edit Song',
        breadcrumb: 'Artist / Edit Song'
      }
    },
    {
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('../views/admin/AdminOverviewView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ADMIN'],
        layout: 'admin',
        workspace: 'admin',
        title: 'Admin Overview',
        breadcrumb: 'Admin / Overview'
      }
    },
    {
      path: '/admin/artist-requests',
      name: 'admin-artist-requests',
      component: () => import('../views/admin/AdminArtistRequestsView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ADMIN'],
        layout: 'admin',
        workspace: 'admin',
        title: 'Artist Requests',
        breadcrumb: 'Admin / Artist Requests'
      }
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/admin/AdminUsersView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ADMIN'],
        layout: 'admin',
        workspace: 'admin',
        title: 'Users',
        breadcrumb: 'Admin / Users'
      }
    },
    {
      path: '/admin/artists',
      name: 'admin-artists',
      component: () => import('../views/admin/AdminArtistsView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ADMIN'],
        layout: 'admin',
        workspace: 'admin',
        title: 'Artists',
        breadcrumb: 'Admin / Artists'
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

  if (!canAccessRoute(authStore.user?.role, to.meta.allowedRoles)) {
    return { name: getRoleHomeRouteName(authStore.user?.role) };
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: getRoleHomeRouteName(authStore.user?.role) };
  }

  document.title = `${to.meta.title || 'MelodyHub'} | MelodyHub`;
  return true;
});

export default router;
