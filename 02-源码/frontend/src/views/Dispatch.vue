<template>
  <div>
    <div class="page-head">
      <div>
        <h1>派工看板</h1>
        <p>按工单分配技师与计划工时，实际工时由技师记录</p>
      </div>
      <div class="head-acts">
        <el-button @click="loadOrders">刷新</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h">
            <h3>待派工工单</h3>
            <span class="sub">维修领料 / 质检阶段</span>
          </div>
          <el-table :data="orders" size="default" highlight-current-row @row-click="pick">
            <el-table-column prop="orderNo" label="工单号" min-width="160" />
            <el-table-column prop="plateNo" label="车牌" min-width="105" />
            <el-table-column prop="vehicleName" label="车型" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" min-width="110">
              <template #default="{ row }"><StatusTag :status="row.status" :name="row.statusName" /></template>
            </el-table-column>
            <el-table-column prop="techName" label="主修" min-width="90">
              <template #default="{ row }">{{ row.techName || '未派' }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="pick(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card" v-if="cur.id">
          <div class="card-h">
            <h3>{{ cur.orderNo }} · 派工明细</h3>
            <span class="sub">{{ cur.plateNo }} · {{ cur.customerName }}</span>
          </div>
          <el-table :data="cur.dispatches || []" size="default">
            <el-table-column label="项目" min-width="180">
              <template #default="{ row }">{{ itemName(row.itemId) }}</template>
            </el-table-column>
            <el-table-column label="工种" min-width="90">
              <template #default="{ row }">{{ label('work_type', row.workType) }}</template>
            </el-table-column>
            <el-table-column label="技师" min-width="100">
              <template #default="{ row }">{{ techName(row.techId) }}</template>
            </el-table-column>
            <el-table-column prop="planHours" label="计划工时" min-width="90" align="right" />
            <el-table-column prop="actualHours" label="实际工时" min-width="90" align="right">
              <template #default="{ row }"><span class="num">{{ row.actualHours ?? '—' }}</span></template>
            </el-table-column>
            <el-table-column label="状态" min-width="100">
              <template #default="{ row }">
                <span class="tag" :class="row.status === 'DONE' ? 'success' : row.status === 'WORKING' ? 'primary' : 'info'">
                  {{ { WAIT: '待开工', WORKING: '施工中', DONE: '已完工' }[row.status] || row.status }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="150" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'WAIT'" link type="primary" @click="log(row, 'START')">开工</el-button>
                <el-button v-if="row.status === 'WORKING'" link type="primary" @click="log(row, 'FINISH')">完工</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>未派工项目</h3><span class="sub">客户已确认</span></div>
          <el-table :data="pendingItems" size="default">
            <el-table-column prop="projectName" label="项目" min-width="150" show-overflow-tooltip />
            <el-table-column prop="hours" label="工时" min-width="70" align="right" />
            <el-table-column label="操作" min-width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDispatch(row)">派工</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!pendingItems.length" class="empty">当前工单没有待派工项目</div>
        </div>

        <div class="card">
          <div class="card-h"><h3>技师负荷</h3></div>
          <div class="card-b">
            <div v-for="t in techs" :key="t.id" class="flex" style="justify-content:space-between;padding:6px 0">
              <div class="flex gap">
                <img :src="img(t.avatar, '/uploads/seed/avatar/01.jpg')"
                     style="width:24px;height:24px;border-radius:50%;object-fit:cover" />
                <span>{{ t.realName }}</span>
              </div>
              <span class="tiny muted">{{ loadOf(t.id) }} 个在办</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dlg" title="派工" width="480px">
      <el-form label-width="96px">
        <el-form-item label="维修项目"><span class="strong">{{ pickItem?.projectName }}</span></el-form-item>
        <el-form-item label="指派技师" required>
          <el-select v-model="dispatchForm.techId" style="width:100%">
            <el-option v-for="t in techs" :key="t.id" :label="`${t.realName} · ${loadOf(t.id)} 个在办`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划工时">
          <el-input v-model.number="dispatchForm.planHours" type="number" step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="doDispatch">确认派工</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import http from '../http'
import { label } from '../dict'
import { img } from '../utils/format'

const orders = ref([])
const cur = ref({})
const techs = ref([])
const allOrders = ref([])
const dlg = ref(false)
const pickItem = ref(null)
const dispatchForm = reactive({ techId: null, planHours: 1 })

const pendingItems = computed(() => {
  const dispatched = new Set((cur.value.dispatches || []).map((d) => d.itemId))
  return (cur.value.items || []).filter((i) => !dispatched.has(i.id))
})

const itemName = (id) => (cur.value.items || []).find((i) => i.id === id)?.projectName || `项目#${id}`
const techName = (id) => techs.value.find((t) => t.id === id)?.realName || `技师#${id}`
const loadOf = (id) => allOrders.value.reduce((n, o) =>
  n + (o.techId === id || (o.dispatches || []).some((d) => d.techId === id && d.status !== 'DONE') ? 1 : 0), 0)

async function loadOrders() {
  const d = await http.get('/order/page', { params: { page: 1, size: 100 } })
  allOrders.value = d.records || []
  orders.value = allOrders.value.filter((o) => ['REPAIR_PART', 'QC'].includes(o.status))
  if (!cur.value.id && orders.value.length) pick(orders.value[0])
  else if (cur.value.id) pick(cur.value)
}

async function pick(row) {
  cur.value = await http.get(`/order/${row.id}`)
}

function openDispatch(item) {
  pickItem.value = item
  dispatchForm.techId = techs.value[0]?.id
  dispatchForm.planHours = item.hours || 1
  dlg.value = true
}

async function doDispatch() {
  await http.post('/order/dispatch', { itemId: pickItem.value.id, techId: dispatchForm.techId, planHours: dispatchForm.planHours })
  ElMessage.success('已派工')
  dlg.value = false
  pick(cur.value)
}

async function log(row, action) {
  if (action === 'FINISH') {
    const { value } = await ElMessageBox.prompt('请输入实际工时（小时）', '完工登记', {
      inputValue: String(row.planHours || 1),
      inputPattern: /^\d+(\.\d+)?$/,
      inputErrorMessage: '请输入数字'
    })
    await http.post(`/order/dispatch/${row.id}/worklog`, { action: 'FINISH', hours: Number(value), remark: '技师完工登记' })
  } else {
    await http.post(`/order/dispatch/${row.id}/worklog`, { action: 'START', hours: null, remark: '技师开工' })
  }
  ElMessage.success('已记录')
  pick(cur.value)
}

onMounted(async () => {
  techs.value = (await http.get('/meta/staff/list')) || []
  await loadOrders()
})
</script>
