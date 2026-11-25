<template>
  <span class="tag" :class="color">{{ text }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { label, statusColor } from '../dict'

/** 工单状态标签：中文文案 + 前置圆点，不只靠颜色区分。 */
const props = defineProps({
  status: { type: String, default: '' },
  name: { type: String, default: '' }
})
const color = computed(() => statusColor(props.status))
const text = computed(() => props.name || label('order_status', props.status))
</script>
