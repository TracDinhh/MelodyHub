import { ref } from 'vue';
import { defineStore } from 'pinia';
import { useAuthStore } from './auth.store';
import { followService } from '../services/followService';

export const useFollowStore = defineStore('follow', () => {
  // Set of artist ids the current user follows. Guests see an empty set.
  const followedArtistIds = ref(new Set());

  function isFollowed(artistId) {
    return followedArtistIds.value.has(artistId);
  }

  // Seeds local state from a server payload (e.g. the artist detail response's
  // `following` field) so the button is correct even before a hydrate call.
  function setFollowed(artistId, following) {
    if (artistId == null) return;
    const next = new Set(followedArtistIds.value);
    if (following) next.add(artistId);
    else next.delete(artistId);
    followedArtistIds.value = next;
  }

  // Loads the user's followed artist ids from the backend so the button state
  // is correct across reloads. No-op for guests. Safe to call repeatedly.
  async function hydrateFollows() {
    let auth = null;
    try {
      auth = useAuthStore();
    } catch {
      auth = null;
    }
    if (!auth?.isAuthenticated) {
      followedArtistIds.value = new Set();
      return;
    }
    try {
      const response = await followService.listIds();
      followedArtistIds.value = new Set(response?.ids || []);
    } catch {
      // Leave whatever state we had; a transient failure shouldn't wipe state.
    }
  }

  // Optimistically flips the follow state, then persists. Rolls back on
  // failure so the UI never claims a follow the server rejected. Guests are
  // ignored (callers decide how to surface the login prompt).
  async function toggleFollow(artistId) {
    if (!artistId) return;
    let auth = null;
    try {
      auth = useAuthStore();
    } catch {
      auth = null;
    }
    if (!auth?.isAuthenticated) return;

    const wasFollowing = followedArtistIds.value.has(artistId);
    setFollowed(artistId, !wasFollowing);
    try {
      if (wasFollowing) await followService.unfollow(artistId);
      else await followService.follow(artistId);
    } catch {
      setFollowed(artistId, wasFollowing);
    }
  }

  return {
    followedArtistIds,
    isFollowed,
    setFollowed,
    hydrateFollows,
    toggleFollow
  };
});