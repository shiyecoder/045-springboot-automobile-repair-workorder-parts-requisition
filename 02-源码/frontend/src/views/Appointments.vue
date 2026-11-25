<template>
  <div>
    <div class="page-head">
      <div>
        <h1>预约接车台</h1>
        <p>按预约时间排布，到店后一键转为维修工单</p>
      </div>
      <div class="head-acts">
        <el-button type="primary" @click="openCreate">新建预约</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="q.status" clearable placeholder="全部状态" @change="load">
            <el-option label="待到店" value="WAITING" />
            <el-option label="已到店" value="RECEIVED" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="车牌 / 客户姓名" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>预约列表</h3><span class="sub">共 {{ total }} 条</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="appointment.appointNo" label="预约单号" min-width="170" />
        <el-table-column prop="plateNo" label="车牌" min-width="110" />
        <el-table-column prop="vehicleName" label="车型" min-width="160" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="100" />
        <el-table-column prop="customerPhone" label="联系电话" min-width="130" />
        <el-table-column label="预约到店时间" min-width="150">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.appointment.expectTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="appointment.problemType" label="业务类型" min-width="110" />
        <el-table-column prop="advisorName" label="服务顾问" min-width="100" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span class="tag" :class="apptColor(row.appointment.status)">{{ apptLabel(row.appointment.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="toReception(row)">接车建单</el-button>
            <el-button link type="danger" :disabled="row.appointment.status === 'CANCELED'"
                       @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新建预约" width="620px">
      <el-form :model="form" label-width="104px">
        <el-form-item label="车辆" required>
          <el-select v-model="form.vehicleId" filterable placeholder="按车牌搜索" style="width:100%">
            <el-option v-for="v in vehicles" :key="v.id" :label="`${v.plateNo} · ${v.brand} ${v.model}`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间" required>
          <el-date-picker v-model="form.expectTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择到店时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="form.appointType" style="width:100%">
            <el-option v-for="o in options('order_type')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="form.problemType" placeholder="如：事故维修 / 常规保养" />
        </el-form-item>
        <el-form-item label="服务顾问">
          <el-select v-model="form.advisorId" style="width:100%">
            <el-option v-for="a in advisors" :key="a.id" :label="a.realName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../http'
import { options } from '../dict'
import { fmtTime } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const vehicles = ref([])
const advisors = ref([])
const q = reactive({ page: 1, size: 10, status: '', keyword: '' })
const form = reactive({ vehicleId: null, expectTime: '', appointType: 'MAINTAIN', problemType: '', advisorId: null, remark: '' })

const apptLabel = (s) => ({ WAITING: '待到店', RECEIVED: '已到店', CANCELED: '已取消' }[s] || s)
const apptColor = (s) => ({ WAITING: 'warning', RECEIVED: 'success', CANCELED: 'info' }[s] || 'info')

async function load() {
  const d = await http.get('/meta/appointment/page', { params: { ...q } })
  rows.value = (d.records || []).filter((r) => {
    if (q.status && r.appointment.status !== q.status) return false
    if (q.keyword && !(r.plateNo || '').includes(q.keyword) && !(r.customerName || '').includes(q.keyword)) return false
    return true
  })
  total.value = d.total || 0
}

function reset() {
  q.status = ''
  q.keyword = ''
  q.page = 1
  load()
}

async function openCreate() {
  const o = await http.get('/meta/options')
  vehicles.value = o.vehicles || []
  advisors.value = o.advisors || []
  Object.assign(form, { vehicleId: null, expectTime: '', appointType: 'MAINTAIN', problemType: '', advisorId: advisors.value[0]?.id, remark: '' })
  dlg.value = true
}

async function save() {
  if (!form.vehicleId || !form.expectTime) {
    ElMessage.warning('请填写车辆与预约时间')
    return
  }
  await http.post('/meta/appointment', { ...form })
  ElMessage.success('预约已创建')
  dlg.value = false
  load()
}

async function cancel(row) {
  await ElMessageBox.confirm(`确认取消预约 ${row.appointment.appointNo}？`, '取消预约', { type: 'warning' })
  await http.post(`/meta/appointment/${row.appointment.id}/cancel`)
  ElMessage.success('已取消')
  load()
}

function toReception(row) {
  router.push({ path: '/reception/new', query: { vehicleId: row.appointment.vehicleId, appointmentId: row.appointment.id } })
}

onMounted(load)
</script>
