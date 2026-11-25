<template>
  <div>
    <div class="page-head">
      <div><h1>消息中心</h1><p>工单状态变更、库存预警与返修通知统一在此推送</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>消息列表</h3><span class="sub">{{ unread }} 条未读</span></div>
      <el-table :data="rows" size="default">
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="tag" :class="row.readFlag ? 'info' : 'warning'">{{ row.readFlag ? '已读' : '未读' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="110">
          <template #default="{ row }">{{ { ORDER: '工单通知', STOCK: '库存预警', REWORK: '返修通知', SYSTEM: '系统消息' }[row.msgType] || row.msgType }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="360" show-overflow-tooltip />
        <el-table-column label="时间" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.readFlag" @click="read(row)">标为已读</el-button>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'
import { fmtTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 10 })
const unread = computed(() => rows.value.filter((r) => !r.readFlag).length)

async function load() {
  const d = await http.get('/system/message/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

async function read(row) {
  await http.put(`/system/message/${row.id}/read`)
  ElMessage.success('已标记为已读')
  load()
}

onMounted(load)
</script>
