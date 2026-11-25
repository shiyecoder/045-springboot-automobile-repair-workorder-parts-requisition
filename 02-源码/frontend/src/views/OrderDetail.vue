<template>
  <div v-if="o.id">
    <div class="page-head">
      <div>
        <h1>{{ o.orderNo }}</h1>
        <p>
          {{ o.plateNo }} · {{ o.vehicleName }} · {{ o.customerName }} ·
          {{ label('order_type', o.orderType) }} · 接车 {{ fmtTime(o.receiveTime) }}
        </p>
      </div>
      <div class="head-acts">
        <el-button @click="router.back()">返回</el-button>
        <el-button v-if="o.status === 'DIAGNOSIS'" type="primary" @click="router.push(`/diagnosis/${o.id}`)">故障诊断</el-button>
        <el-button v-if="o.status === 'QUOTE_CONFIRM'" type="primary" @click="router.push(`/quote/${o.id}`)">报价确认</el-button>
        <el-button v-if="o.status === 'QC'" type="primary" @click="router.push(`/inspection/${o.id}`)">质量检验</el-button>
        <el-button v-if="o.status === 'SETTLE'" type="primary" @click="router.push(`/settlement/${o.id}`)">交车结算</el-button>
        <el-button v-if="['REPAIR_PART','QC'].includes(o.status)" @click="router.push(`/part/claim/${o.id}`)">领用配件</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-b tight">
        <div class="flex" style="justify-content:space-between;margin-bottom:6px">
          <StatusTag :status="o.status" :name="o.statusName" />
          <span class="tiny muted">预计交车 {{ fmtTime(o.expectFinish) }} · 返工 {{ o.reworkTimes }} 次</span>
        </div>
        <div class="steps">
          <div v-for="s in STEPS" :key="s.v" class="step" :class="stepClass(s.v)">{{ s.l }}</div>
        </div>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <el-tabs v-model="tab" class="tabs">
            <el-tab-pane label="概览" name="over">
              <div class="grid-2">
                <dl class="kv">
                  <dt>工单号</dt><dd class="num strong">{{ o.orderNo }}</dd>
                  <dt>车牌 / 车型</dt><dd>{{ o.plateNo }} · {{ o.vehicleName }}</dd>
                  <dt>客户</dt><dd>{{ o.customerName }}</dd>
                  <dt>服务顾问</dt><dd>{{ o.advisorName }}</dd>
                  <dt>主修技师</dt><dd>{{ o.techName || '未派工' }}</dd>
                  <dt>业务类型</dt><dd>{{ label('order_type', o.orderType) }}</dd>
                </dl>
                <dl class="kv">
                  <dt>进厂里程</dt><dd class="num">{{ num(o.reception?.mileage) }} km</dd>
                  <dt>油量</dt><dd>{{ o.reception?.fuelLevel || '—' }}</dd>
                  <dt>外观划痕</dt><dd class="tiny">{{ o.reception?.scratchDesc || '—' }}</dd>
                  <dt>随车物品</dt><dd class="tiny">{{ o.reception?.itemsDesc || '—' }}</dd>
                  <dt>应收金额</dt><dd class="num strong">{{ money(o.totalAmount) }}</dd>
                  <dt>已收金额</dt><dd class="num">{{ money(o.paidAmount) }}</dd>
                </dl>
              </div>
              <div class="split"></div>
              <div class="tiny muted">故障描述</div>
              <div style="margin-top:4px">{{ o.faultDesc || '—' }}</div>
              <div v-if="(o.reception?.photos || '').length" class="flex gap mt12" style="flex-wrap:wrap">
                <img v-for="p in String(o.reception.photos).split(',')" :key="p" :src="p"
                     style="width:110px;height:82px;object-fit:cover;border-radius:5px;border:1px solid var(--line)" />
              </div>
            </el-tab-pane>

            <el-tab-pane :label="`检测(${(o.inspections || []).length})`" name="insp">
              <el-table :data="o.inspections || []" size="default">
                <el-table-column prop="checkItem" label="检查项" min-width="140" />
                <el-table-column label="结果" min-width="110">
                  <template #default="{ row }">
                    <span class="tag" :class="row.checkResult === 'REPAIR' ? 'danger' : row.checkResult === 'OBSERVE' ? 'warning' : 'success'">
                      {{ label('check_result', row.checkResult) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="faultDesc" label="检测描述" min-width="240" show-overflow-tooltip />
                <el-table-column label="严重度" min-width="80">
                  <template #default="{ row }">{{ label('severity', row.severity) }}</template>
                </el-table-column>
                <el-table-column prop="suggestion" label="建议" min-width="150" show-overflow-tooltip />
                <el-table-column label="检测时间" min-width="135">
                  <template #default="{ row }"><span class="num">{{ fmtTime(row.checkTime) }}</span></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`项目(${(o.items || []).length})`" name="item">
              <el-table :data="o.items || []" size="default">
                <el-table-column prop="projectName" label="维修项目" min-width="160" />
                <el-table-column label="工种" min-width="90">
                  <template #default="{ row }">{{ label('work_type', row.workType) }}</template>
                </el-table-column>
                <el-table-column prop="hours" label="工时" min-width="80" align="right" />
                <el-table-column label="工时费" min-width="100" align="right">
                  <template #default="{ row }"><span class="num">{{ money(row.laborFee) }}</span></template>
                </el-table-column>
                <el-table-column label="确认" min-width="90">
                  <template #default="{ row }">
                    <span class="tag" :class="row.confirmed ? 'success' : 'warning'">{{ row.confirmed ? '已确认' : '待确认' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="项目状态" min-width="100">
                  <template #default="{ row }">{{ row.itemStatus === 'DONE' ? '已完成' : '待施工' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`报价(${(o.quotes || []).length})`" name="quote">
              <div v-for="q in o.quotes || []" :key="q.id" class="card" style="margin-bottom:10px">
                <div class="card-h">
                  <h3>{{ q.quoteNo }} · 第 {{ q.versionNo }} 版</h3>
                  <span class="sub">{{ q.status === 'CONFIRMED' ? '已确认' : q.status }}</span>
                </div>
                <div class="card-b tight">
                  <div class="flex" style="justify-content:space-between">
                    <span class="tiny muted">工时 {{ money(q.totalLabor) }} · 配件 {{ money(q.totalPart) }} · 优惠 {{ money(q.discount) }}</span>
                    <span class="num strong">{{ money(q.totalAmount) }}</span>
                  </div>
                </div>
              </div>
              <div v-if="!(o.quotes || []).length" class="empty">暂无报价单</div>
            </el-tab-pane>

            <el-tab-pane :label="`领退(${(o.flows || []).length})`" name="flow">
              <el-table :data="o.flows || []" size="default">
                <el-table-column prop="flowNo" label="单号" min-width="160" />
                <el-table-column prop="partId" label="配件ID" min-width="90" />
                <el-table-column prop="qty" label="数量" min-width="80" align="right" />
                <el-table-column label="类型" min-width="90">
                  <template #default="{ row }">{{ label('part_flow_type', row.flowType) }}</template>
                </el-table-column>
                <el-table-column prop="oldPartHandle" label="旧件处理" min-width="130" />
                <el-table-column label="时间" min-width="140">
                  <template #default="{ row }"><span class="num">{{ fmtTime(row.flowTime) }}</span></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`质检(${(o.qcList || []).length})`" name="qc">
              <el-table :data="o.qcList || []" size="default">
                <el-table-column label="结果" min-width="100">
                  <template #default="{ row }">
                    <span class="tag" :class="row.qcResult === 'PASS' ? 'success' : 'danger'">{{ label('qc_result', row.qcResult) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="qcDesc" label="检验说明" min-width="280" show-overflow-tooltip />
                <el-table-column label="检验时间" min-width="140">
                  <template #default="{ row }"><span class="num">{{ fmtTime(row.qcTime) }}</span></template>
                </el-table-column>
              </el-table>
              <div v-if="!(o.qcList || []).length" class="empty">尚未质检</div>
            </el-tab-pane>

            <el-tab-pane :label="`费用(${(o.fees || []).length})`" name="fee">
              <el-table :data="o.fees || []" size="default">
                <el-table-column prop="feeDesc" label="费用项" min-width="200" />
                <el-table-column label="类型" min-width="100">
                  <template #default="{ row }">{{ { LABOR: '工时费', PART: '配件费', OTHER: '其他' }[row.feeType] || row.feeType }}</template>
                </el-table-column>
                <el-table-column label="金额" min-width="120" align="right">
                  <template #default="{ row }"><span class="num">{{ money(row.amount) }}</span></template>
                </el-table-column>
              </el-table>
              <div class="split"></div>
              <div class="tiny muted">收款记录</div>
              <div v-for="p in o.payments || []" :key="p.id" class="flex" style="justify-content:space-between;margin-top:6px">
                <span>{{ label('pay_type', p.payType) }} · {{ fmtTime(p.payTime) }}</span>
                <span class="num">{{ money(p.amount) }}</span>
              </div>
              <div v-if="!(o.payments || []).length" class="empty">暂无收款</div>
            </el-tab-pane>

            <el-tab-pane label="质保/交车" name="warranty">
              <el-table :data="o.warranties || []" size="default">
                <el-table-column prop="warrantyNo" label="质保单号" min-width="160" />
                <el-table-column prop="projectName" label="项目" min-width="150" />
                <el-table-column prop="warrantyDays" label="质保天数" min-width="100" align="right" />
                <el-table-column prop="warrantyKm" label="质保里程" min-width="110" align="right" />
                <el-table-column label="有效期" min-width="180">
                  <template #default="{ row }"><span class="num">{{ fmtDate(row.startDate) }} ~ {{ fmtDate(row.endDate) }}</span></template>
                </el-table-column>
              </el-table>
              <div class="split"></div>
              <dl class="kv" v-if="o.delivery">
                <dt>交车时间</dt><dd class="num">{{ fmtTime(o.delivery.deliveryTime) }}</dd>
                <dt>交车里程</dt><dd class="num">{{ num(o.delivery.mileage) }} km</dd>
                <dt>验收结果</dt><dd>{{ o.delivery.acceptResult === 'ACCEPTED' ? '客户验收通过' : o.delivery.acceptResult }}</dd>
                <dt>旧件</dt><dd>{{ o.delivery.oldPartTaken }}</dd>
              </dl>
              <div v-else class="empty">尚未交车</div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>流转轨迹</h3><span class="sub">留痕不可改</span></div>
          <div class="card-b">
            <div class="timeline">
              <div v-for="t in o.timeline || []" :key="t.id" class="tl-item">
                <div class="tt">{{ t.action }}</div>
                <div class="td">{{ t.detail }}</div>
                <div class="td">{{ t.username }} · {{ fmtTime(t.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-h"><h3>金额汇总</h3></div>
          <div class="card-b">
            <dl class="kv">
              <dt>工时费</dt><dd class="num">{{ money(o.currentQuote?.totalLabor) }}</dd>
              <dt>配件费</dt><dd class="num">{{ money(o.currentQuote?.totalPart) }}</dd>
              <dt>优惠</dt><dd class="num">{{ money(o.discount) }}</dd>
              <dt>应收</dt><dd class="num strong">{{ money(o.totalAmount) }}</dd>
              <dt>已收</dt><dd class="num">{{ money(o.paidAmount) }}</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StatusTag from '../components/StatusTag.vue'
import http from '../http'
import { label } from '../dict'
import { fmtDate, fmtTime, money, num } from '../utils/format'

const route = useRoute()
const router = useRouter()
const o = ref({})
const tab = ref('over')

// 状态机主链路（返工与取消为异常分支，单独用标签体现）
const STEPS = [
  { v: 'RECEIVE', l: '接车登记' }, { v: 'DIAGNOSIS', l: '故障诊断' }, { v: 'QUOTE_CONFIRM', l: '报价确认' },
  { v: 'REPAIR_PART', l: '维修领料' }, { v: 'QC', l: '质量检验' }, { v: 'SETTLE', l: '交车结算' },
  { v: 'FINISHED', l: '已交车' }
]

function stepClass(v) {
  const cur = STEPS.findIndex((s) => s.v === o.value.status)
  const idx = STEPS.findIndex((s) => s.v === v)
  if (o.value.status === 'CANCELED') return ''
  if (idx === cur) return 'on'
  return idx < cur ? 'done' : ''
}

onMounted(async () => {
  o.value = await http.get(`/order/${route.params.id}`)
})
</script>

<style scoped>
.tabs :deep(.el-tabs__header) { margin: 0 0 10px; padding: 0 16px; }
.tabs :deep(.el-tabs__item) { font-size: 13px; height: 42px; }
.tabs :deep(.el-tabs__content) { padding: 0 16px 16px; }
</style>
