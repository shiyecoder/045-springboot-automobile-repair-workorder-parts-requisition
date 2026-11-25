<template>
  <div>
    <div class="p-hero">
      <div>
        <h2>{{ user?.realName }}，您的爱车正在这里保养</h2>
        <p>共 {{ orders.length }} 次维修记录 · 待确认报价 {{ quotes.length }} 单</p>
      </div>
      <div class="plate">{{ orders[0]?.plateNo || '暂无车辆' }}</div>
    </div>

    <div class="grid-3" style="margin-top:16px">
      <div class="card" style="margin:0">
        <div class="card-h"><h3>维修中</h3></div>
        <div class="card-b">
          <div class="v num" style="font-size:26px;font-weight:600">{{ active.length }}</div>
          <div class="tiny muted">正在进行中的工单</div>
        </div>
      </div>
      <div class="card" style="margin:0">
        <div class="card-h"><h3>待确认报价</h3></div>
        <div class="card-b">
          <div class="v num" style="font-size:26px;font-weight:600;color:var(--warn)">{{ quotes.length }}</div>
          <div class="tiny muted">确认后开始施工</div>
        </div>
      </div>
      <div class="card" style="margin:0">
        <div class="card-h"><h3>质保内项目</h3></div>
        <div class="card-b">
          <div class="v num" style="font-size:26px;font-weight:600;color:var(--ok)">{{ warrantyCount }}</div>
          <div class="tiny muted">交车后自动生成</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <div class="card-h">
        <h3>最近维修</h3>
        <el-button link type="primary" @click="router.push('/portal/orders')">查看全部</el-button>
      </div>
      <el-table :data="orders.slice(0, 5)" size="default">
        <el-table-column prop="orderNo" label="工单号" min-width="170" />
        <el-table-column prop="vehicleName" label="车型" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" min-width="115">
          <template #default="{ row }"><StatusTag :status="row.status" :name="row.statusName" /></template>
        </el-table-column>
        <el-table-column label="接车时间" min-width="145">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.receiveTime) }}</span></template>
        </el-table-column>
        <el-table-column label="金额" min-width="110" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="card-h"><h3>门店服务承诺</h3></div>
      <div class="card-b">
        <div class="grid-3">
          <div>
            <div class="strong">报价先确认</div>
            <div class="tiny muted" style="line-height:1.9">所有收费项目均需您确认后才开始施工，追加项目会再次通知。</div>
          </div>
          <div>
            <div class="strong">配件可追溯</div>
            <div class="tiny muted" style="line-height:1.9">每件配件记录批次与领用技师，旧件可选带回。</div>
          </div>
          <div>
            <div class="strong">质保与返修</div>
            <div class="tiny muted" style="line-height:1.9">交车即生成质保单，质保期内可直接在线申请返修。</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import http from '../../http'
import { store } from '../../store'
import { fmtTime, money } from '../../utils/format'

const router = useRouter()
const user = store.user
const orders = ref([])
const quotes = ref([])

const active = computed(() => orders.value.filter((o) => !['FINISHED', 'CANCELED'].includes(o.status)))
const warrantyCount = computed(() => orders.value.filter((o) => o.status === 'FINISHED').length * 2)

onMounted(async () => {
  const d = await http.get('/order/portal/my', { params: { page: 1, size: 50 } })
  orders.value = d.records || []
  quotes.value = (await http.get('/order/portal/quotes')) || []
})
</script>
