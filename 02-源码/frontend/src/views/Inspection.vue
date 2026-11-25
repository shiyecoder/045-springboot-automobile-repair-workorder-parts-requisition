<template>
  <div v-if="o.id">
    <div class="page-head">
      <div>
        <h1>质量检验</h1>
        <p>{{ o.orderNo }} · {{ o.plateNo }} · {{ o.vehicleName }} · 当前状态 {{ o.statusName }}</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.push(`/orders/${o.id}`)">工单详情</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>项目完工情况</h3><span class="sub">共 {{ (o.items || []).length }} 项</span></div>
          <el-table :data="o.items || []" size="default">
            <el-table-column prop="projectName" label="维修项目" min-width="190" />
            <el-table-column label="工种" min-width="90">
              <template #default="{ row }">{{ label('work_type', row.workType) }}</template>
            </el-table-column>
            <el-table-column prop="hours" label="工时" min-width="80" align="right" />
            <el-table-column label="完工" min-width="90">
              <template #default="{ row }">
                <span class="tag" :class="row.itemStatus === 'DONE' ? 'success' : 'warning'">
                  {{ row.itemStatus === 'DONE' ? '已完成' : '未完成' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card">
          <div class="card-h"><h3>检验历史</h3></div>
          <el-table :data="o.qcList || []" size="default">
            <el-table-column label="结果" min-width="100">
              <template #default="{ row }">
                <span class="tag" :class="row.qcResult === 'PASS' ? 'success' : 'danger'">{{ label('qc_result', row.qcResult) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="qcDesc" label="检验说明" min-width="300" show-overflow-tooltip />
            <el-table-column label="检验时间" min-width="140">
              <template #default="{ row }"><span class="num">{{ fmtTime(row.qcTime) }}</span></template>
            </el-table-column>
          </el-table>
          <div v-if="!(o.qcList || []).length" class="empty">尚未检验</div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>提交检验结果</h3></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item label="检验结论">
                <el-radio-group v-model="form.qcResult">
                  <el-radio value="PASS">合格</el-radio>
                  <el-radio value="FAIL">不合格</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="检验说明">
                <el-input v-model="form.qcDesc" type="textarea" :rows="3"
                          placeholder="逐项核对工艺、安全检查清单" />
              </el-form-item>
              <el-form-item label="不合格项" v-if="form.qcResult === 'FAIL'">
                <el-select v-model="form.failItemIds" multiple style="width:100%" placeholder="选择需返工的项目">
                  <el-option v-for="i in o.items || []" :key="i.id" :label="i.projectName" :value="i.id" />
                </el-select>
              </el-form-item>
              <el-button type="primary" style="width:100%" @click="submitQc">提交检验</el-button>
            </el-form>
            <div class="split"></div>
            <div class="tiny muted" style="line-height:1.9">
              合格 → 工单进入「交车结算」；不合格 → 自动转「返工中」，必须填写不少于 5 字的返工原因。
            </div>
          </div>
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
import { label } from '../dict'
import { fmtTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const o = ref({})
const form = reactive({ qcResult: 'PASS', qcDesc: '全部项目合格，安全检查清单通过', failItemIds: [] })

async function load() {
  o.value = await http.get(`/order/${route.params.id}`)
}

async function submitQc() {
  if (!form.qcDesc) return ElMessage.warning('请填写检验说明')
  await http.post('/order/qc', { orderId: o.value.id, qcResult: form.qcResult, qcDesc: form.qcDesc, failItemIds: form.failItemIds })
  ElMessage.success('检验已提交')
  load()
}

onMounted(load)
</script>
