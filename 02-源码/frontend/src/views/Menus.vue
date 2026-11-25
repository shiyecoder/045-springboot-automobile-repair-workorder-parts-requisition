<template>
  <div>
    <div class="page-head">
      <div><h1>菜单管理</h1><p>菜单与权限标识决定侧边栏结构，按角色动态下发</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>菜单结构</h3><span class="sub">{{ flat.length }} 个节点</span></div>
      <el-table :data="tree" row-key="id" default-expand-all size="default">
        <el-table-column prop="name" label="菜单名称" min-width="220" />
        <el-table-column prop="path" label="路由路径" min-width="200">
          <template #default="{ row }"><span class="num tiny">{{ row.path }}</span></template>
        </el-table-column>
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.type === 'catalog' ? 'info' : 'primary'">
              {{ row.type === 'catalog' ? '目录' : '菜单' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="perm" label="权限标识" min-width="160">
          <template #default="{ row }"><span class="num tiny">{{ row.perm }}</span></template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" min-width="80" align="right" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../http'

const flat = ref([])

/** 扁平 → 树，供表格展开显示 */
const tree = computed(() => {
  const nodes = flat.value.map((m) => ({ ...m, children: [] }))
  const map = new Map(nodes.map((n) => [n.id, n]))
  const roots = []
  nodes.forEach((n) => {
    const p = map.get(n.parentId)
    if (p) p.children.push(n)
    else roots.push(n)
  })
  return roots
})

async function load() {
  flat.value = (await http.get('/system/menu/list')) || []
}
onMounted(load)
</script>
