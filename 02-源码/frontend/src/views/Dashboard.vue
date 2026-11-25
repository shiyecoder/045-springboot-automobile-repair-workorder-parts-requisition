<template>
  <div>
    <div class="page-head">
      <div>
        <h1>维修经营分析</h1>
        <p>数据范围：近 30 天 · 更新于 {{ updated }}</p>
      </div>
      <div class="head-acts">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="router.push('/reception/new')">接车登记</el-button>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi">
        <div class="t">今日进厂</div>
        <div class="v num">{{ d.kpi?.todayIn ?? 0 }}<small>台</small></div>
        <div class="d">在修 {{ d.kpi?.inRepair ?? 0 }} 台</div>
      </div>
      <div class="kpi warn">
        <div class="t">待客户确认</div>
        <div class="v num">{{ d.kpi?.waitConfirm ?? 0 }}<small>单</small></div>
        <div class="d">确认率 {{ d.kpi?.confirmRate ?? 0 }}%</div>
      </div>
      <div class="kpi">
        <div class="t">待质检</div>
        <div class="v num">{{ d.kpi?.waitQc ?? 0 }}<small>单</small></div>
        <div class="d">一次合格率 {{ d.kpi?.firstPassRate ?? 0 }}%</div>
      </div>
      <div class="kpi dan">
        <div class="t">库存预警</div>
        <div class="v num">{{ d.kpi?.stockWarn ?? 0 }}<small>项</small></div>
        <div class="d">低于安全库存</div>
      </div>
      <div class="kpi ok">
        <div class="t">本月产值</div>
        <div class="v num">{{ num(d.kpi?.monthAmount) }}<small>元</small></div>
        <div class="d">配件成本 {{ num(d.kpi?.partAmount) }} 元</div>
      </div>
      <div class="kpi">
        <div class="t">技师总工时</div>
        <div class="v num">{{ d.kpi?.techHours ?? 0 }}<small>h</small></div>
        <div class="d">平均在修 {{ d.kpi?.avgHours ?? 0 }} h</div>
      </div>
    </div>

    <div class="grid-3">
      <div class="card" style="margin-bottom:0">
        <div class="card-h"><h3>近 14 天接车趋势</h3><span class="sub">台次</span></div>
        <div class="card-b tight"><Chart :option="trendOpt" :height="150" /></div>
      </div>
      <div class="card" style="margin-bottom:0">
        <div class="card-h"><h3>工单状态分布</h3><span class="sub">全量</span></div>
        <div class="card-b tight"><Chart :option="statusOpt" :height="150" /></div>
      </div>
      <div class="card" style="margin-bottom:0">
        <div class="card-h"><h3>产值结构</h3><span class="sub">按工种</span></div>
        <div class="card-b tight"><Chart :option="incomeOpt" :height="150" /></div>
      </div>
    </div>

    <div class="grid-3" style="margin-top:12px">
      <div class="card" style="margin-bottom:0">
        <div class="card-h"><h3>技师工时排行</h3><span class="sub">TOP 6</span></div>
        <div class="card-b tight"><Chart :option="techOpt" :height="150" /></div>
      </div>
      <div class="card" style="margin-bottom:0">
        <div class="card-h"><h3>配件消耗 TOP 6</h3><span class="sub">数量</span></div>
        <div class="card-b tight"><Chart :option="partOpt" :height="150" /></div>
      </div>
      <div class="card" style="margin-bottom:0">
        <div class="card-h"><h3>领用预检建议分布</h3><span class="sub">创新点</span></div>
        <div class="card-b tight"><Chart :option="preOpt" :height="150" /></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Chart from '../components/Chart.vue'
import http from '../http'
import { num } from '../utils/format'

const router = useRouter()
const d = ref({})
const updated = ref('—')
const COLORS = ['#15344A', '#237A8B', '#4FA3B3', '#B07A20', '#B03035', '#5F7078']

async function load() {
  d.value = (await http.get('/dashboard')) || {}
  const now = new Date()
  updated.value = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
}

const trendOpt = computed(() => {
  const t = d.value.trend || {}
  return {
    grid: { left: 4, right: 8, top: 16, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: Object.keys(t), axisLine: { lineStyle: { color: '#DDE6E8' } }, axisTick: { show: false }, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#EDF3F4' } }, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', smooth: true, data: Object.values(t), symbolSize: 5,
      itemStyle: { color: '#237A8B' }, lineStyle: { width: 2 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(35,122,139,.28)' }, { offset: 1, color: 'rgba(35,122,139,0)' }] } }
    }]
  }
})

const statusOpt = computed(() => {
  const s = d.value.statusDist || {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    legend: { type: 'scroll', bottom: 0, itemWidth: 8, itemHeight: 8, itemGap: 8, textStyle: { fontSize: 10 } },
    series: [{
      type: 'pie', radius: ['40%', '60%'], center: ['50%', '42%'], avoidLabelOverlap: true,
      label: { show: false },
      data: Object.entries(s).map(([k, v], i) => ({ name: k, value: v, itemStyle: { color: COLORS[i % 6] } }))
    }]
  }
})

const incomeOpt = computed(() => {
  const s = d.value.income || {}
  return {
    xAxis: { type: 'category', data: Object.keys(s), axisTick: { show: false }, axisLine: { lineStyle: { color: '#DDE6E8' } }, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F4' } }, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', barWidth: 26, data: Object.values(s), itemStyle: { color: '#2D5B6B', borderRadius: [3, 3, 0, 0] } }]
  }
})

const techOpt = computed(() => {
  const arr = (d.value.techRank || []).slice().reverse()
  return {
    grid: { left: 4, right: 24, top: 6, bottom: 0, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F4' } }, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: arr.map((x) => x.name), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { fontSize: 11 } },
    series: [{ type: 'bar', barWidth: 11, data: arr.map((x) => x.hours), itemStyle: { color: '#237A8B', borderRadius: [0, 3, 3, 0] }, label: { show: true, position: 'right', fontSize: 10, color: '#6B7C86' } }]
  }
})

const partOpt = computed(() => {
  const arr = (d.value.partRank || []).slice().reverse()
  return {
    grid: { left: 4, right: 30, top: 6, bottom: 0, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F4' } }, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: arr.map((x) => (x.name || '').slice(0, 8)), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', barWidth: 11, data: arr.map((x) => x.qty), itemStyle: { color: '#B07A20', borderRadius: [0, 3, 3, 0] }, label: { show: true, position: 'right', fontSize: 10, color: '#6B7C86' } }]
  }
})

const preOpt = computed(() => {
  const p = d.value.precheck || {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 次' },
    legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10 } },
    series: [{
      type: 'pie', radius: ['40%', '62%'], center: ['50%', '42%'], label: { show: false },
      data: [
        { name: '推荐领用', value: p['推荐领用'] ?? 0, itemStyle: { color: '#2D7A58' } },
        { name: '谨慎处理', value: p['谨慎处理'] ?? 0, itemStyle: { color: '#B07A20' } },
        { name: '不推荐', value: p['不推荐'] ?? 0, itemStyle: { color: '#B03035' } }
      ]
    }]
  }
})

onMounted(load)
</script>
