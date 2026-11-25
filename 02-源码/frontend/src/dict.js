/**
 * 字典表镜像（与 new_045.sys_dict 保持一致）。
 * 车主/技师账号无权访问 /api/system/dict，因此前端内置一份，避免接口 403。
 */
export const DICT = {
  order_status: [
    { v: 'RECEIVE', l: '接车登记', c: 'info' },
    { v: 'DIAGNOSIS', l: '故障诊断', c: 'primary' },
    { v: 'QUOTE_CONFIRM', l: '报价确认', c: 'warning' },
    { v: 'REPAIR_PART', l: '维修领料', c: 'primary' },
    { v: 'QC', l: '质量检验', c: 'warning' },
    { v: 'SETTLE', l: '交车结算', c: 'warning' },
    { v: 'FINISHED', l: '已交车', c: 'success' },
    { v: 'REWORK', l: '返工中', c: 'danger' },
    { v: 'CANCELED', l: '已取消', c: 'info' }
  ],
  order_type: [
    { v: 'MAINTAIN', l: '保养' }, { v: 'REPAIR', l: '一般维修' }, { v: 'ACCIDENT', l: '事故维修' },
    { v: 'CHECK', l: '检测' }, { v: 'SHEET_METAL', l: '钣喷' }, { v: 'ELECTRIC', l: '电控检测' }
  ],
  work_type: [{ v: 'MECHANIC', l: '机修' }, { v: 'ELECTRIC', l: '电控' }, { v: 'SHEET_METAL', l: '钣金' }, { v: 'PAINT', l: '喷漆' }],
  check_result: [{ v: 'NORMAL', l: '正常' }, { v: 'OBSERVE', l: '建议观察' }, { v: 'REPAIR', l: '建议维修' }],
  severity: [{ v: 'HIGH', l: '高' }, { v: 'MID', l: '中' }, { v: 'LOW', l: '低' }],
  confirm_type: [{ v: 'PHONE', l: '电话确认' }, { v: 'SMS', l: '短信链接' }, { v: 'ONSITE', l: '到店确认' }, { v: 'APP', l: 'APP 确认' }],
  part_flow_type: [{ v: 'CLAIM', l: '领用' }, { v: 'EXTRA', l: '补领' }, { v: 'RETURN', l: '退回' }],
  pay_type: [{ v: 'CASH', l: '现金' }, { v: 'WECHAT', l: '微信' }, { v: 'ALIPAY', l: '支付宝' }, { v: 'CARD', l: '刷卡' }],
  qc_result: [{ v: 'PASS', l: '合格' }, { v: 'FAIL', l: '不合格' }],
  duty_result: [{ v: 'STORE', l: '门店责任' }, { v: 'PART', l: '配件质量' }, { v: 'CUSTOMER', l: '客户使用' }, { v: 'WAIT', l: '待判定' }],
  customer_type: [{ v: 'MEMBER', l: '会员客户' }, { v: 'NORMAL', l: '普通客户' }],
  vehicle_status: [{ v: 'NORMAL', l: '正常' }, { v: 'IN_REPAIR', l: '在修' }, { v: 'WAIT_SETTLE', l: '待结算' }]
}

export function label(type, v) {
  const hit = (DICT[type] || []).find((x) => x.v === v)
  return hit ? hit.l : (v || '—')
}

export function options(type) {
  return (DICT[type] || []).map((x) => ({ value: x.v, label: x.l }))
}

/** 状态 → 标签色（与设计系统一致：成功/警示/危险/信息） */
export function statusColor(v) {
  const hit = (DICT.order_status || []).find((x) => x.v === v)
  return hit ? hit.c : 'info'
}
