<template>
  <div>
    <div class="page-head">
      <div>
        <h1>我的待办</h1>
        <p>按当前角色推送待处理工单，逾期与高优先级置顶</p>
      </div>
      <div class="head-acts">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="router.push('/orders')">全部工单</el-button>
      </div>
    </div>

    <div class="kpi-row four">
      <div class="kpi"><div class="t">待处理总数</div><div class="v num">{{ rows.length }}</div></div>
      <div class="kpi dan"><div class="t">逾期 / 今日到期</div><div class="v num">{{ urgent }}</div></div>
      <div class="kpi warn"><div class="t">高优先级</div><div class="v num">{{ high }}</div></div>
      <div class="kpi ok"><div class="t">我的角色</div><div class="v" style="font-size:16px">{{ store.user?.roleName }}</div></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>待办清单</h3><span class="sub">共 {{ rows.length }} 条</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="orderNo" label="工单号" min-width="160" />
        <el-table-column prop="item" label="待办事项" min-width="220" show-overflow-tooltip />
        <el-table-column label="当前状态" min-width="120">
          <template #default="{ row }"><StatusTag :status="row.status" :name="row.statusName" /></template>
        </el-table-column>
        <el-table-column label="优先级" min-width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.priority === 'HIGH' ? 'danger' : 'info'">{{ row.priority === 'HIGH' ? '高' : '普通' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预计交车" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.expectFinish) }}</span></template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="140">
          <template #default="{ row }"><span class="num muted">{{ fmtTime(row.updateTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">去处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusTag from '../components/StatusTag.vue'
import http from '../http'
import { store } from '../store'
import { fmtTime } from '../utils/format'

const router = useRouter()
const rows = ref([])
const high = computed(() => rows.value.filter((r) => r.priority === 'HIGH').length)
const urgent = computed(() => rows.value.filter((r) => {
  if (!r.expectFinish) return false
  return new Date(String(r.expectFinish).replace(' ', 'T')).getTime() - Date.now() < 86400000 * 2
}).length)

async function load() {
  rows.value = (await http.get('/dashboard/todo')) || []
}
onMounted(load)
</script>
