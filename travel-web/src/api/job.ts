import request from './request'

// GET /api/etl/jobs
export function getEtlJobs() {
  return request.get('/etl/jobs')
}
