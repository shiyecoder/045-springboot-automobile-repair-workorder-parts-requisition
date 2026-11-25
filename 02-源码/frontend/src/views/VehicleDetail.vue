<template>
  <div v-if="d.vehicle">
    <div class="page-head">
      <div>
        <h1>{{ d.vehicle.plateNo }} · {{ d.vehicle.brand }} {{ d.vehicle.model }}</h1>
        <p>车主 {{ d.customer?.name }} · {{ d.customer?.phone }} · 累计维修 {{ (d.orders || []).length }} 次</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" @click="router.push(`/reception/new?vehicleId=${d.vehicle.id}`)">再次接车</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>车辆档案</h3><span class="sub">VIN {{ d.vehicle.vin }}</span></div>
          <div class="card-b">
            <div class="flex gap" style="align-items:flex-start">
              <img :src="img(d.vehicle.photo)" style="width:246px;height:164px;object-fit:cover;border-radius:6px" />
              <dl class="kv" style="flex:1">
                <dt>车牌号</dt><dd class="strong">{{ d.vehicle.plateNo }}</dd>
                <dt>品牌车型</dt><dd>{{ d.vehicle.brand }} {{ d.vehicle.model }}</dd>
                <dt>发动机号</dt><dd class="num">{{ d.vehicle.engineNo }}</dd>
                <dt>当前里程</dt><dd class="num">{{ num(d.vehicle.mileage) }} km</dd>
                <dt>购车日期</dt><dd class="num">{{ fmtDate(d.vehicle.buyDate) }}</dd>
                <dt>车辆状态</dt><dd>{{ label('vehicle_status', d.vehicle.vehicleStatus) }}</dd>
                <dt>建档时间</dt><dd class="num">{{ fmtTime(d.vehicle.createTime) }}</dd>
              </dl>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>维修历史</h3><span class="sub">共 {{ (d.orders || []).length }} 单</span></div>
          <el-table :data="d.orders || []" size="default">
            <el-table-column prop="orderNo" label="工单号" min-width="165">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">{{ row.orderNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column label="业务类型" min-width="100">
              <template #default="{ row }">{{ label('order_type', row.orderType) }}</template>
            </el-table-column>
            <el-table-column label="状态" min-width="115">
              <template #default="{ row }"><StatusTag :status="row.status" :name="row.statusName" /></template>
            </el-table-column>
            <el-table-column label="接车时间" min-width="140">
              <template #default="{ row }"><span class="num">{{ fmtTime(row.receiveTime) }}</span></template>
            </el-table-column>
            <el-table-column prop="faultDesc" label="故障描述" min-width="220" show-overflow-tooltip />
            <el-table-column label="金额" min-width="110" align="right">
              <template #default="{ row }"><span class="num">{{ money(row.totalAmount) }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>车主信息</h3></div>
          <div class="card-b">
            <dl class="kv">
              <dt>姓名</dt><dd>{{ d.customer?.name }}</dd>
              <dt>手机号</dt><dd class="num">{{ d.customer?.phone }}</dd>
              <dt>客户类型</dt><dd>{{ label('customer_type', d.customer?.customerType) }}</dd>
              <dt>到店次数</dt><dd class="num">{{ d.customer?.visitCount }} 次</dd>
              <dt>累计消费</dt><dd class="num strong">{{ money(d.customer?.totalAmount) }}</dd>
              <dt>备注</dt><dd class="tiny">{{ d.customer?.remark || '—' }}</dd>
            </dl>
          </div>
        </div>
        <div class="card">
          <div class="card-h"><h3>操作轨迹</h3></div>
          <div class="card-b">
            <div class="timeline">
              <div v-for="t in (d.timeline || []).slice(0, 10)" :key="t.id" class="tl-item">
                <div class="tt">{{ t.action }}</div>
                <div class="td">{{ t.detail }} · {{ fmtTime(t.createTime) }}</div>
              </div>
              <div v-if="!(d.timeline || []).length" class="empty">暂无轨迹</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StatusTag from '../components/StatusTag.vue'
import http from '../http'
import { label } from '../dict'
import { fmtDate, fmtTime, img, money, num } from '../utils/format'

const route = useRoute()
const router = useRouter()
const d = ref({})

onMounted(async () => {
  d.value = await http.get(`/meta/vehicle/${route.params.id}`)
})
</script>
