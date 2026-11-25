<template>
  <div>
    <div class="page-head">
      <div><h1>经营分析报表</h1><p>产值、工时、返修与预检采纳率的多维对照</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="kpi-row four">
      <div class="kpi ok"><div class="t">月度产值</div><div class="v num">{{ money(kpi.monthAmount) }}</div></div>
      <div class="kpi"><div class="t">配件成本</div><div class="v num">{{ money(kpi.partAmount) }}</div></div>
      <div class="kpi warn"><div class="t">返修率</div><div class="v num">{{ kpi.returnRate ?? 0 }}<small>%</small></div></div>
      <div class="kpi"><div class="t">客户确认率</div><div class="v num">{{ kpi.confirmRate ?? 0 }}<small>%</small></div></div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-h"><h3>月度产值趋势</h3><span class="sub">近 6 个月</span></div>
        <div class="card-b tight"><Chart :option="monthOpt" :height="200" /></div>
      </div>
      <div class="card">
        <div class="card-h"><h3>工种产值结构</h3></div>
        <div class="card-b tight"><Chart :option="incomeOpt" :height="200" /></div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-h"><h3>技师工时与产值</h3></div>
        <el-table :data="techRank" size="default">
          <el-table-column type="index" label="排名" width="70" />
          <el-table-column prop="name" label="技师" min-width="100" />
          <el-table-column label="工时" min-width="100" align="right">
            <template #default="{ row }"><span class="num">{{ row.hours }} h</span></template>
          </el-table-column>
          <el-table-column label="工时占比" min-width="140">
            <template #default="{ row }">
              <div class="factor" style="margin:0">
                <span></span>
                <div class="bar"><span :style="{ width: pct(row.hours) + '%' }"></span></div>
                <span class="num tiny">{{ pct(row.hours) }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="card">
        <div class="card-h"><h3>预检采纳情况</h3><span class="sub">创新点指标</span></div>
        <div class="card-b">
          <div class="kpi-row four" style="margin-bottom:0">
            <div class="kpi"><div class="t">预检总数</div><div class="v num">{{ pre.total ?? 0 }}</div></div>
            <div class="kpi ok"><div class="t">推荐</div><div class="v num">{{ pre.recommend ?? 0 }}</div></div>
            <div class="kpi warn"><div class="t">谨慎</div><div class="v num">{{ pre.caution ?? 0 }}</div></div>
            <div class="kpi dan"><div class="t">不推荐</div><div class="v num">{{ pre.reject ?? 0 }}</div></div>
          </div>
          <div class="split"></div>
          <dl class="kv">
            <dt>采纳率</dt><dd class="num">{{ pre.adoptRate ?? 0 }}%</dd>
            <dt>人工修正率</dt><dd class="num">{{ pre.manualRate ?? 0 }}%</dd>
            <dt>已领用次数</dt><dd class="num">{{ pre.claimed ?? 0 }}</dd>
          </dl>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import Chart from '../components/Chart.vue'
import http from '../http'
import { money } from '../utils/format'

const kpi = ref({})
const income = ref({})
const techRank = ref([])
const monthly = ref([])
const pre = ref({})

const pct = (h) => {
  const max = Math.max(...techRank.value.map((t) => t.hours), 1)
  return Math.round((h / max) * 100)
}

const monthOpt = computed(() => ({
  xAxis: { type: 'category', data: monthly.value.map((m) => m.month), axisTick: { show: false }, axisLine: { lineStyle: { color: '#DDE6E8' } } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F4' } } },
  series: [{ type: 'bar', barWidth: 28, data: monthly.value.map((m) => m.amount), itemStyle: { color: '#15344A', borderRadius: [3, 3, 0, 0] } }]
}))

const incomeOpt = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, itemWidth: 8, itemHeight: 8 },
  series: [{
    type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'], label: { show: false },
    data: Object.entries(income.value).map(([k, v], i) => ({
      name: k, value: v, itemStyle: { color: ['#15344A', '#237A8B', '#4FA3B3', '#B07A20', '#B03035'][i % 5] }
    }))
  }]
}))

async function load() {
  const d = await http.get('/dashboard')
  kpi.value = d.kpi || {}
  income.value = d.income || {}
  techRank.value = d.techRank || []
  pre.value = (await http.get('/precheck/stats')) || {}
  monthly.value = (await http.get('/dashboard/monthly')) || []
}

onMounted(load)
</script>
