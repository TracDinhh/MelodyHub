import { apiClient } from './http';

export const listenHistoryService = {
  list(params) {
    return apiClient.get('/api/listens', { params });
  },

  record(songId, playedSec) {
    return apiClient.post('/api/listens', {
      songId,
      playedSec
    });
  },

  remove(historyId) {
    return apiClient.delete(`/api/listens/${historyId}`);
  },

  clear() {
    return apiClient.delete('/api/listens');
  }
};
