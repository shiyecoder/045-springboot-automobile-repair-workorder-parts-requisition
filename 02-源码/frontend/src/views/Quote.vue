<template>
  <div v-if="o.id">
    <div class="page-head">
      <div>
        <h1>报价与客户确认</h1>
        <p>{{ o.orderNo }} · {{ o.plateNo }} · {{ o.customerName }} · 当前状态 {{ o.statusName }}</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.push(`/orders/${o.id}`)">工单详情</el-button>
        <el-button v-if="o.status === 'QUOTE_CONFIRM'" type="primary" @click="genQuote">生成报价单</el-button>
        <el-button v-if="o.status === 'REPAIR_PART'" @click="router.push(`/part/claim/${o.id}`)">去领料</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h">
            <h3>报价单 {{ current?.quoteNo || '（未生成）' }}</h3>
            <span class="sub" v-if="current">第 {{ current.versionNo }} 版 · {{ quoteStatusText(current.status) }}</span>
          </div>
          <el-table :data="o.quoteItems || []" size="default">
            <el-table-column prop="itemName" label="项目 / 配件" min-width="220" />
            <el-table-column label="类型" min-width="90">
              <template #default="{ row }">{{ row.itemType === 'LABOR' ? '工时' : '配件' }}</template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" min-width="80" align="right" />
            <el-table-column prop="unit" label="单位" min-width="70" />
            <el-table-column label="单价" min-width="100" align="right">
              <template #default="{ row }"><span class="num">{{ money(row.unitPrice) }}</span></template>
            </el-table-column>
            <el-table-column label="金额" min-width="110" align="right">
              <template #default="{ row }"><span class="num strong">{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="确认" min-width="100">
              <template #default="{ row }">
                <span class="tag" :class="row.confirmStatus === 'AGREE' ? 'success' : row.confirmStatus === 'REJECT' ? 'danger' : 'info'">
                  {{ { AGREE: '客户同意', REJECT: '客户拒绝', WAIT: '待确认' }[row.confirmStatus] || '待确认' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <div class="card-b tight" v-if="current">
            <div class="flex" style="justify-content:space-between">
              <span class="tiny muted">
                工时费 {{ money(current.totalLabor) }} · 配件费 {{ money(current.totalPart) }} · 优惠 {{ money(current.discount) }}
              </span>
              <span class="num strong" style="font-size:16px">{{ money(current.totalAmount) }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>客户确认记录</h3></div>
          <el-table :data="o.confirms || []" size="default">
            <el-table-column prop="versionNo" label="版本" min-width="70" />
            <el-table-column label="确认方式" min-width="110">
              <template #default="{ row }">{{ label('confirm_type', row.confirmType) }}</template>
            </el-table-column>
            <el-table-column label="结果" min-width="100">
              <template #default="{ row }">
                <span class="tag" :class="row.confirmResult === 'AGREE' ? 'success' : 'danger'">
                  {{ row.confirmResult === 'AGREE' ? '客户同意' : '客户拒绝' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="confirmBy" label="确认人" min-width="110" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="确认时间" min-width="140">
              <template #default="{ row }"><span class="num">{{ fmtTime(row.confirmTime) }}</span></template>
            </el-table-column>
          </el-table>
          <div v-if="!(o.confirms || []).length" class="empty">客户尚未确认报价</div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>操作</h3></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item label="优惠金额">
                <el-input v-model.number="quoteForm.discount" type="number" />
              </el-form-item>
              <el-form-item label="预计天数">
                <el-input v-model.number="quoteForm.expectDays" type="number" />
              </el-form-item>
              <el-button style="width:100%" @click="genQuote">按项目重新生成</el-button>
            </el-form>
            <div class="split"></div>
            <el-form label-width="88px">
              <el-form-item label="确认方式">
                <el-select v-model="confirmForm.confirmType" style="width:100%">
                  <el-option v-for="c in options('confirm_type')" :key="c.value" :label="c.label" :value="c.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="客户意见">
                <el-radio-group v-model="confirmForm.confirmResult">
                  <el-radio value="AGREE">同意全部</el-radio>
                  <el-radio value="REJECT">拒绝</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="confirmForm.remark" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="primary" style="width:100%" :disabled="!current" @click="doConfirm">
                登记客户确认
              </el-button>
            </el-form>
            <div class="split"></div>
            <div class="tiny muted" style="line-height:1.9">
              硬规则：未确认的收费项目不得进入派工与配件领用。客户同意后工单自动进入「维修领料」。
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime, money } from '../utils/format'

const route = useRoute()
const router = useRouter()
const o = ref({})
const quoteForm = reactive({ discount: 0, expectDays: 2 })
const confirmForm = reactive({ confirmType: 'SMS', confirmResult: 'AGREE', remark: '同意全部维修项目' })

const current = computed(() => o.value.currentQuote)

const quoteStatusText = (s) => ({ DRAFT: '草稿', CONFIRMED: '已确认', REJECTED: '客户拒绝' }[s] || s || '—')

async function load() {
  o.value = await http.get(`/order/${route.params.id}`)
  if (o.value.currentQuote) {
    quoteForm.discount = o.value.currentQuote.discount || 0
    quoteForm.expectDays = o.value.currentQuote.expectDays || 2
  }
}

async function genQuote() {
  await http.post('/order/quote', { orderId: o.value.id, discount: quoteForm.discount, expectDays: quoteForm.expectDays })
  ElMessage.success('报价单已生成')
  load()
}

async function doConfirm() {
  await http.post('/order/confirm', { quoteId: current.value.id, ...confirmForm, agreeItemIds: null, rejectItemIds: null })
  ElMessage.success('客户确认已登记')
  load()
}

onMounted(load)
</script>
