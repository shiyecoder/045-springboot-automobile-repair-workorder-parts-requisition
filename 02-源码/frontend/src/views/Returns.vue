<template>
  <div>
    <div class="page-head">
      <div><h1>返修记录</h1><p>质保期内返修需判定责任，门店责任不计费</p></div>
      <div class="head-acts"><el-button type="primary" @click="dlg = true">新建返修</el-button></div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="返修状态">
          <el-select v-model="q.status" clearable placeholder="全部状态" @change="load">
            <el-option label="待处理" value="WAIT" />
            <el-option label="返修中" value="DOING" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任判定">
          <el-select v-model="q.duty" clearable placeholder="全部" @change="load">
            <el-option v-for="o in options('duty_result')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>返修单</h3><span class="sub">共 {{ total }} 条</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="returnNo" label="返修单号" min-width="165" />
        <el-table-column prop="orderNo" label="原工单" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.orderId}`)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="plateNo" label="车牌" min-width="105" />
        <el-table-column prop="itemName" label="返修项目" min-width="160" show-overflow-tooltip />
        <el-table-column prop="problemDesc" label="问题描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="责任判定" min-width="110">
          <template #default="{ row }">
            <span class="tag" :class="row.dutyResult === 'STORE' ? 'danger' : row.dutyResult === 'WAIT' ? 'warning' : 'info'">
              {{ label('duty_result', row.dutyResult) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 'DONE' ? 'success' : row.status === 'DOING' ? 'primary' : 'warning'">
              {{ { WAIT: '待处理', DOING: '返修中', DONE: '已完成' }[row.status] || row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="reworkTimes" label="返修次数" min-width="95" align="right" />
        <el-table-column label="建立时间" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDuty(row)">责任判定</el-button>
            <el-button link type="primary" :disabled="row.status === 'DONE'" @click="finish(row)">完成返修</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新建返修" width="580px">
      <el-form :model="form" label-width="104px">
        <el-form-item label="原工单" required>
          <el-select v-model="form.orderId" filterable style="width:100%" placeholder="选择已交车工单">
            <el-option v-for="o in doneOrders" :key="o.id" :label="`${o.orderNo} · ${o.plateNo}`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="form.problemDesc" type="textarea" :rows="3" placeholder="不少于 10 个字，描述故障复现情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">提交返修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dutyDlg" title="责任判定" width="520px">
      <el-form label-width="96px">
        <el-form-item label="判定结果">
          <el-select v-model="duty.dutyResult" style="width:100%">
            <el-option v-for="o in options('duty_result')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="判定说明" required>
          <el-input v-model="duty.reason" type="textarea" :rows="3" placeholder="不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dutyDlg = false">取消</el-button>
        <el-button type="primary" @click="saveDuty">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const doneOrders = ref([])
const dlg = ref(false)
const dutyDlg = ref(false)
const curId = ref(null)
const q = reactive({ page: 1, size: 10, status: '', duty: '' })
const form = reactive({ orderId: null, problemDesc: '' })
const duty = reactive({ dutyResult: 'STORE', reason: '' })

async function load() {
  const d = await http.get('/meta/return/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

function reset() {
  q.status = ''
  q.duty = ''
  q.page = 1
  load()
}

async function save() {
  if (!form.orderId || (form.problemDesc || '').length < 10) {
    return ElMessage.warning('请选择工单并填写不少于 10 字的问题描述')
  }
  await http.post('/order/return', { ...form })
  ElMessage.success('返修单已创建')
  dlg.value = false
  load()
}

function openDuty(row) {
  curId.value = row.id
  duty.dutyResult = 'STORE'
  duty.reason = ''
  dutyDlg.value = true
}

async function saveDuty() {
  if ((duty.reason || '').length < 5) return ElMessage.warning('判定说明不少于 5 个字')
  await http.post(`/order/return/${curId.value}/duty`, { ...duty })
  ElMessage.success('责任判定已保存')
  dutyDlg.value = false
  load()
}

async function finish(row) {
  await http.post(`/order/return/${row.id}/finish`, {})
  ElMessage.success('返修已完成')
  load()
}

onMounted(async () => {
  const d = await http.get('/order/page', { params: { page: 1, size: 200 } })
  doneOrders.value = (d.records || []).filter((o) => o.status === 'FINISHED')
  load()
})
</script>
