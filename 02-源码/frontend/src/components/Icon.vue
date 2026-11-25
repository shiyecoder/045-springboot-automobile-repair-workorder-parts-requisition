<template>
  <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
       stroke-linecap="round" stroke-linejoin="round" :width="size" :height="size">
    <path v-for="(d, i) in paths" :key="i" :d="d" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

/** 侧栏与按钮图标集（内联 SVG，无外部依赖，与 design/icons.svg 一一对应）。 */
const props = defineProps({ name: { type: String, default: 'clip' }, size: { type: Number, default: 15 } })

const SET = {
  chart: ['M4 20V10', 'M10 20V4', 'M16 20v-7', 'M21 20H3'],
  clip: ['M9 4h6v3H9z', 'M8 5.5H6.5A1.5 1.5 0 0 0 5 7v12a1.5 1.5 0 0 0 1.5 1.5h11A1.5 1.5 0 0 0 19 19V7a1.5 1.5 0 0 0-1.5-1.5H16', 'M8.5 12h7', 'M8.5 16h4.5'],
  cal: ['M4 6.5A1.5 1.5 0 0 1 5.5 5h13A1.5 1.5 0 0 1 20 6.5v12A1.5 1.5 0 0 1 18.5 20h-13A1.5 1.5 0 0 1 4 18.5z', 'M4 9.5h16', 'M8 3.5v3', 'M16 3.5v3'],
  car: ['M3 16v-3l2-5h14l2 5v3', 'M5.5 16h13', 'M7 19.5v-3.5', 'M17 19.5v-3.5', 'M6.5 12.5h2', 'M15.5 12.5h2'],
  user: ['M12 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z', 'M5 20c0-3.3 3.1-5.5 7-5.5s7 2.2 7 5.5'],
  wrench: ['M14.5 4.5a4.5 4.5 0 0 0 5.6 5.9L11 19.5a2.4 2.4 0 1 1-3.4-3.4z', 'M6.5 6.5l2 2'],
  cash: ['M3.5 7.5h17v9h-17z', 'M12 14.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z', 'M6.5 10v4', 'M17.5 10v4'],
  menu: ['M4 7h16', 'M4 12h16', 'M4 17h10'],
  check: ['M4 12.5l5 5L20 6.5'],
  box: ['M4 8l8-4 8 4-8 4z', 'M4 8v8l8 4 8-4V8', 'M12 12v8'],
  shield: ['M12 3.5l7 2.5v6c0 4-3 7-7 8.5-4-1.5-7-4.5-7-8.5V6z', 'M9 12l2 2 4-4'],
  gear: ['M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z', 'M19.4 14a7.6 7.6 0 0 0 0-4l1.6-1.2-1.8-3.1-1.9.8a7.6 7.6 0 0 0-3.5-2L13.5 2.5h-3l-.3 2a7.6 7.6 0 0 0-3.5 2l-1.9-.8L3 8.8 4.6 10a7.6 7.6 0 0 0 0 4L3 15.2l1.8 3.1 1.9-.8a7.6 7.6 0 0 0 3.5 2l.3 2h3l.3-2a7.6 7.6 0 0 0 3.5-2l1.9.8 1.8-3.1z'],
  bell: ['M18 16H6l1.5-2v-4a4.5 4.5 0 0 1 9 0v4z', 'M10 19h4'],
  home: ['M4 11l8-6.5 8 6.5', 'M6.5 10v9.5h11V10']
}
const paths = computed(() => SET[props.name] || SET.clip)
</script>
