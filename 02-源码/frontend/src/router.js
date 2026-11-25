import { createRouter, createWebHashHistory } from 'vue-router'
import { store } from './store'

/**
 * 路由表：管理端（后台风格）与车主门户（前台风格）两套布局。
 * 侧边栏内容来自登录接口返回的菜单，这里只负责组件映射与标题。
 */
const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { title: '登录', public: true } },
  {
    path: '/',
    component: () => import('./layout/AdminLayout.vue'),
    redirect: '/dashboard/index',
    children: [
      { path: 'dashboard/index', component: () => import('./views/Dashboard.vue'), meta: { title: '维修经营分析' } },
      { path: 'todo', component: () => import('./views/Todo.vue'), meta: { title: '我的待办' } },
      { path: 'appointments', component: () => import('./views/Appointments.vue'), meta: { title: '预约接车台' } },
      { path: 'reception/new', component: () => import('./views/Reception.vue'), meta: { title: '接车登记' } },
      { path: 'customers', component: () => import('./views/Customers.vue'), meta: { title: '客户档案' } },
      { path: 'vehicles', component: () => import('./views/Vehicles.vue'), meta: { title: '维修车辆' } },
      { path: 'vehicles/:id', component: () => import('./views/VehicleDetail.vue'), meta: { title: '车辆详情' } },
      { path: 'orders', component: () => import('./views/Orders.vue'), meta: { title: '维修工单' } },
      { path: 'orders/:id', component: () => import('./views/OrderDetail.vue'), meta: { title: '工单详情' } },
      { path: 'diagnosis/:id', component: () => import('./views/Diagnosis.vue'), meta: { title: '故障诊断与检测' } },
      { path: 'quote/:id', component: () => import('./views/Quote.vue'), meta: { title: '报价与客户确认' } },
      { path: 'dispatch', component: () => import('./views/Dispatch.vue'), meta: { title: '派工看板' } },
      { path: 'inspection/:id', component: () => import('./views/Inspection.vue'), meta: { title: '质量检验' } },
      { path: 'settlement/:id', component: () => import('./views/Settlement.vue'), meta: { title: '交车结算' } },
      { path: 'parts', component: () => import('./views/Parts.vue'), meta: { title: '配件档案' } },
      { path: 'part/claim/:id', component: () => import('./views/PartClaim.vue'), meta: { title: '领用与预检' } },
      { path: 'part/flows', component: () => import('./views/PartFlows.vue'), meta: { title: '领退记录' } },
      { path: 'part/batches', component: () => import('./views/Batches.vue'), meta: { title: '库存批次' } },
      { path: 'warranties', component: () => import('./views/Warranties.vue'), meta: { title: '质保台账' } },
      { path: 'returns', component: () => import('./views/Returns.vue'), meta: { title: '返修记录' } },
      { path: 'analysis', component: () => import('./views/Analysis.vue'), meta: { title: '经营分析报表' } },
      { path: 'rules', component: () => import('./views/Rules.vue'), meta: { title: '预检规则配置' } },
      { path: 'projects', component: () => import('./views/Projects.vue'), meta: { title: '维修项目库' } },
      { path: 'staff', component: () => import('./views/Staff.vue'), meta: { title: '员工与技师' } },
      { path: 'users', component: () => import('./views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'roles', component: () => import('./views/Roles.vue'), meta: { title: '角色管理' } },
      { path: 'menus', component: () => import('./views/Menus.vue'), meta: { title: '菜单管理' } },
      { path: 'messages', component: () => import('./views/Messages.vue'), meta: { title: '消息中心' } },
      { path: 'profile', component: () => import('./views/Profile.vue'), meta: { title: '个人信息' } }
    ]
  },
  {
    path: '/portal',
    component: () => import('./layout/PortalLayout.vue'),
    children: [
      { path: '', component: () => import('./views/portal/Home.vue'), meta: { title: '我的爱车' } },
      { path: 'orders', component: () => import('./views/portal/MyOrders.vue'), meta: { title: '维修进度' } },
      { path: 'quotes', component: () => import('./views/portal/MyQuotes.vue'), meta: { title: '报价确认' } },
      { path: 'rework', component: () => import('./views/portal/MyRework.vue'), meta: { title: '返修申请' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard/index' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!store.token) return { path: '/login', query: { redirect: to.fullPath } }
  // 车主只能进门户，员工不能进门户
  if (to.path.startsWith('/portal') && store.user?.roleCode !== 'ROLE_OWNER') return '/dashboard/index'
  if (!to.path.startsWith('/portal') && store.user?.roleCode === 'ROLE_OWNER') return '/portal'
  return true
})

export default router
