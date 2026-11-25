<template>
  <div>
    <div class="page-head">
      <div><h1>维修进度</h1><p>工单每一步流转都会记录，交车后可对服务进行评价</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>我的工单</h3><span class="sub">共 {{ total }} 单</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="orderNo" label="工单号" min-width="170" />
        <el-table-column prop="plateNo" label="车牌" min-width="105" />
        <el-table-column prop="vehicleName" label="车型" min-width="165" show-overflow-tooltip />
        <el-table-column label="状态" min-width="115">
          <template #default="{ row }"><StatusTag :status="row.status" :name="row.statusName" /></template>
        </el-table-column>
        <el-table-column label="接车" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.receiveTime) }}</span></template>
        </el-table-column>
        <el-table-column label="预计交车" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.expectFinish) }}</span></template>
        </el-table-column>
        <el-table-column prop="faultDesc" label="故障描述" min-width="230" show-overflow-tooltip />
        <el-table-column label="金额" min-width="110" align="right">
          <template #default="{ row }"><span class="num strong">{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="detail(row)">进度</el-button>
            <el-button v-if="row.status === 'FINISHED'" link type="primary" @click="openReview(row)">评价</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @change="load" />
      </div>
    </div>

    <el-drawer v-model="dlg" title="维修进度" size="520px">
      <template v-if="cur.id">
        <div class="steps" style="margin-bottom:14px">
          <div v-for="s in STEPS" :key="s.v" class="step" :class="stepClass(s.v)">{{ s.l }}</div>
        </div>
        <dl class="kv">
          <dt>工单号</dt><dd class="num">{{ cur.orderNo }}</dd>
          <dt>状态</dt><dd>{{ cur.statusName }}</dd>
          <dt>接车时间</dt><dd class="num">{{ fmtTime(cur.receiveTime) }}</dd>
          <dt>预计交车</dt><dd class="num">{{ fmtTime(cur.expectFinish) }}</dd>
          <dt>故障描述</dt><dd>{{ cur.faultDesc }}</dd>
        </dl>
        <div class="split"></div>
        <div class="tiny muted">维修项目</div>
        <div v-for="i in cur.items || []" :key="i.id" class="flex" style="justify-content:space-between;margin-top:6px">
          <span>{{ i.projectName }}</span>
          <span class="num tiny muted">{{ i.hours }}h · {{ money(i.laborFee) }}</span>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="reviewDlg" title="服务评价" width="480px">
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="review.score" :max="5" show-score />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="review.content" type="textarea" :rows="3" placeholder="说说这次维修体验" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDlg = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import http from '../../http'
import { fmtTime, money } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const reviewDlg = ref(false)
const cur = ref({})
const q = reactive({ page: 1, size: 10 })
const review = reactive({ orderId: null, score: 5, content: '' })

const STEPS = [
  { v: 'RECEIVE', l: '接车' }, { v: 'DIAGNOSIS', l: '诊断' }, { v: 'QUOTE_CONFIRM', l: '报价' },
  { v: 'REPAIR_PART', l: '维修' }, { v: 'QC', l: '质检' }, { v: 'SETTLE', l: '结算' }, { v: 'FINISHED', l: '交车' }
]

function stepClass(v) {
  const curIdx = STEPS.findIndex((s) => s.v === cur.value.status)
  const idx = STEPS.findIndex((s) => s.v === v)
  if (cur.value.status === 'CANCELED') return ''
  if (idx === curIdx) return 'on'
  return idx < curIdx ? 'done' : ''
}

async function load() {
  const d = await http.get('/order/portal/my', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

async function detail(row) {
  cur.value = await http.get(`/order/${row.id}`)
  dlg.value = true
}

function openReview(row) {
  review.orderId = row.id
  review.score = 5
  review.content = ''
  reviewDlg.value = true
}

async function submitReview() {
  await http.post('/order/review', { ...review })
  ElMessage.success('感谢您的评价')
  reviewDlg.value = false
}

onMounted(load)
</script>
