<template>
  <div v-if="o.id">
    <div class="page-head">
      <div>
        <h1>交车结算</h1>
        <p>{{ o.orderNo }} · {{ o.plateNo }} · {{ o.customerName }} · 应收 {{ money(o.totalAmount) }}</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.push(`/orders/${o.id}`)">工单详情</el-button>
      </div>
    </div>

    <div class="kpi-row four">
      <div class="kpi"><div class="t">工时费</div><div class="v num">{{ money(o.currentQuote?.totalLabor) }}</div></div>
      <div class="kpi"><div class="t">配件费</div><div class="v num">{{ money(o.currentQuote?.totalPart) }}</div></div>
      <div class="kpi warn"><div class="t">应收</div><div class="v num">{{ money(o.totalAmount) }}</div></div>
      <div class="kpi ok"><div class="t">已收</div><div class="v num">{{ money(o.paidAmount) }}</div></div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>费用明细</h3></div>
          <el-table :data="o.fees || []" size="default">
            <el-table-column prop="feeDesc" label="费用项" min-width="240" />
            <el-table-column label="类型" min-width="100">
              <template #default="{ row }">{{ { LABOR: '工时费', PART: '配件费', OTHER: '其他' }[row.feeType] || row.feeType }}</template>
            </el-table-column>
            <el-table-column label="金额" min-width="120" align="right">
              <template #default="{ row }"><span class="num">{{ money(row.amount) }}</span></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card">
          <div class="card-h"><h3>收款记录</h3></div>
          <el-table :data="o.payments || []" size="default">
            <el-table-column label="收款方式" min-width="110">
              <template #default="{ row }">{{ label('pay_type', row.payType) }}</template>
            </el-table-column>
            <el-table-column label="金额" min-width="120" align="right">
              <template #default="{ row }"><span class="num strong">{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="收款时间" min-width="150">
              <template #default="{ row }"><span class="num">{{ fmtTime(row.payTime) }}</span></template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
          </el-table>
          <div v-if="!(o.payments || []).length" class="empty">尚未收款</div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>登记收款</h3></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item label="收款方式">
                <el-select v-model="pay.payType" style="width:100%">
                  <el-option v-for="p in options('pay_type')" :key="p.value" :label="p.label" :value="p.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="收款金额">
                <el-input v-model.number="pay.amount" type="number" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="pay.remark" placeholder="如：线下收款，已开票" />
              </el-form-item>
              <el-button style="width:100%" @click="doPay">确认收款</el-button>
            </el-form>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>交车登记</h3></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item label="交车里程">
                <el-input v-model.number="del.mileage" type="number" />
              </el-form-item>
              <el-form-item label="验收结果">
                <el-select v-model="del.acceptResult" style="width:100%">
                  <el-option label="客户验收通过" value="ACCEPTED" />
                  <el-option label="客户有异议" value="OBJECTED" />
                </el-select>
              </el-form-item>
              <el-form-item label="旧件">
                <el-select v-model="del.oldPartTaken" style="width:100%">
                  <el-option label="已领取" value="已领取" />
                  <el-option label="客户放弃" value="客户放弃" />
                </el-select>
              </el-form-item>
              <el-form-item label="交车凭证">
                <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
                  <el-button>上传凭证</el-button>
                </el-upload>
                <img v-if="del.voucher" :src="del.voucher"
                     style="width:64px;height:48px;object-fit:cover;border-radius:4px;margin-left:8px" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="del.remark" placeholder="客户现场验收说明" />
              </el-form-item>
              <el-button type="primary" style="width:100%" @click="doDelivery">完成交车</el-button>
            </el-form>
            <div class="split"></div>
            <div class="tiny muted" style="line-height:1.9">
              硬规则：未通过质检或存在未结费用的工单不能交车，后端会直接拒绝。
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime, money } from '../utils/format'

const route = useRoute()
const router = useRouter()
const o = ref({})
const pay = reactive({ payType: 'WECHAT', amount: 0, remark: '' })
const del = reactive({ mileage: null, acceptResult: 'ACCEPTED', oldPartTaken: '已领取', voucher: '', remark: '' })

async function load() {
  o.value = await http.get(`/order/${route.params.id}`)
  pay.amount = Math.max(0, Number(o.value.totalAmount || 0) - Number(o.value.paidAmount || 0))
  del.mileage = o.value.reception?.mileage || null
}

async function upload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  del.voucher = typeof res === 'string' ? res : (res?.url || res?.path || '')
}

async function doPay() {
  await http.post('/order/payment', { orderId: o.value.id, ...pay })
  ElMessage.success('收款已登记')
  load()
}

async function doDelivery() {
  if (!del.mileage) return ElMessage.warning('请填写交车里程')
  await http.post('/order/delivery', { orderId: o.value.id, ...del })
  ElMessage.success('交车完成')
  load()
}

onMounted(load)
</script>
