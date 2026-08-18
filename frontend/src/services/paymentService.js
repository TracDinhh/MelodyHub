import { apiClient } from './http';

export const paymentService = {
  createOrder(planCode) { return apiClient.post('/api/payments/orders', { planCode }); },
  getOrder(orderId) { return apiClient.get(`/api/payments/orders/${orderId}`); },
  listMine(params) { return apiClient.get('/api/payments/orders', { params }); },
  markPaid(orderId) { return apiClient.post(`/api/payments/orders/${orderId}/paid`); }
};
