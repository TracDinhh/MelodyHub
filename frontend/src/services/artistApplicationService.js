import { apiClient } from './http';

export const artistApplicationService = {
  /**
   * Submit a PENDING artist request for the current user.
   * Returns the created ArtistRequest (does NOT upgrade the role immediately).
   */
  submitRequest(payload) {
    return apiClient.post('/api/artist/become', payload);
  },

  /**
   * Get the current user's latest artist request, or null if none exists.
   */
  getMyRequest() {
    return apiClient.get('/api/artist/request');
  }
};
