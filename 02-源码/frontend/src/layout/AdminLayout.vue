<template>
  <div class="app">
    <aside class="side">
      <div class="side-logo">
        <div class="mark">车</div>
        <div>
          <b>车维通</b>
          <span>维修工单 · 配件领用</span>
        </div>
      </div>
      <div class="side-scroll">
        <template v-for="g in nav" :key="g.id">
          <div class="side-group">{{ g.name }}</div>
          <router-link v-for="m in g.items" :key="m.id" :to="m.path"
                       class="side-item" :class="{ on: isOn(m.path) }">
            <Icon :name="m.icon" />
            <span>{{ m.name }}</span>
          </router-link>
        </template>
      </div>
    </aside>

    <div class="main">
      <div class="top">
        <div class="crumb">
          车维通 · 汽车维修工单与配件领用管理系统 / <b>{{ route.meta.title || '' }}</b>
        </div>
        <div class="top-right">
          <div class="bell" @click="router.push('/messages')">
            <Icon name="bell" :size="17" />
            <i v-if="unread">{{ unread }}</i>
          </div>
          <el-dropdown trigger="click" @command="onCmd">
            <div class="who">
              <img :src="avatar" alt="头像" />
              <div>
                <div class="nm">{{ store.user?.realName }}</div>
                <div class="rl">{{ store.user?.roleName }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="messages">消息中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="route.fullPath" />
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { clearAuth, setUser, store } from '../store'
import http from '../http'
import { img } from '../utils/format'

const route = useRoute()
const router = useRouter()
const unread = ref(0)

// 详情类路由带参数，不在侧栏出现
const HIDDEN = ['/vehicles/:id', '/orders/:id', '/diagnosis/:id', '/quote/:id',
  '/inspection/:id', '/settlement/:id', '/part/claim/:id', '/reception/new']

/** 侧栏由登录返回的动态菜单生成（按角色不同），不是前端写死的。 */
const nav = computed(() =>
  (store.menus || []).map((g) => ({
    id: g.id,
    name: g.name,
    items: (g.children || [])
      .filter((c) => c.type === 'menu' && c.path && !HIDDEN.includes(c.path))
      .map((c) => ({ ...c, icon: c.icon || 'clip' }))
  })).filter((g) => g.items.length)
)

const avatar = computed(() => img(store.user?.avatar, '/uploads/seed/avatar/01.jpg'))
const isOn = (p) => route.path === p || route.path.startsWith(p + '/')

function onCmd(c) {
  if (c === 'logout') {
    clearAuth()
    router.push('/login')
  } else if (c === 'messages') {
    router.push('/messages')
  } else {
    router.push('/profile')
  }
}

onMounted(async () => {
  try {
    const me = await http.get('/auth/me')
    if (me?.user) setUser(me.user)
    const list = await http.get('/system/message/page', { params: { page: 1, size: 1 } })
    unread.value = (list?.records || []).filter((m) => !m.readFlag).length
  } catch (e) {
    /* 非经理账号无权读消息，忽略 */
  }
})
</script>
