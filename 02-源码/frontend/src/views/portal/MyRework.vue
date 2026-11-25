<template>
  <div>
    <div class="page-head">
      <div><h1>返修申请</h1><p>质保期内出现同类问题可在线申请返修，门店判定责任后安排</p></div>
      <div class="head-acts"><el-button type="primary" @click="dlg = true">申请返修</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>可申请返修的工单</h3><span class="sub">已交车工单</span></div>
      <el-table :data="done" size="default">
        <el-table-column prop="orderNo" label="工单号" min-width="170" />
        <el-table-column prop="plateNo" label="车牌" min-width="105" />
        <el-table-column prop="vehicleName" label="车型" min-width="165" show-overflow-tooltip />
        <el-table-column label="交车时间" min-width="145">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.updateTime) }}</span></template>
        </el-table-column>
        <el-table-column label="质保" min-width="110">
          <template #default><span class="tag success">质保中</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="apply(row)">申请返修</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!done.length" class="empty">暂无可申请返修的已交车工单</div>
    </div>

    <el-dialog v-model="dlg" title="返修申请" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="原工单" required>
          <el-select v-model="form.orderId" filterable style="width:100%">
            <el-option v-for="o in done" :key="o.id" :label="`${o.orderNo} · ${o.plateNo}`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="form.problemDesc" type="textarea" :rows="3"
                    placeholder="请描述故障现象与发生时间，不少于 10 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../http'
import { fmtTime } from '../../utils/format'

const all = ref([])
const done = ref([])
const dlg = ref(false)
const form = reactive({ orderId: null, problemDesc: '' })

async function load() {
  const d = await http.get('/order/portal/my', { params: { page: 1, size: 50 } })
  all.value = d.records || []
  done.value = all.value.filter((o) => o.status === 'FINISHED')
}

function apply(row) {
  form.orderId = row.id
  form.problemDesc = ''
  dlg.value = true
}

async function submit() {
  if (!form.orderId || (form.problemDesc || '').length < 10) {
    return ElMessage.warning('请选择工单并填写不少于 10 字的问题描述')
  }
  await http.post('/order/return', { ...form })
  ElMessage.success('返修申请已提交，门店会尽快联系您')
  dlg.value = false
}

onMounted(load)
</script>
