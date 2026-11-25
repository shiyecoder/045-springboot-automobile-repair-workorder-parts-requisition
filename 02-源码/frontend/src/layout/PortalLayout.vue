<template>
  <div class="portal">
    <div class="p-nav">
      <div class="brand">
        <div class="mark">车</div>
        <b>车维通 · 车主中心</b>
      </div>
      <router-link to="/portal" :class="{ on: route.path === '/portal' }">我的爱车</router-link>
      <router-link to="/portal/orders" :class="{ on: route.path.includes('/orders') }">维修进度</router-link>
      <router-link to="/portal/quotes" :class="{ on: route.path.includes('/quotes') }">报价确认</router-link>
      <router-link to="/portal/rework" :class="{ on: route.path.includes('/rework') }">返修申请</router-link>
      <div class="right">
        <span class="tiny muted">{{ store.user?.realName }} · {{ store.user?.phone }}</span>
        <el-button link @click="logout">退出</el-button>
      </div>
    </div>
    <div class="p-wrap">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, store } from '../store'

const route = useRoute()
const router = useRouter()
function logout() {
  clearAuth()
  router.push('/login')
}
</script>
