<template>
  <div>
    <div class="page-head">
      <div><h1>报价确认</h1><p>确认后门店才开始施工；不同意的项目可以单独拒绝</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div v-if="!list.length" class="card"><div class="empty">当前没有待确认的报价</div></div>

    <div v-for="q in list" :key="q.quoteId" class="card">
      <div class="card-h">
        <h3>{{ q.orderNo }} · {{ q.plateNo }}</h3>
        <span class="sub">第 {{ q.versionNo }} 版报价 · 合计 {{ money(q.quoteAmount) }}</span>
      </div>
      <el-table :data="q.items || []" size="default">
        <el-table-column prop="itemName" label="项目 / 配件" min-width="240" />
        <el-table-column label="类型" min-width="90">
          <template #default="{ row }">{{ row.itemType === 'LABOR' ? '工时' : '配件' }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" min-width="80" align="right" />
        <el-table-column label="单价" min-width="100" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.unitPrice) }}</span></template>
        </el-table-column>
        <el-table-column label="金额" min-width="110" align="right">
          <template #default="{ row }"><span class="num strong">{{ money(row.amount) }}</span></template>
        </el-table-column>
      </el-table>
      <div class="card-b tight">
        <div class="flex" style="justify-content:space-between;align-items:flex-start">
          <div class="tiny muted" style="line-height:1.9">
            工时费 {{ money(q.totalLabor) }} · 配件费 {{ money(q.totalPart) }} · 优惠 {{ money(q.discount) }}<br />
            确认后如增加项目，门店会再次发起确认。
          </div>
          <div class="flex gap">
            <el-button @click="reject(q)">不同意</el-button>
            <el-button type="primary" @click="agree(q)">同意并开始维修</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../http'
import { money } from '../../utils/format'

const list = ref([])

async function load() {
  list.value = (await http.get('/order/portal/quotes')) || []
}

async function agree(q) {
  await ElMessageBox.confirm(`确认同意报价 ${money(q.quoteAmount)} 并开始维修？`, '报价确认', { type: 'info' })
  await http.post('/order/confirm', {
    quoteId: q.quoteId, confirmType: 'APP', confirmResult: 'AGREE',
    remark: '车主在车主中心确认全部项目', agreeItemIds: null, rejectItemIds: null
  })
  ElMessage.success('已确认，门店将开始维修')
  load()
}

async function reject(q) {
  const { value } = await ElMessageBox.prompt('请说明不同意原因', '拒绝报价', { inputPlaceholder: '如：费用超出预算' })
  await http.post('/order/confirm', {
    quoteId: q.quoteId, confirmType: 'APP', confirmResult: 'REJECT', remark: value || '车主拒绝', agreeItemIds: null, rejectItemIds: null
  })
  ElMessage.success('已提交拒绝，门店会与您联系')
  load()
}

onMounted(load)
</script>
