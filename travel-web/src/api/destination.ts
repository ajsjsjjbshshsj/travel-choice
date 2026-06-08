import request from './request'

// GET /api/destinations
export function getDestinations() {
  return request.get('/destinations')
}

// GET /api/destinations/:code
export function getDestinationByCode(code) {
  return request.get(`/destinations/${code}`)
}
