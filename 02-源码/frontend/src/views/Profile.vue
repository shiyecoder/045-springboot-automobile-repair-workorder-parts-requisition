<template>
  <div>
    <div class="page-head">
      <div><h1>个人信息</h1><p>修改资料、头像与登录密码，头像同步显示在右上角</p></div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>基本资料</h3></div>
          <div class="card-b">
            <el-form :model="form" label-width="104px">
              <el-form-item label="登录名">
                <el-input :model-value="user?.username" disabled />
              </el-form-item>
              <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
              <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11" /></el-form-item>
              <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
              <el-form-item label="头像">
                <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
                  <el-button>上传头像</el-button>
                </el-upload>
                <img v-if="form.avatar" :src="form.avatar" class="avatar" />
              </el-form-item>
              <el-button type="primary" @click="saveProfile">保存资料</el-button>
            </el-form>
          </div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>我的账号</h3></div>
          <div class="card-b">
            <div class="flex gap">
              <img :src="img(form.avatar, '/uploads/seed/avatar/01.jpg')" class="avatar lg" />
              <div>
                <div class="strong">{{ user?.realName }}</div>
                <div class="tiny muted">{{ user?.roleName }} · {{ user?.orgName }}</div>
              </div>
            </div>
            <div class="split"></div>
            <dl class="kv">
              <dt>数据范围</dt><dd>{{ { all: '全部门店', store: '本门店', self: '仅本人' }[user?.dataScope] || user?.dataScope }}</dd>
              <dt>所属门店</dt><dd>{{ user?.orgName }}</dd>
              <dt>手机号</dt><dd class="num">{{ user?.phone }}</dd>
            </dl>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>修改密码</h3></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
              <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
              <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
              <el-button type="primary" @click="savePwd">修改密码</el-button>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'
import { setUser, store } from '../store'
import { img } from '../utils/format'

const user = store.user
const form = reactive({ realName: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function load() {
  const me = await http.get('/auth/me')
  if (me?.user) setUser(me.user)
  Object.assign(form, {
    realName: store.user?.realName || '',
    phone: store.user?.phone || '',
    email: store.user?.email || '',
    avatar: store.user?.avatar || ''
  })
}

async function upload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  form.avatar = typeof res === 'string' ? res : (res?.url || res?.path || '')
}

async function saveProfile() {
  if (!form.realName) return ElMessage.warning('姓名不能为空')
  await http.put('/auth/profile', { ...form })
  setUser({ ...store.user, ...form })
  ElMessage.success('资料已更新')
}

async function savePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await http.put('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  Object.assign(pwd, { oldPassword: '', newPassword: '', confirm: '' })
}

onMounted(load)
</script>

<style scoped>
.avatar { width: 64px; height: 64px; border-radius: 8px; object-fit: cover; margin-left: 12px; border: 1px solid var(--line); }
.avatar.lg { width: 56px; height: 56px; border-radius: 50%; margin-left: 0; }
</style>
