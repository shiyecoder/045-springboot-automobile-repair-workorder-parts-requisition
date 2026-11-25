<template>
  <div>
    <div class="page-head">
      <div><h1>维修项目库</h1><p>标准工时与工时费的标准来源，接车后按项目组单</p></div>
      <div class="head-acts"><el-button type="primary" @click="dlg = true">新增项目</el-button></div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="工种">
          <el-select v-model="q.workType" clearable placeholder="全部工种" @change="load">
            <el-option v-for="o in options('work_type')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="项目名称 / 编码" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>项目列表</h3><span class="sub">共 {{ total }} 个</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="projectCode" label="项目编码" min-width="110" />
        <el-table-column prop="projectName" label="项目名称" min-width="190" show-overflow-tooltip />
        <el-table-column label="工种" min-width="90">
          <template #default="{ row }">{{ label('work_type', row.workType) }}</template>
        </el-table-column>
        <el-table-column prop="stdHours" label="标准工时" min-width="100" align="right" />
        <el-table-column label="工时费" min-width="100" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.laborFee) }}</span></template>
        </el-table-column>
        <el-table-column prop="applyModels" label="适用车型" min-width="160" show-overflow-tooltip />
        <el-table-column prop="commonParts" label="常用配件" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</span>
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
import { ElMessage } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { money } from '../utils/format'

const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, workType: '', keyword: '' })

async function load() {
  const d = await http.get('/meta/project/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

function reset() {
  q.workType = ''
  q.keyword = ''
  q.page = 1
  load()
}

onMounted(load)
</script>
