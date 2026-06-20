import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
      if (body.code === 200) {
        return body.data
      }
      return Promise.reject(new Error(body.message || 'Request failed'))
    }
    return body
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || 'Request failed'
    console.error('[API Error]', msg)
    return Promise.reject(error)
  }
)

export default request
