<template>
  <div>
    <div class="page-head">
      <div>
        <h1>配件档案</h1>
        <p>销售价、库存与安全上下限统一维护，低于下限进入预警</p>
      </div>
      <div class="head-acts">
        <el-button type="primary" @click="openEdit()">新增配件</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>库存预警</h3><span class="sub">{{ warnings.length }} 项低于安全库存</span></div>
      <div class="card-b tight">
        <div class="flex gap" style="flex-wrap:wrap">
          <span v-for="w in warnings" :key="w.partCode" class="tag danger">
            {{ w.partName }} 余 {{ w.stockQty }}{{ w.unit }}（下限 {{ w.minQty }}）
          </span>
          <span v-if="!warnings.length" class="tiny muted">当前库存充足</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="配件名称 / 编码 / 适用车型" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="card-h"><h3>配件列表</h3><span class="sub">共 {{ total }} 种</span></div>
      <el-table :data="rows" size="default">
        <el-table-column label="图片" width="72">
          <template #default="{ row }"><Thumb :src="row.image" fallback="/uploads/seed/part/01.jpg" /></template>
        </el-table-column>
        <el-table-column prop="partCode" label="配件编码" min-width="110" />
        <el-table-column prop="partName" label="配件名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applyModels" label="适用车型" min-width="150" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" min-width="70" />
        <el-table-column label="进价" min-width="100" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.purchasePrice) }}</span></template>
        </el-table-column>
        <el-table-column label="销售价" min-width="105" align="right">
          <template #default="{ row }"><span class="num strong">{{ money(row.salePrice) }}</span></template>
        </el-table-column>
        <el-table-column label="库存" min-width="95" align="right">
          <template #default="{ row }">
            <span class="num" :style="{ color: Number(row.stockQty) <= Number(row.minQty) ? 'var(--dan)' : 'inherit' }">
              {{ row.stockQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="minQty" label="下限" min-width="80" align="right" />
        <el-table-column prop="maxQty" label="上限" min-width="80" align="right" />
        <el-table-column label="操作" min-width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="router.push(`/part/batches?partId=${row.id}`)">批次</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>

    <el-drawer v-model="dlg" :title="form.id ? '编辑配件' : '新增配件'" size="460px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="配件编码" required><el-input v-model="form.partCode" /></el-form-item>
        <el-form-item label="配件名称" required><el-input v-model="form.partName" /></el-form-item>
        <el-form-item label="适用车型"><el-input v-model="form.applyModels" /></el-form-item>
        <div class="grid-2">
          <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
          <el-form-item label="进价"><el-input v-model.number="form.purchasePrice" /></el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="销售价"><el-input v-model.number="form.salePrice" /></el-form-item>
          <el-form-item label="下限"><el-input v-model.number="form.minQty" /></el-form-item>
        </div>
        <el-form-item label="上限"><el-input v-model.number="form.maxQty" /></el-form-item>
        <el-form-item label="配件图片">
          <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
            <el-button>上传图片</el-button>
          </el-upload>
          <img v-if="form.image" :src="form.image"
               style="width:64px;height:48px;object-fit:cover;border-radius:4px;margin-left:8px" />
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Thumb from '../components/Thumb.vue'
import http from '../http'
import { money } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const warnings = ref([])
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '' })
const form = reactive({ id: null, partCode: '', partName: '', applyModels: '', unit: '个', purchasePrice: 0, salePrice: 0, minQty: 5, maxQty: 100, image: '', status: 1 })

async function load() {
  const d = await http.get('/meta/part/page', { params: { ...q } })
  rows.value = d.records || []
  total.value = d.total || 0
  warnings.value = (await http.get('/meta/stock/warnings')) || []
}

function reset() {
  q.keyword = ''
  q.page = 1
  load()
}

function openEdit(row) {
  Object.assign(form, row ? { ...row } : { id: null, partCode: '', partName: '', applyModels: '', unit: '个', purchasePrice: 0, salePrice: 0, minQty: 5, maxQty: 100, image: '', status: 1 })
  dlg.value = true
}

async function upload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  form.image = typeof res === 'string' ? res : (res?.url || res?.path || '')
}

async function save() {
  if (!form.partCode || !form.partName) return ElMessage.warning('请填写编码与名称')
  await http.post('/meta/part', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

onMounted(load)
</script>
