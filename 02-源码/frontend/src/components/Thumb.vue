<template>
  <img class="thumb" :src="img(src, fallback)" :alt="alt" @error="onError" />
</template>

<script setup>
import { ref } from 'vue'
import { img } from '../utils/format'

/** 列表缩略图：加载失败退到本地兜底图，避免出现破图。 */
const props = defineProps({
  src: String,
  alt: { type: String, default: '' },
  fallback: { type: String, default: '/uploads/seed/part/01.jpg' }
})
const onError = (e) => { e.target.src = props.fallback }
</script>
