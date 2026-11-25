<template>
  <div v-if="o.id">
    <div class="page-head">
      <div>
        <h1>故障诊断与检测</h1>
        <p>{{ o.orderNo }} · {{ o.plateNo }} · {{ o.vehicleName }} · {{ o.customerName }}</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.push(`/orders/${o.id}`)">工单详情</el-button>
        <el-button type="primary" :loading="saving" @click="toQuote">完成诊断，转报价</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>检测记录</h3><span class="sub">共 {{ (o.inspections || []).length }} 项</span></div>
          <el-table :data="o.inspections || []" size="default">
            <el-table-column prop="checkItem" label="检查项" min-width="140" />
            <el-table-column label="结果" min-width="110">
              <template #default="{ row }">
                <span class="tag" :class="row.checkResult === 'REPAIR' ? 'danger' : row.checkResult === 'OBSERVE' ? 'warning' : 'success'">
                  {{ label('check_result', row.checkResult) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="严重度" min-width="80">
              <template #default="{ row }">{{ label('severity', row.severity) }}</template>
            </el-table-column>
            <el-table-column prop="faultDesc" label="检测描述" min-width="230" show-overflow-tooltip />
            <el-table-column prop="suggestion" label="处理建议" min-width="150" show-overflow-tooltip />
            <el-table-column label="时间" min-width="135">
              <template #default="{ row }"><span class="num">{{ fmtTime(row.checkTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" min-width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="delInsp(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card">
          <div class="card-h"><h3>维修项目</h3><span class="sub">确认后才可派工与领料</span></div>
          <el-table :data="o.items || []" size="default">
            <el-table-column prop="projectName" label="项目" min-width="170" />
            <el-table-column label="工种" min-width="90">
              <template #default="{ row }">{{ label('work_type', row.workType) }}</template>
            </el-table-column>
            <el-table-column prop="hours" label="工时" min-width="70" align="right" />
            <el-table-column label="工时费" min-width="100" align="right">
              <template #default="{ row }"><span class="num">{{ money(row.laborFee) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" min-width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="delItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>新增检测项</h3></div>
          <div class="card-b">
            <el-form :model="insp" label-width="88px">
              <el-form-item label="检查项" required>
                <el-select v-model="insp.checkItem" filterable allow-create style="width:100%"
                           placeholder="如：制动系统">
                  <el-option v-for="i in CHECK_ITEMS" :key="i" :label="i" :value="i" />
                </el-select>
              </el-form-item>
              <el-form-item label="检测结果" required>
                <el-radio-group v-model="insp.checkResult">
                  <el-radio v-for="c in options('check_result')" :key="c.value" :value="c.value">{{ c.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="严重度">
                <el-select v-model="insp.severity" style="width:100%">
                  <el-option v-for="s in options('severity')" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="检测描述">
                <el-input v-model="insp.faultDesc" type="textarea" :rows="2" placeholder="如：刹车片厚度 6.2mm" />
              </el-form-item>
              <el-form-item label="处理建议">
                <el-input v-model="insp.suggestion" placeholder="如：建议更换刹车片" />
              </el-form-item>
              <el-button type="primary" style="width:100%" @click="addInsp">添加检测项</el-button>
            </el-form>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>添加维修项目</h3></div>
          <div class="card-b">
            <el-form :model="item" label-width="88px">
              <el-form-item label="项目" required>
                <el-select v-model="item.projectId" filterable style="width:100%" placeholder="从项目库选择">
                  <el-option v-for="p in projects" :key="p.id"
                             :label="`${p.projectName} · ${p.stdHours}h · ${p.laborFee}元`" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="数量">
                <el-input v-model.number="item.qty" />
              </el-form-item>
              <el-button type="primary" style="width:100%" @click="addItem">加入工单</el-button>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime, money } from '../utils/format'

const route = useRoute()
const router = useRouter()
const o = ref({})
const projects = ref([])
const saving = ref(false)
const CHECK_ITEMS = ['制动系统', '发动机工况', '变速箱', '转向系统', '悬挂系统', '电控系统', '空调系统', '轮胎与四轮定位']
const insp = reactive({ checkItem: '', checkResult: 'NORMAL', severity: 'LOW', faultDesc: '', suggestion: '' })
const item = reactive({ projectId: null, qty: 1 })

async function load() {
  o.value = await http.get(`/order/${route.params.id}`)
}

async function addInsp() {
  if (!insp.checkItem) return ElMessage.warning('请选择检查项')
  await http.post('/order/inspection', { orderId: o.value.id, ...insp })
  ElMessage.success('检测项已记录')
  Object.assign(insp, { checkItem: '', checkResult: 'NORMAL', severity: 'LOW', faultDesc: '', suggestion: '' })
  load()
}

async function delInsp(row) {
  await ElMessageBox.confirm(`删除检测项「${row.checkItem}」？`, '提示', { type: 'warning' })
  await http.delete(`/order/inspection/${row.id}`)
  load()
}

async function addItem() {
  if (!item.projectId) return ElMessage.warning('请选择维修项目')
  await http.post('/order/item', { orderId: o.value.id, projectId: item.projectId, qty: item.qty, hours: null, techId: null })
  ElMessage.success('项目已加入')
  item.projectId = null
  load()
}

async function delItem(row) {
  await ElMessageBox.confirm(`移除项目「${row.projectName}」？`, '提示', { type: 'warning' })
  await http.delete(`/order/item/${row.id}`)
  load()
}

async function toQuote() {
  if (!(o.value.inspections || []).length) return ElMessage.warning('请先记录至少 1 条检测项')
  if (!(o.value.items || []).length) return ElMessage.warning('请至少添加 1 个维修项目')
  saving.value = true
  try {
    await http.post(`/order/${o.value.id}/transition`, { targetStatus: 'QUOTE_CONFIRM', reason: null })
    ElMessage.success('已转入报价确认')
    router.push(`/quote/${o.value.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
  projects.value = (await http.get('/meta/project/list')) || []
})
</script>
