<template>
  <div>
    <div class="page-head">
      <div><h1>用户管理</h1><p>系统账号与角色绑定，重置密码后为初始密码 123456</p></div>
      <div class="head-acts">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openEdit()">新增用户</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="角色">
          <el-select v-model="q.roleId" clearable placeholder="全部角色" @change="load">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="登录名 / 姓名 / 手机号" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>账号列表</h3><span class="sub">共 {{ total }} 个</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="username" label="登录名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="phone" label="手机号" min-width="125" />
        <el-table-column prop="roleName" label="角色" min-width="110">
          <template #default="{ row }"><span class="tag primary">{{ row.roleName }}</span></template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属门店" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="150">
          <template #default="{ row }"><span class="num muted">{{ fmtTime(row.lastLogin) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>

    <el-drawer v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" size="460px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="登录名" required><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleIds" multiple style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属门店"><el-input v-model="form.orgName" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="初始密码" v-if="!form.id">
          <el-input v-model="form.password" placeholder="留空则为 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../http'
import { fmtTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const roles = ref([])
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, roleId: null, keyword: '' })
const form = reactive({ id: null, username: '', realName: '', phone: '', email: '', roleIds: [], orgName: '车维通 · 南山旗舰店', status: 1, password: '' })

async function load() {
  const d = await http.get('/system/user/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
}

function reset() {
  q.roleId = null
  q.keyword = ''
  q.page = 1
  load()
}

function openEdit(row) {
  Object.assign(form, row
    ? { id: row.id, username: row.username, realName: row.realName, phone: row.phone, email: row.email, roleIds: row.roleId ? [row.roleId] : [], orgName: row.orgName, status: row.status, password: '' }
    : { id: null, username: '', realName: '', phone: '', email: '', roleIds: [], orgName: '车维通 · 南山旗舰店', status: 1, password: '' })
  dlg.value = true
}

async function save() {
  if (!form.username || !form.realName) return ElMessage.warning('请填写登录名与姓名')
  if (!form.id && !form.password) form.password = '123456'
  await http.post('/system/user', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

async function resetPwd(row) {
  await ElMessageBox.confirm(`将 ${row.realName} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await http.put(`/system/user/${row.id}/reset`)
  ElMessage.success('密码已重置为 123456')
}

onMounted(async () => {
  roles.value = (await http.get('/system/role/list')) || []
  load()
})
</script>
