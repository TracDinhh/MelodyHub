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
  }
};
