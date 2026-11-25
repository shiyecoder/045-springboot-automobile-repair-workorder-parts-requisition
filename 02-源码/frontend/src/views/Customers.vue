<template>
  <div>
    <div class="page-head">
      <div>
        <h1>客户档案</h1>
        <p>会员与普通客户统一维护，消费金额随工单结算自动累加</p>
      </div>
      <div class="head-acts">
        <el-button type="primary" @click="openEdit()">新增客户</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="客户类型">
          <el-select v-model="q.customerType" clearable placeholder="全部类型" @change="load">
            <el-option v-for="o in options('customer_type')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="姓名 / 手机号 / 客户编号" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>客户列表</h3><span class="sub">共 {{ total }} 位客户</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="customerNo" label="客户编号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.customerType === 'MEMBER' ? 'primary' : 'info'">
              {{ label('customer_type', row.customerType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="120" show-overflow-tooltip />
        <el-table-column prop="visitCount" label="到店次数" min-width="90" align="right" />
        <el-table-column label="累计消费" min-width="130" align="right">
          <template #default="{ row }"><span class="num strong">{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="建档时间" min-width="140">
          <template #default="{ row }"><span class="num muted">{{ fmtTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="router.push(`/vehicles?customerId=${row.id}`)">名下车辆</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>

    <el-drawer v-model="dlg" :title="form.id ? '编辑客户' : '新增客户'" size="440px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="form.phone" maxlength="11" /></el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="form.customerType" style="width:100%">
            <el-option v-for="o in options('customer_type')" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户来源"><el-input v-model="form.source" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { label, options } from '../dict'
import { fmtTime, money } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, customerType: '', keyword: '' })
const form = reactive({ id: null, name: '', phone: '', customerType: 'NORMAL', source: '', remark: '' })

async function load() {
  const d = await http.get('/meta/customer/page', { params: { page: q.page, size: q.size } })
  let list = d.records || []
  if (q.customerType) list = list.filter((r) => r.customerType === q.customerType)
  if (q.keyword) {
    const k = q.keyword
    list = list.filter((r) => (r.name || '').includes(k) || (r.phone || '').includes(k) || (r.customerNo || '').includes(k))
  }
  rows.value = list
  total.value = d.total || 0
}

function reset() {
  q.customerType = ''
  q.keyword = ''
  q.page = 1
  load()
}

function openEdit(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', phone: '', customerType: 'NORMAL', source: '', remark: '' })
  dlg.value = true
}

async function save() {
  if (!form.name || !form.phone) return ElMessage.warning('请填写姓名与手机号')
  await http.post('/meta/customer', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

onMounted(load)
</script>
