import axios from 'axios'

// VITE_API_BASE_URL = full ALB/gateway URL for production or cross-env dev
// Leave unset (empty string) to fall through to the Vite dev-server proxy
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// AuthProvider registers this so the interceptor can trigger a React-based
// logout (no hard page reload) instead of window.location.href.
let _onUnauthorized: (() => void) | null = null

export function setOnUnauthorized(fn: () => void): void {
  _onUnauthorized = fn
}

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !err.config?.url?.startsWith('/auth/')) {
      if (_onUnauthorized) {
        _onUnauthorized()
      } else {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default apiClient
