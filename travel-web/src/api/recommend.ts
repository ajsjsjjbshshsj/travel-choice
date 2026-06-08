import request from './request'

// POST /api/recommend/calculate  — 触发 Java 调 Python 计算
export function calculateRecommend(data) {
  return request.post('/recommend/calculate', data)
}

// GET /api/recommend/results  — 查询 ads 表推荐结果（按城市+日期）
export function getRecommendResults(params) {
  return request.get('/recommend/results', { params })
}

// GET /api/recommend/results/by-request  — 按 requestId 查询推荐结果
export function getRecommendResultsByRequestId(requestId: string) {
  return request.get('/recommend/results/by-request', {
    params: { requestId }
  })
}

// GET /api/cities  — 获取出发城市列表
export function getCities() {
  return request.get('/cities')
}
