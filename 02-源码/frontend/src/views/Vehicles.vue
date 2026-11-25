<template>
  <div>
    <div class="page-head">
      <div>
        <h1>维修车辆</h1>
        <p>车辆档案与最近一次维修状态，点击查看全部历史工单</p>
      </div>
      <div class="head-acts">
        <el-button type="primary" @click="dlg = true">新增车辆</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="车辆状态">
          <el-select v-model="q.status" clearable placeholder="全部状态" @change="load">
            <el-option v-for="o in options('vehicle_status')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="车牌 / 车型 / 客户" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>车辆列表</h3><span class="sub">共 {{ total }} 台</span></div>
      <el-table :data="rows" size="default">
        <el-table-column label="照片" width="72">
          <template #default="{ row }">
            <Thumb :src="row.vehicle.photo" fallback="/uploads/seed/vehicle/01.jpg" />
          </template>
        </el-table-column>
        <el-table-column prop="vehicle.plateNo" label="车牌" min-width="110" />
        <el-table-column label="车型" min-width="175">
          <template #default="{ row }">{{ row.vehicle.brand }} {{ row.vehicle.model }}</template>
        </el-table-column>
        <el-table-column prop="vehicle.vin" label="VIN" min-width="180">
          <template #default="{ row }"><span class="num tiny muted">{{ row.vehicle.vin }}</span></template>
        </el-table-column>
        <el-table-column prop="customerName" label="车主" min-width="95" />
        <el-table-column prop="customerPhone" label="联系电话" min-width="125" />
        <el-table-column label="里程" min-width="110" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.vehicle.mileage) }} km</span></template>
        </el-table-column>
        <el-table-column label="最近工单" min-width="165">
          <template #default="{ row }">
            <span v-if="row.lastOrderNo" class="num tiny">{{ row.lastOrderNo }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="最近状态" min-width="120">
          <template #default="{ row }">
            <StatusTag v-if="row.lastStatus" :status="row.lastStatus" :name="row.lastStatusName" />
            <span v-else class="muted tiny">暂无维修</span>
          </template>
        </el-table-column>
        <el-table-column label="结算" min-width="95">
          <template #default="{ row }">
            <span class="tag" :class="row.settleStatus === 'UNPAID' ? 'warning' : 'success'">
              {{ row.settleStatus === 'UNPAID' ? '未结清' : '已结清' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/vehicles/${row.vehicle.id}`)">详情</el-button>
            <el-button link type="primary" @click="router.push(`/reception/new?vehicleId=${row.vehicle.id}`)">接车</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新增车辆" width="620px">
      <el-form :model="form" label-width="104px">
        <el-form-item label="车牌号" required><el-input v-model="form.plateNo" placeholder="粤B·12345" /></el-form-item>
        <div class="grid-2">
          <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
          <el-form-item label="车型"><el-input v-model="form.model" /></el-form-item>
        </div>
        <el-form-item label="VIN"><el-input v-model="form.vin" maxlength="17" /></el-form-item>
        <div class="grid-2">
          <el-form-item label="发动机号"><el-input v-model="form.engineNo" /></el-form-item>
          <el-form-item label="当前里程"><el-input v-model.number="form.mileage" /></el-form-item>
        </div>
        <el-form-item label="购车日期">
          <el-date-picker v-model="form.buyDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="所属客户" required>
          <el-select v-model="form.customerId" filterable style="width:100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name} · ${c.phone}`" :value="c.id" />
          </el-select>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import Thumb from '../components/Thumb.vue'
import http from '../http'
import { options } from '../dict'
import { num } from '../utils/format'

const route = useRoute()
const router = useRouter()
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const customers = ref([])
const q = reactive({ page: 1, size: 10, status: '', keyword: '' })
const form = reactive({ plateNo: '', brand: '', model: '', vin: '', engineNo: '', mileage: null, buyDate: '', customerId: null })

async function load() {
  const d = await http.get('/meta/vehicle/page', { params: { ...q } })
  let list = d.records || []
  if (q.status) list = list.filter((r) => r.vehicle.vehicleStatus === q.status)
  if (route.query.customerId) list = list.filter((r) => String(r.vehicle.customerId) === String(route.query.customerId))
  rows.value = list
  total.value = d.total || 0
}

function reset() {
  Object.assign(q, { status: '', keyword: '', page: 1 })
  load()
}

async function save() {
  if (!form.plateNo || !form.customerId) return ElMessage.warning('请填写车牌与所属客户')
  await http.post('/meta/vehicle', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

onMounted(async () => {
  const cs = await http.get('/meta/customer/page', { params: { page: 1, size: 200 } })
  customers.value = cs.records || []
  load()
})
</script>
