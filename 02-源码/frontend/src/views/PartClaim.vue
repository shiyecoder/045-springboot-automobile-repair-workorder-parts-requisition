<template>
  <div v-if="o.id">
    <div class="page-head">
      <div>
        <h1>领用与预检</h1>
        <p>{{ o.orderNo }} · {{ o.plateNo }} · {{ o.vehicleModel }} · 客户已确认 {{ (o.items || []).length }} 个项目</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.push(`/orders/${o.id}`)">工单详情</el-button>
        <el-button type="primary" :loading="evaluating" @click="evaluate">执行领用预检</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h">
            <h3>预检结果（创新点）</h3>
            <span class="sub" v-if="score">规则 {{ score.ruleVersion }} · {{ fmtTime(score.createTime) }}</span>
          </div>
          <div class="card-b" v-if="score">
            <div class="precheck">
              <div class="hd">
                <div>
                  <div class="tiny muted">综合得分 / 100</div>
                  <div class="score" :style="{ color: levelColor }">{{ score.totalScore }}</div>
                </div>
                <div style="text-align:right">
                  <div class="verdict" :class="score.resultLevel">{{ levelText(score.resultLevel) }}</div>
                  <div class="tiny muted">阈值：≥{{ high }} 推荐 · &lt;{{ low }} 不推荐</div>
                </div>
              </div>
              <div class="card-b">
                <div v-for="f in factors" :key="f.name" class="factor">
                  <span>{{ f.name }}（{{ f.weight }}分）</span>
                  <div class="bar"><span :style="{ width: f.percent + '%' }"></span></div>
                  <span class="num tiny">{{ f.score }} / {{ f.weight }}</span>
                </div>
                <div class="split"></div>
                <div class="tiny" style="line-height:1.9">
                  <b>结果解释：</b>{{ score.explainText }}<br />
                  <b>未满足条件：</b>{{ score.missText }}<br />
                  <b>改进建议：</b>{{ score.improveText }}
                </div>
                <div class="split"></div>
                <div class="flex gap">
                  <el-select v-model="manual.manualResult" style="width:180px" placeholder="人工修正为">
                    <el-option label="推荐领用" value="RECOMMEND" />
                    <el-option label="谨慎处理" value="CAUTION" />
                    <el-option label="不推荐" value="REJECT" />
                  </el-select>
                  <el-input v-model="manual.manualReason" style="flex:1" placeholder="修正原因（不少于 5 字）" />
                  <el-button @click="doManual">提交修正</el-button>
                </div>
                <div class="tiny muted mt8" v-if="score.manualResult">
                  已人工修正为 <b>{{ levelText(score.manualResult) }}</b>（{{ score.manualBy }}）原因：{{ score.manualReason }}。
                  原始计算结果完整保留，不可覆盖。
                </div>
              </div>
            </div>
          </div>
          <div v-else class="card-b">
            <div class="empty">尚未执行预检，点击右上角「执行领用预检」按规则计算推荐领用清单</div>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>推荐领用清单</h3><span class="sub">库存不足会标红缺口</span></div>
          <el-table :data="demands" size="default" empty-text="预检后会自动生成推荐领用清单">
            <el-table-column label="配件" min-width="180">
              <template #default="{ row }">{{ partName(row.partId) }}</template>
            </el-table-column>
            <el-table-column label="编码" min-width="110">
              <template #default="{ row }"><span class="num tiny">{{ partCode(row.partId) }}</span></template>
            </el-table-column>
            <el-table-column prop="demandQty" label="建议数量" min-width="90" align="right" />
            <el-table-column prop="stockQty" label="当前库存" min-width="90" align="right" />
            <el-table-column label="缺口" min-width="80" align="right">
              <template #default="{ row }">
                <span class="num" :style="{ color: row.gapQty > 0 ? 'var(--dan)' : 'inherit' }">{{ row.gapQty }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="来源" min-width="100">
              <template #default="{ row }">{{ row.source === 'PRECHECK' ? '预检推荐' : '人工添加' }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status === 'ISSUED'" @click="openClaim(row)">
                  {{ row.status === 'ISSUED' ? '已领用' : '领用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

        </div>

        <div class="card">
          <div class="card-h"><h3>本单领退记录</h3></div>
          <el-table :data="o.flows || []" size="default">
            <el-table-column prop="flowNo" label="单号" min-width="160" />
            <el-table-column label="配件" min-width="150">
              <template #default="{ row }">{{ partName(row.partId) }}</template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" min-width="80" align="right" />
            <el-table-column label="类型" min-width="90">
              <template #default="{ row }">{{ label('part_flow_type', row.flowType) }}</template>
            </el-table-column>
            <el-table-column prop="oldPartHandle" label="旧件处理" min-width="120" />
            <el-table-column label="时间" min-width="140">
              <template #default="{ row }"><span class="num">{{ fmtTime(row.flowTime) }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>领用登记</h3></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item label="配件" required>
                <el-select v-model="claim.partId" filterable style="width:100%" @change="onPart">
                  <el-option v-for="p in parts" :key="p.id"
                             :label="`${p.partName}（库存 ${p.stockQty}${p.unit}）`" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="数量" required>
                <el-input v-model.number="claim.qty" type="number" />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="claim.flowType" style="width:100%">
                  <el-option v-for="f in options('part_flow_type')" :key="f.value" :label="f.label" :value="f.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="批次">
                <el-select v-model="claim.batchId" clearable style="width:100%" placeholder="可不指定（先进先出）">
                  <el-option v-for="b in batches" :key="b.batch.id"
                             :label="`${b.batch.batchNo} · 余 ${b.batch.leftQty}`" :value="b.batch.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="技师">
                <el-select v-model="claim.techId" style="width:100%">
                  <el-option v-for="t in techs" :key="t.id" :label="t.realName" :value="t.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="旧件处理">
                <el-input v-model="claim.oldPartHandle" placeholder="如：废油回收 / 交客户" />
              </el-form-item>
              <el-button type="primary" style="width:100%" @click="doClaim">确认领用</el-button>
            </el-form>
            <div class="split"></div>
            <div class="tiny muted" style="line-height:1.9">
              硬规则：领用必须关联工单，库存不足后端直接拒绝并返回缺口数量；领用后自动扣减批次余量与配件总库存。
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const o = ref({})
const score = ref(null)
const factors = ref([])
const demands = ref([])
const parts = ref([])
const batches = ref([])
const techs = ref([])
const rules = ref([])
const evaluating = ref(false)
const manual = reactive({ manualResult: '', manualReason: '' })
const claim = reactive({ partId: null, qty: 1, flowType: 'CLAIM', batchId: null, techId: null, oldPartHandle: '废料回收' })

const high = computed(() => rules.value[0]?.thresholdHigh ?? 80)
const low = computed(() => rules.value[0]?.thresholdLow ?? 60)
const levelText = (v) => ({ RECOMMEND: '推荐领用', CAUTION: '谨慎处理', REJECT: '不推荐领用' }[v] || v)
const levelColor = computed(() => ({ RECOMMEND: 'var(--ok)', CAUTION: 'var(--warn)', REJECT: 'var(--dan)' }[score.value?.resultLevel] || 'var(--txt)'))
const partName = (id) => parts.value.find((p) => p.id === id)?.partName || `配件#${id}`
const partCode = (id) => parts.value.find((p) => p.id === id)?.partCode || '—'

/** 由分数记录与当前规则还原六项因素（历史接口只回分数，不含权重，权重取自规则） */
function buildFactors(sc) {
  const r = rules.value[0] || {}
  const map = [
    ['车型', r.wmodel, sc.scoreModel],
    ['故障标签', r.wfault, sc.scoreFault],
    ['诊断项目', r.wproject, sc.scoreProject],
    ['历史工时', r.whours, sc.scoreHours],
    ['常用配件', r.wpart, sc.scorePart],
    ['当前库存', r.wstock, sc.scoreStock]
  ]
  return map.filter((m) => Number(m[1]) > 0).map(([name, weight, got]) => ({
    name, weight: Number(weight), score: Number(got || 0),
    percent: Math.min(100, Math.round((Number(got || 0) / Number(weight)) * 100))
  }))
}

async function load() {
  o.value = await http.get(`/order/${route.params.id}`)
  if (o.value.vehicle) o.value.vehicleModel = o.value.vehicle.brand + ' ' + o.value.vehicle.model
  // history 按 id 倒序返回，第一条即最新一次预检
  const h = await http.get(`/precheck/history/${o.value.id}`)
  if (Array.isArray(h) && h.length) {
    score.value = h[0]
    factors.value = buildFactors(h[0])
    demands.value = (await http.get(`/precheck/demands/${o.value.id}`)) || []
  } else {
    score.value = null
    factors.value = []
    demands.value = []
  }
}

async function evaluate() {
  evaluating.value = true
  try {
    const res = await http.post(`/precheck/${o.value.id}/evaluate`, {})
    score.value = res.score
    factors.value = (res.factors && res.factors.length) ? res.factors : buildFactors(res.score)
    demands.value = res.demands || []
    ElMessage.success(`预检完成：${res.score.totalScore} 分 · ${levelText(res.score.resultLevel)}`)
  } finally {
    evaluating.value = false
  }
}

async function doManual() {
  if (!manual.manualResult || (manual.manualReason || '').length < 5) {
    return ElMessage.warning('请选择修正结果并填写不少于 5 字的原因')
  }
  await http.post(`/precheck/score/${score.value.id}/manual`, { ...manual })
  ElMessage.success('人工修正已留痕')
  manual.manualResult = ''
  manual.manualReason = ''
  load()
}

function onPart(id) {
  const p = parts.value.find((x) => x.id === id)
  if (p) claim.qty = 1
}

function openClaim(row) {
  claim.partId = row.partId
  claim.qty = row.demandQty
  claim.flowType = 'CLAIM'
  claim.batchId = null
}

async function doClaim() {
  if (!claim.partId) return ElMessage.warning('请选择配件')
  await http.post('/meta/flow', { orderId: o.value.id, ...claim })
  ElMessage.success('领用成功，库存已扣减')
  load()
}

onMounted(async () => {
  const opts = await http.get('/meta/options')
  parts.value = opts.parts || []
  techs.value = opts.techs || []
  rules.value = (await http.get('/precheck/rules')) || []
  const b = await http.get('/meta/batch/page', { params: { page: 1, size: 100 } })
  batches.value = b.records || []
  claim.techId = techs.value[0]?.id
  await load()
})
</script>
