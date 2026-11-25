<template>
  <div>
    <div class="page-head">
      <div><h1>预检规则配置</h1><p>六项因素权重合计必须等于 100，规则变更后对新预检生效</p></div>
      <div class="head-acts"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>当前生效规则</h3><span class="sub">{{ cur?.ruleVersion }} · {{ fmtDate(cur?.effectTime) }} 起</span></div>
          <div class="card-b" v-if="cur">
            <div v-for="f in factors" :key="f.k" class="factor">
              <span>{{ f.l }}</span>
              <div class="bar"><span :style="{ width: (f.v / 30 * 100) + '%' }"></span></div>
              <span class="num tiny">{{ f.v }} 分</span>
            </div>
            <div class="split"></div>
            <dl class="kv">
              <dt>推荐阈值</dt><dd class="num">≥ {{ cur.thresholdHigh }} 分</dd>
              <dt>不推荐阈值</dt><dd class="num">&lt; {{ cur.thresholdLow }} 分</dd>
              <dt>创建人</dt><dd>{{ cur.createBy }}</dd>
              <dt>生效时间</dt><dd class="num">{{ fmtTime(cur.effectTime) }}</dd>
            </dl>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>规则版本历史</h3><span class="sub">共 {{ rules.length }} 个版本</span></div>
          <el-table :data="rules" size="default">
            <el-table-column prop="ruleVersion" label="版本" min-width="80" />
            <el-table-column prop="wmodel" label="车型" min-width="70" align="right" />
            <el-table-column prop="wfault" label="故障" min-width="70" align="right" />
            <el-table-column prop="wproject" label="项目" min-width="70" align="right" />
            <el-table-column prop="whours" label="工时" min-width="70" align="right" />
            <el-table-column prop="wpart" label="配件" min-width="70" align="right" />
            <el-table-column prop="wstock" label="库存" min-width="70" align="right" />
            <el-table-column label="阈值" min-width="110">
              <template #default="{ row }"><span class="num tiny">{{ row.thresholdHigh }} / {{ row.thresholdLow }}</span></template>
            </el-table-column>
            <el-table-column label="生效时间" min-width="140">
              <template #default="{ row }"><span class="num">{{ fmtDate(row.effectTime) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" min-width="90">
              <template #default="{ row }">
                <span class="tag" :class="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '生效中' : '历史' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>新增规则版本</h3><span class="sub">合计 {{ sum }} / 100</span></div>
          <div class="card-b">
            <el-form label-width="88px">
              <el-form-item v-for="f in weights" :key="f.k" :label="f.l">
                <el-input-number v-model="form[f.k]" :min="0" :max="100" :step="5" style="width:100%" />
              </el-form-item>
              <el-form-item label="推荐阈值">
                <el-input-number v-model="form.thresholdHigh" :min="0" :max="100" style="width:100%" />
              </el-form-item>
              <el-form-item label="不推荐阈值">
                <el-input-number v-model="form.thresholdLow" :min="0" :max="100" style="width:100%" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="primary" style="width:100%" :disabled="sum !== 100" @click="save">
                {{ sum === 100 ? '保存并生效' : '权重合计需等于 100' }}
              </el-button>
            </el-form>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>预检统计</h3></div>
          <div class="card-b">
            <dl class="kv">
              <dt>预检总数</dt><dd class="num">{{ stats.total ?? 0 }}</dd>
              <dt>推荐 / 谨慎</dt><dd class="num">{{ stats.recommend ?? 0 }} / {{ stats.caution ?? 0 }}</dd>
              <dt>不推荐</dt><dd class="num">{{ stats.reject ?? 0 }}</dd>
              <dt>人工修正</dt><dd class="num">{{ stats.manual ?? 0 }} 次（{{ stats.manualRate ?? 0 }}%）</dd>
              <dt>已领用</dt><dd class="num">{{ stats.claimed ?? 0 }} 次</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'
import { fmtDate, fmtTime } from '../utils/format'

const rules = ref([])
const stats = ref({})
const form = reactive({ wmodel: 15, wfault: 20, wproject: 20, whours: 15, wpart: 20, wstock: 10, thresholdHigh: 80, thresholdLow: 60, remark: '' })

const weights = [
  { k: 'wmodel', l: '车型匹配' }, { k: 'wfault', l: '故障标签' }, { k: 'wproject', l: '诊断项目' },
  { k: 'whours', l: '历史工时' }, { k: 'wpart', l: '常用配件' }, { k: 'wstock', l: '当前库存' }
]
const factors = computed(() => weights.map((w) => ({ ...w, v: rules.value[0]?.[w.k] ?? 0 })))
const cur = computed(() => rules.value[0])
const sum = computed(() => weights.reduce((s, w) => s + Number(form[w.k] || 0), 0))

async function load() {
  rules.value = (await http.get('/precheck/rules')) || []
  stats.value = (await http.get('/precheck/stats')) || {}
}

async function save() {
  await http.post('/precheck/rules', { ...form })
  ElMessage.success('规则已生效')
  load()
}

onMounted(load)
</script>
