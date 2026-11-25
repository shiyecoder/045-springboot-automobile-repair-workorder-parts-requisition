<template>
  <div>
    <div class="page-head">
      <div><h1>质保台账</h1><p>交车后自动生成质保单，到期前 30 天标黄提醒</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="质保状态">
          <el-select v-model="q.status" clearable placeholder="全部状态" @change="load">
            <el-option label="质保中" value="VALID" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="车牌 / 项目 / 质保单号" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>质保单</h3><span class="sub">共 {{ total }} 条</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="warrantyNo" label="质保单号" min-width="165" />
        <el-table-column prop="plateNo" label="车牌" min-width="105" />
        <el-table-column prop="vehicleName" label="车型" min-width="160" show-overflow-tooltip />
        <el-table-column prop="customerName" label="车主" min-width="95" />
        <el-table-column prop="projectName" label="质保项目" min-width="170" show-overflow-tooltip />
        <el-table-column label="质保期" min-width="150">
          <template #default="{ row }">
            <span class="num tiny">{{ row.warrantyDays }} 天 / {{ num(row.warrantyKm) }} km</span>
          </template>
        </el-table-column>
        <el-table-column label="起止日期" min-width="180">
          <template #default="{ row }"><span class="num tiny">{{ fmtDate(row.startDate) }} ~ {{ fmtDate(row.endDate) }}</span></template>
        </el-table-column>
        <el-table-column label="剩余" min-width="100" align="right">
          <template #default="{ row }">
            <span class="num" :style="{ color: row.leftDays < 30 ? 'var(--warn)' : 'inherit' }">
              {{ row.leftDays === null ? '—' : row.leftDays + ' 天' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 'VALID' ? 'success' : 'info'">
              {{ row.status === 'VALID' ? '质保中' : '已过期' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.orderId}`)">原工单</el-button>
            <el-button link type="primary" @click="router.push(`/returns?warrantyId=${row.id}`)">返修</el-button>
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
import http from '../http'
import { fmtDate, num } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 10, status: '', keyword: '' })

async function load() {
  const d = await http.get('/meta/warranty/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

function reset() {
  q.status = ''
  q.keyword = ''
  q.page = 1
  load()
}

onMounted(load)
</script>
