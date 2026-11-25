<template>
  <div>
    <div class="page-head">
      <div><h1>库存批次</h1><p>按批次管理入库与剩余量，领用时优先消耗早期批次</p></div>
      <div class="head-acts"><el-button type="primary" @click="dlg = true">批次入库</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>批次列表</h3><span class="sub">共 {{ total }} 个批次</span></div>
      <el-table :data="rows" size="default">
        <el-table-column prop="batch.batchNo" label="批次号" min-width="150" />
        <el-table-column prop="partName" label="配件" min-width="170" show-overflow-tooltip />
        <el-table-column prop="partCode" label="编码" min-width="110">
          <template #default="{ row }"><span class="num tiny">{{ row.partCode }}</span></template>
        </el-table-column>
        <el-table-column prop="batch.warehouse" label="仓库" min-width="100" />
        <el-table-column label="入库数量" min-width="100" align="right">
          <template #default="{ row }"><span class="num">{{ row.batch.inQty }} {{ row.unit }}</span></template>
        </el-table-column>
        <el-table-column label="剩余" min-width="100" align="right">
          <template #default="{ row }">
            <span class="num strong" :style="{ color: Number(row.batch.leftQty) <= 0 ? 'var(--dan)' : 'inherit' }">
              {{ row.batch.leftQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="进价" min-width="100" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.batch.purchasePrice) }}</span></template>
        </el-table-column>
        <el-table-column prop="batch.supplier" label="供应商" min-width="130" show-overflow-tooltip />
        <el-table-column label="入库时间" min-width="140">
          <template #default="{ row }"><span class="num">{{ fmtTime(row.batch.inTime) }}</span></template>
        </el-table-column>
        <el-table-column label="有效期至" min-width="120">
          <template #default="{ row }"><span class="num">{{ fmtDate(row.batch.expireDate) }}</span></template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="批次入库" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="配件" required>
          <el-select v-model="form.partId" filterable style="width:100%">
            <el-option v-for="p in parts" :key="p.id" :label="`${p.partName}（${p.partCode}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号" required><el-input v-model="form.batchNo" placeholder="如：B2026110101" /></el-form-item>
        <div class="grid-2">
          <el-form-item label="入库数量"><el-input v-model.number="form.inQty" type="number" /></el-form-item>
          <el-form-item label="仓库"><el-input v-model="form.warehouse" /></el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="进价"><el-input v-model.number="form.purchasePrice" type="number" /></el-form-item>
          <el-form-item label="有效期">
            <el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="供应商"><el-input v-model="form.supplier" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { fmtDate, fmtTime, money } from '../utils/format'

const route = useRoute()
const rows = ref([])
const total = ref(0)
const parts = ref([])
const dlg = ref(false)
const q = reactive({ page: 1, size: 10 })
const form = reactive({ partId: null, batchNo: '', inQty: 10, warehouse: '主仓', purchasePrice: 0, supplier: '', expireDate: '' })

async function load() {
  const d = await http.get('/meta/batch/page', { params: { ...q } })
  let list = d.records || []
  if (route.query.partId) list = list.filter((r) => String(r.batch.partId) === String(route.query.partId))
  rows.value = list
  total.value = d.total || 0
}

async function save() {
  if (!form.partId || !form.batchNo) return ElMessage.warning('请填写配件与批次号')
  await http.post('/meta/batch/inbound', { ...form })
  ElMessage.success('入库成功，库存已增加')
  dlg.value = false
  load()
}

onMounted(async () => {
  parts.value = (await http.get('/meta/options')).parts || []
  load()
})
</script>
