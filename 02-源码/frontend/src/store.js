import { reactive } from 'vue'

/**
 * 轻量全局状态：登录态、当前用户、动态菜单。
 * 持久化键统一 ar_*（本项目专用前缀）。
 */
export const store = reactive({
  token: localStorage.getItem('ar_token') || '',
  user: JSON.parse(localStorage.getItem('ar_user') || 'null'),
  menus: JSON.parse(localStorage.getItem('ar_menus') || '[]')
})

export function setAuth(token, user, menus) {
  store.token = token
  store.user = user
  store.menus = menus || []
  localStorage.setItem('ar_token', token)
  localStorage.setItem('ar_user', JSON.stringify(user || null))
  localStorage.setItem('ar_menus', JSON.stringify(menus || []))
}

export function setUser(user) {
  store.user = user
  localStorage.setItem('ar_user', JSON.stringify(user || null))
}

export function clearAuth() {
  store.token = ''
  store.user = null
  store.menus = []
  localStorage.removeItem('ar_token')
  localStorage.removeItem('ar_user')
  localStorage.removeItem('ar_menus')
}

export const isOwner = () => store.user?.roleCode === 'ROLE_OWNER'
