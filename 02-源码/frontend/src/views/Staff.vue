<template>
  <div>
    <div class="page-head">
      <div><h1>员工与技师</h1><p>技师实时在办任务数，派工时按负荷均衡分配</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="kpi-row four">
      <div class="kpi"><div class="t">员工总数</div><div class="v num">{{ rows.length }}</div></div>
      <div class="kpi"><div class="t">在办技师</div><div class="v num">{{ rows.filter(r => r.working > 0).length }}</div></div>
      <div class="kpi ok"><div class="t">总在办任务</div><div class="v num">{{ rows.reduce((s, r) => s + (r.working || 0), 0) }}</div></div>
      <div class="kpi warn"><div class="t">门店</div><div class="v" style="font-size:15px">{{ rows[0]?.orgName || '—' }}</div></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>员工名册</h3><span class="sub">含服务顾问、技师、仓管、质检</span></div>
      <el-table :data="rows" size="default">
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <img :src="img(row.avatar, '/uploads/seed/avatar/01.jpg')"
                 style="width:28px;height:28px;border-radius:50%;object-fit:cover" />
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="username" label="登录名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="125" />
        <el-table-column prop="orgName" label="所属门店" min-width="180" show-overflow-tooltip />
        <el-table-column label="在办任务" min-width="100" align="right">
          <template #default="{ row }">
            <span class="num" :style="{ color: row.working > 2 ? 'var(--warn)' : 'inherit' }">{{ row.working }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="150">
          <template #default="{ row }"><span class="num muted">{{ fmtTime(row.lastLogin) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push('/dispatch')">派工</el-button>
            <el-button link type="primary" @click="router.push('/users')">账号</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../http'
import { fmtTime, img } from '../utils/format'

const router = useRouter()
const rows = ref([])

async function load() {
  rows.value = (await http.get('/meta/staff/list')) || []
}
onMounted(load)
</script>
