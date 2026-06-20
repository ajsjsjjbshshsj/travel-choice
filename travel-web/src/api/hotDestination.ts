import request from './request'

export function getHotDestinationTopN(limit = 10) {
  return request.get('/hot-destinations/topn', {
    params: { limit }
  })
}
