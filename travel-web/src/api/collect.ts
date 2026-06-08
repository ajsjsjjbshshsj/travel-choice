import request from './request'

// POST /api/weather/collect — 采集高德天气预报（无请求体）
export function collectWeather() {
  return request.post('/weather/collect')
}

// POST /api/route/collect — 采集高德公交路线
export function collectRoute(data: {
  originCity: string
  originLongitude: number
  originLatitude: number
  travelDate: string
}) {
  return request.post('/route/collect', data)
}

// POST /api/hotel/generate — 生成酒店价格数据
export function generateHotelPrice() {
  return request.post('/hotel/generate')
}

// POST /api/recommend/calculate — 生成推荐结果
export function calculateRecommend(data: any) {
  return request.post('/recommend/calculate', data)
}