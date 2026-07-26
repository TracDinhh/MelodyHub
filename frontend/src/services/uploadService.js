import { apiClient } from './http';

export const uploadService = {
  /**
   * Upload an image for the current user. Returns { imageUrl, fileId, filePath }.
   * Lets axios set the multipart boundary by passing a FormData instance.
   */
  uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/api/uploads/image', formData);
  },

  /**
   * Upload an audio file for the current user. Returns { imageUrl, fileId, filePath }
   * where imageUrl is the hosted audio URL.
   */
  uploadAudio(file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/api/uploads/audio', formData, { timeout: 120000 });
  }
};
