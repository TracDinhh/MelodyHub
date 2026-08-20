import { apiClient } from './http';

/**
 * Artist access requests (CLAIM_ARTIST / CREATE_ARTIST) and their status.
 */
export const artistAccessService = {
  /**
   * Submit a CLAIM_ARTIST or CREATE_ARTIST request. Returns the created request.
   */
  submitRequest(payload) {
    return apiClient.post('/api/artist-access-requests', payload);
  },

  /**
   * The current user's full access request history (newest first).
   */
  getMyRequests() {
    return apiClient.get('/api/artist-access-requests/me');
  }
};