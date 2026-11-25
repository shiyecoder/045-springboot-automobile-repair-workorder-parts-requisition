<template>
  <div>
    <div class="page-head">
      <div>
        <h1>维修工单</h1>
        <p>全部工单按状态流转：接车 → 诊断 → 报价确认 → 维修领料 → 质检 → 交车结算</p>
      </div>
      <div class="head-acts">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="router.push('/reception/new')">接车登记</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="工单状态">
          <el-select v-model="q.status" clearable placeholder="全部状态" @change="load">
            <el-option v-for="o in options('order_status')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="q.orderType" clearable placeholder="全部类型" @change="load">
            <el-option v-for="o in options('order_type')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="工单号 / 车牌 / 客户" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h">
        <h3>工单列表</h3>
        <span class="sub">共 {{ total }} 单</span>
      </div>
      <el-table :data="rows" size="default">
        <el-table-column prop="orderNo" label="工单号" min-width="165">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="plateNo" label="车牌" min-width="105" />
        <el-table-column prop="vehicleName" label="车型" min-width="155" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="90" />
        <el-table-column label="业务类型" min-width="100">
          <template #default="{ row }">{{ label('order_type', row.orderType) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }"><StatusTag :status="row.status" :name="row.statusName" /></template>
        </el-table-column>
        <el-table-column prop="techName" label="主修技师" min-width="95">
          <template #default="{ row }">{{ row.techName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="advisorName" label="服务顾问" min-width="95" />
        <el-table-column label="接车时间" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.receiveTime) }}</span></template>
        </el-table-column>
        <el-table-column label="应收 / 已收" min-width="165" align="right">
          <template #default="{ row }">
            <span class="num strong">{{ money(row.totalAmount) }}</span>
            <span class="num muted" style="margin-left:6px">{{ money(row.paidAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'DIAGNOSIS'" link type="primary"
                       @click="router.push(`/diagnosis/${row.id}`)">诊断</el-button>
            <el-button v-if="row.status === 'QUOTE_CONFIRM'" link type="primary"
                       @click="router.push(`/quote/${row.id}`)">报价</el-button>
            <el-button v-if="row.status === 'QC'" link type="primary"
                       @click="router.push(`/inspection/${row.id}`)">质检</el-button>
            <el-button v-if="row.status === 'SETTLE'" link type="primary"
                       @click="router.push(`/settlement/${row.id}`)">结算</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusTag from '../components/StatusTag.vue'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime, money } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 10, status: '', orderType: '', keyword: '' })

async function load() {
  const d = await http.get('/order/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

function reset() {
  Object.assign(q, { status: '', orderType: '', keyword: '', page: 1 })
  load()
}

onMounted(load)
</script>
