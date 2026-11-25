/**
 * 全局时间/金额格式化工具。
 * 需求文档要求「全系统统一调用」，任何页面不得自行拼接时间字符串。
 */
const pad = (n) => String(n).padStart(2, '0')

/** 2026-05-29T21:50:00 → 2026-05-29 21:50 */
export function fmtTime(v) {
  if (!v) return '—'
  const d = typeof v === 'string' ? new Date(v.replace(' ', 'T')) : new Date(v)
  if (isNaN(d.getTime())) return String(v)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 只取日期 */
export function fmtDate(v) {
  if (!v) return '—'
  const s = fmtTime(v)
  return s === '—' ? s : s.slice(0, 10)
}

/** 相对时间：3 分钟前 / 2 天前（看板与消息用） */
export function fmtAgo(v) {
  if (!v) return '—'
  const d = new Date(String(v).replace(' ', 'T'))
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 86400 * 30) return `${Math.floor(diff / 86400)} 天前`
  return fmtDate(v)
}

/** 金额：1492 → ¥1,492.00 */
export function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 数字千分位 */
export function num(v, digits = 0) {
  const n = Number(v ?? 0)
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

/** 图片地址兜底：空值给默认占位（本地文件，不用外链） */
export function img(v, fallback = '/uploads/seed/vehicle/01.jpg') {
  return v && String(v).trim() ? v : fallback
}
