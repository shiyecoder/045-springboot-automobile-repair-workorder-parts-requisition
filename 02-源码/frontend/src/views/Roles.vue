<template>
  <div>
    <div class="page-head">
      <div><h1>角色管理</h1><p>角色决定菜单可见范围与数据范围，分配后重新登录生效</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>角色列表</h3><span class="sub">共 {{ roles.length }} 个角色</span></div>
      <el-table :data="roles" size="default">
        <el-table-column prop="roleCode" label="角色编码" min-width="150" />
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="dataScope" label="数据范围" min-width="120">
          <template #default="{ row }">
            {{ { all: '全部门店', store: '本门店', self: '仅本人' }[row.dataScope] || row.dataScope }}
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" min-width="90" align="right" />
        <el-table-column prop="menuCount" label="菜单数" min-width="90" align="right" />
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMenu(row)">分配菜单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="dlg" :title="`分配菜单 · ${curRole?.roleName || ''}`" size="480px">
      <el-tree ref="treeRef" :data="tree" show-checkbox node-key="id" default-expand-all
               :props="{ label: 'label', children: 'children' }" />
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="saveMenu">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'

const roles = ref([])
const tree = ref([])
const dlg = ref(false)
const curRole = ref(null)
const treeRef = ref(null)

async function load() {
  roles.value = (await http.get('/system/role/list')) || []
}

/** 把扁平菜单建成树；「菜单管理」本身不作为可分配项（规范要求隐藏）。 */
function buildTree(list) {
  const nodes = list
    .filter((m) => m.name !== '菜单管理' && m.path !== '/menus')
    .map((m) => ({ id: m.id, label: m.name, parentId: m.parentId, children: [] }))
  const map = new Map(nodes.map((n) => [n.id, n]))
  const roots = []
  nodes.forEach((n) => {
    const p = map.get(n.parentId)
    if (p) p.children.push(n)
    else roots.push(n)
  })
  return roots
}

async function openMenu(row) {
  curRole.value = row
  const flat = (await http.get('/system/menu/list')) || []
  tree.value = buildTree(flat)
  const owned = (await http.get(`/system/role/${row.id}/menu`)) || []
  dlg.value = true
  setTimeout(() => {
    const leafIds = new Set(flat.map((m) => m.id))
    treeRef.value?.setCheckedKeys((owned || []).filter((id) => leafIds.has(id)), true)
  }, 120)
}

async function saveMenu() {
  const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await http.put(`/system/role/${curRole.value.id}/menu`, { menuIds: ids })
  ElMessage.success('菜单权限已保存')
  dlg.value = false
  load()
}

onMounted(load)
</script>
