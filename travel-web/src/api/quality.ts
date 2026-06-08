import request from './request'

// GET /api/quality/results
export function getQualityResults() {
  return request.get('/quality/results')
}

// POST /api/quality/run
export function runQualityChecks() {
  return request.post('/quality/run')
}
