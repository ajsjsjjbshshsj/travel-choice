import request from './request'

// GET /api/dashboard/stats
export function getDashboardStats() {
  return request.get('/dashboard/stats')
}
