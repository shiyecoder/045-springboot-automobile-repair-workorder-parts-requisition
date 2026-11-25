<template>
  <div class="login-page">
    <div class="login-art">
      <img :src="bg" alt="车间背景" />
      <div class="veil"></div>
      <div class="brand">
        <div class="mark">车</div>
        <div>
          <b>车维通 CarRepair</b>
          <span>AUTO SERVICE WORKSHOP SYSTEM</span>
        </div>
      </div>
      <div class="copy">
        <h2>从接车到交车，一张工单走完全流程</h2>
        <p>
          接车登记 · 故障诊断 · 报价确认 · 派工领料 · 质量检验 · 交车结算<br />
          配件领用与预检推荐联动，库存实时扣减，返工返修全程留痕。
        </p>
      </div>
    </div>

    <div class="login-form-wrap">
      <div class="login-form">
        <h1>登录车维通</h1>
        <p class="tip">门店内部账号登录后进入管理后台，车主账号进入车主中心</p>
        <el-form :model="form" @submit.prevent>
          <el-form-item>
            <el-input v-model="form.username" placeholder="登录名" size="large" autofocus />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large"
                      @keyup.enter="submit" />
          </el-form-item>
          <div class="row">
            <el-checkbox v-model="form.remember">记住登录名</el-checkbox>
            <span class="muted">忘记密码请联系门店经理</span>
          </div>
          <el-button type="primary" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="accounts">
          演示账号：<b>admin / admin123</b>（门店经理）<br />
          服务顾问 service01 · 技师 tech01 · 仓管 store01 · 质检 qc01 · 车主 owner02，密码均为 <b>123456</b>
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
import { setAuth } from '../store'
import bg from '../assets/login-bg.jpg'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: localStorage.getItem('ar_last_user') || '', password: '', remember: true })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入登录名与密码')
    return
  }
  loading.value = true
  try {
    const res = await http.post('/auth/login', { username: form.username, password: form.password })
    setAuth(res.token, res.user, res.menus)
    if (form.remember) localStorage.setItem('ar_last_user', form.username)
    ElMessage.success(`欢迎回来，${res.user.realName}`)
    const target = res.user.roleCode === 'ROLE_OWNER' ? '/portal' : (route.query.redirect || '/dashboard/index')
    router.push(target)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (localStorage.getItem('ar_token')) router.push('/dashboard/index')
})
</script>
