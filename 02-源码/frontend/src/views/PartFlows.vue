<template>
  <div>
    <div class="page-head">
      <div><h1>领退记录</h1><p>所有领用、补领、退回均关联工单，库存流动可追溯</p></div>
      <div class="head-acts"><el-button type="primary" @click="router.push('/parts')">配件档案</el-button></div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="类型">
          <el-select v-model="q.type" clearable placeholder="全部类型" @change="load">
            <el-option v-for="o in options('part_flow_type')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="单号 / 工单号 / 配件" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>领退明细</h3><span class="sub">共 {{ total }} 条</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="flow.flowNo" label="单号" min-width="165" />
        <el-table-column prop="partName" label="配件" min-width="170" show-overflow-tooltip />
        <el-table-column prop="partCode" label="编码" min-width="110">
          <template #default="{ row }"><span class="num tiny">{{ row.partCode }}</span></template>
        </el-table-column>
        <el-table-column label="数量" min-width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.flow.qty }} {{ row.unit }}</span></template>
        </el-table-column>
        <el-table-column label="类型" min-width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.flow.flowType === 'RETURN' ? 'warning' : 'primary'">
              {{ label('part_flow_type', row.flow.flowType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联工单" min-width="160">
          <template #default="{ row }"><span class="num tiny">{{ row.orderNo || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次" min-width="130">
          <template #default="{ row }"><span class="num tiny">{{ row.batchNo || '未指定' }}</span></template>
        </el-table-column>
        <el-table-column prop="techName" label="技师" min-width="90" />
        <el-table-column prop="flow.oldPartHandle" label="旧件处理" min-width="120" show-overflow-tooltip />
        <el-table-column label="时间" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.flow.flowTime) }}</span></template>
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
import { label, options } from '../dict'
import { fmtTime } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 10, type: '', keyword: '' })

async function load() {
  const d = await http.get('/meta/flow/page', { params: { page: q.page, size: q.size } })
  let list = d.records || []
  if (q.type) list = list.filter((r) => r.flow.flowType === q.type)
  if (q.keyword) {
    const k = q.keyword
    list = list.filter((r) => (r.flow.flowNo || '').includes(k) || (r.orderNo || '').includes(k) || (r.partName || '').includes(k))
  }
  rows.value = list
  total.value = d.total || 0
}

function reset() {
  q.type = ''
  q.keyword = ''
  q.page = 1
  load()
}

onMounted(load)
</script>
