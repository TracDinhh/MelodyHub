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
      path: '/songs/:slug',
      name: 'song-detail',
      component: () => import('../views/SongDetailView.vue'),
      meta: {
        title: 'Song',
        breadcrumb: 'Home / Song'
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
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: {
        requiresAuth: true,
        title: 'Your profile',
        breadcrumb: 'Home / Profile'
      }
    },
    {
      path: '/profile/edit',
      name: 'profile-edit',
      component: () => import('../views/EditProfileView.vue'),
      meta: {
        requiresAuth: true,
        title: 'Edit profile',
        breadcrumb: 'Home / Profile / Edit'
      }
    },
    {
      path: '/library/history',
      name: 'library-history',
      component: () => import('../views/ListenHistoryView.vue'),
      meta: {
        requiresAuth: true,
        title: 'Listen history',
        breadcrumb: 'Home / My Library / Listen history'
      }
    },
    {
      path: '/library/liked',
      name: 'library-liked',
      component: () => import('../views/LikedSongsView.vue'),
      meta: {
        requiresAuth: true,
        title: 'Liked songs',
        breadcrumb: 'Home / My Library / Liked songs'
      }
    },
    {
      path: '/playlists',
      name: 'playlists',
      component: () => import('../views/PlaylistsView.vue'),
      meta: {
        requiresAuth: true,
        title: 'My Playlists',
        breadcrumb: 'Home / My Playlists'
      }
    },
    {
      path: '/premium',
      name: 'premium',
      component: () => import('../views/PremiumView.vue'),
      meta: { requiresAuth: true, title: 'Premium', breadcrumb: 'Home / Premium' }
    },
    {
      path: '/playlists/:id',
      name: 'playlist-detail',
      component: () => import('../views/PlaylistDetailView.vue'),
      meta: {
        requiresAuth: true,
        title: 'Playlist',
        breadcrumb: 'Home / My Playlists / Playlist'
      }
    },
    {
      path: '/for-artists',
      name: 'artist-landing',
      component: () => import('../views/artist/ArtistLandingView.vue'),
      meta: {
        layout: 'auth',
        title: 'MelodyHub for Artists'
      }
    },
    {
      path: '/artist/login',
      name: 'artist-login',
      component: () => import('../views/artist/ArtistLoginView.vue'),
      meta: {
        layout: 'auth',
        title: 'Artist Login'
      }
    },
    {
      path: '/studio',
      name: 'studio-entry',
      component: () => import('../views/studio/StudioEntryView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Artist Studio',
        breadcrumb: 'Studio'
      }
    },
    {
      path: '/studio/access',
      name: 'studio-access',
      component: () => import('../views/studio/GetArtistAccessView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Get Artist Access',
        breadcrumb: 'Studio / Get Access'
      }
    },
    {
      path: '/studio/access/claim',
      name: 'studio-claim',
      component: () => import('../views/studio/ClaimArtistView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Claim an Artist',
        breadcrumb: 'Studio / Get Access / Claim'
      }
    },
    {
      path: '/studio/access/create',
      name: 'studio-create',
      component: () => import('../views/studio/CreateArtistView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Create an Artist',
        breadcrumb: 'Studio / Get Access / Create'
      }
    },
    {
      path: '/studio/requests',
      name: 'studio-requests',
      component: () => import('../views/studio/RequestStatusView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Request Status',
        breadcrumb: 'Studio / Request Status'
      }
    },
    {
      path: '/studio/artists/:artistId',
      name: 'studio-artist-overview',
      component: () => import('../views/studio/ArtistOverviewView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Artist Overview',
        breadcrumb: 'Studio / Overview'
      }
    },
    {
      path: '/studio/artists/:artistId/music',
      name: 'studio-artist-music',
      component: () => import('../views/studio/ArtistSongsView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'My Songs',
        breadcrumb: 'Studio / Music'
      }
    },
    {
      path: '/studio/artists/:artistId/music/new',
      name: 'studio-artist-upload',
      component: () => import('../views/studio/ArtistSongUploadView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Upload Song',
        breadcrumb: 'Studio / Music / Upload'
      }
    },
    {
      path: '/studio/artists/:artistId/music/:songId/edit',
      name: 'studio-artist-song-edit',
      component: () => import('../views/studio/ArtistSongEditView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Edit Song',
        breadcrumb: 'Studio / Music / Edit'
      }
    },
    {
      path: '/studio/artists/:artistId/profile',
      name: 'studio-artist-profile',
      component: () => import('../views/studio/ArtistProfileView.vue'),
      meta: {
        requiresAuth: true,
        layout: 'studio',
        title: 'Artist Profile',
        breadcrumb: 'Studio / Profile'
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
      path: '/admin/songs',
      name: 'admin-songs',
      component: () => import('../views/admin/AdminSongsView.vue'),
      meta: {
        requiresAuth: true,
        allowedRoles: ['ADMIN'],
        layout: 'admin',
        workspace: 'admin',
        title: 'Songs',
        breadcrumb: 'Admin / Songs'
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
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordView.vue'),
      meta: {
        guestOnly: true,
        layout: 'auth',
        title: 'Forgot password'
      }
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('../views/ResetPasswordView.vue'),
      meta: {
        guestOnly: true,
        layout: 'auth',
        title: 'Reset password'
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

  if (to.meta.requiresPremium && !authStore.isPremium) {
    return { name: 'premium', query: { redirect: to.fullPath } };
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
