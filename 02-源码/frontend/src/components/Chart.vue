<template>
  <div ref="el" :style="{ height: height + 'px', width: '100%' }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

/** ECharts 容器：统一图表基线样式，页面只传 option。 */
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 150 }
})
const el = ref(null)
let chart = null
let ro = null

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value, null, { renderer: 'canvas' })
  // 全局基线：去掉多余网格线，字号与正文一致
  const opt = {
    animationDuration: 400,
    textStyle: { fontFamily: '-apple-system, "PingFang SC", sans-serif', fontSize: 11, color: '#6B7C86' },
    grid: { left: 8, right: 12, top: 24, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', textStyle: { fontSize: 11 } },
    ...props.option
  }
  chart.setOption(opt, true)
  chart.resize()
}

onMounted(async () => {
  await nextTick()
  render()
  ro = new ResizeObserver(() => chart && chart.resize())
  ro.observe(el.value)
})
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => {
  ro && ro.disconnect()
  chart && chart.dispose()
  chart = null
})
</script>
