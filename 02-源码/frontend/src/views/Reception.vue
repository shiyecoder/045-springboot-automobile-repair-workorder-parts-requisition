<template>
  <div>
    <div class="page-head">
      <div>
        <h1>接车登记</h1>
        <p>进厂里程不得小于上次记录，现场照片至少 1 张</p>
      </div>
      <div class="head-acts">
        <el-button @click="router.back()">返回</el-button>
      </div>
    </div>

    <div class="grid-detail">
      <div>
        <div class="card">
          <div class="card-h"><h3>车辆与客户</h3><span class="sub">必填项标 *</span></div>
          <div class="card-b">
            <el-form :model="form" label-width="104px">
              <el-form-item label="选择车辆" required>
                <el-select v-model="form.vehicleId" filterable placeholder="输入车牌搜索" style="width:100%"
                           @change="onVehicle">
                  <el-option v-for="v in vehicles" :key="v.id"
                             :label="`${v.plateNo} · ${v.brand} ${v.model} · ${v.mileage} km`" :value="v.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="业务类型">
                <el-radio-group v-model="form.orderType">
                  <el-radio-button v-for="o in options('order_type')" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <div class="grid-2">
                <el-form-item label="进厂里程" required>
                  <el-input v-model.number="form.mileage" placeholder="km">
                    <template #append>km</template>
                  </el-input>
                </el-form-item>
                <el-form-item label="油量">
                  <el-select v-model="form.fuelLevel" style="width:100%">
                    <el-option v-for="f in ['空','1/4','1/2','3/4','满']" :key="f" :label="f" :value="f" />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="故障描述" required>
                <el-input v-model="form.faultDesc" type="textarea" :rows="2" placeholder="客户主诉与初步判断" />
              </el-form-item>
              <div class="grid-2">
                <el-form-item label="预计交车">
                  <el-date-picker v-model="form.expectFinish" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                                  style="width:100%" />
                </el-form-item>
                <el-form-item label="服务顾问">
                  <el-select v-model="form.advisorId" style="width:100%">
                    <el-option v-for="a in advisors" :key="a.id" :label="a.realName" :value="a.id" />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="外观划痕">
                <el-input v-model="form.scratchDesc" placeholder="如：左前翼子板 3cm 划痕（旧）" />
              </el-form-item>
              <el-form-item label="随车物品">
                <el-input v-model="form.itemsDesc" placeholder="如：行车记录仪 1、儿童安全座椅 1" />
              </el-form-item>
              <el-form-item label="现场照片" required>
                <el-upload :show-file-list="false" :http-request="upload" :before-upload="beforeUpload"
                           list-type="picture-card" accept="image/*" class="up">
                  <div class="up-inner">+ 上传</div>
                </el-upload>
                <div class="pics" v-if="form.photos.length">
                  <div v-for="(p, i) in form.photos" :key="p" class="pic">
                    <img :src="p" alt="现场照片" />
                    <span @click="form.photos.splice(i, 1)">×</span>
                  </div>
                </div>
                <div class="tiny muted" style="width:100%">照片保存在项目根目录 uploads/ 下，不允许外链</div>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>车辆档案</h3></div>
          <div class="card-b">
            <template v-if="current">
              <img :src="img(current.photo)" style="width:100%;height:132px;object-fit:cover;border-radius:6px" />
              <div class="split"></div>
              <dl class="kv">
                <dt>车牌</dt><dd class="strong">{{ current.plateNo }}</dd>
                <dt>车型</dt><dd>{{ current.brand }} {{ current.model }}</dd>
                <dt>VIN</dt><dd class="num tiny">{{ current.vin }}</dd>
                <dt>上次里程</dt><dd class="num">{{ num(current.mileage) }} km</dd>
                <dt>购车日期</dt><dd class="num">{{ fmtDate(current.buyDate) }}</dd>
                <dt>客户</dt><dd>{{ customerName }}</dd>
              </dl>
            </template>
            <div v-else class="empty">请选择车辆查看档案</div>
          </div>
        </div>
        <div class="card">
          <div class="card-h"><h3>提交</h3></div>
          <div class="card-b">
            <div class="tiny muted" style="line-height:1.9">
              提交后系统生成工单号并置为「接车登记」状态，服务顾问可继续做故障诊断与报价。
            </div>
            <el-button type="primary" style="width:100%;margin-top:10px" :loading="saving" @click="submit">
              提交并生成工单
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../http'
import { options } from '../dict'
import { fmtDate, img, num } from '../utils/format'

const route = useRoute()
const router = useRouter()
const vehicles = ref([])
const advisors = ref([])
const customers = ref({})
const saving = ref(false)
const form = reactive({
  vehicleId: null, orderType: 'MAINTAIN', mileage: null, fuelLevel: '1/2',
  scratchDesc: '', itemsDesc: '', photos: [], faultDesc: '', expectFinish: '', advisorId: null,
  appointmentId: null
})

const current = computed(() => vehicles.value.find((v) => v.id === form.vehicleId))
const customerName = computed(() => customers.value[current.value?.customerId] || '—')

function onVehicle(id) {
  const v = vehicles.value.find((x) => x.id === id)
  if (v) form.mileage = v.mileage
}

function beforeUpload(file) {
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning('单张照片不超过 8MB')
    return false
  }
  return true
}

async function upload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  const url = typeof res === 'string' ? res : (res?.url || res?.path || res)
  form.photos.push(url)
}

async function submit() {
  if (!form.vehicleId) return ElMessage.warning('请选择车辆')
  if (!form.mileage) return ElMessage.warning('请填写进厂里程')
  if (!form.photos.length) return ElMessage.warning('请至少上传 1 张现场照片')
  const prev = current.value?.mileage || 0
  if (form.mileage < prev) return ElMessage.error(`进厂里程不得小于上次记录 ${prev} km`)
  saving.value = true
  try {
    await http.post('/order/receive', { ...form })
    ElMessage.success('工单已创建')
    router.push('/orders')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await http.get('/meta/options')
  vehicles.value = o.vehicles || []
  advisors.value = o.advisors || []
  const cs = await http.get('/meta/customer/page', { params: { page: 1, size: 200 } })
  ;(cs.records || []).forEach((c) => { customers.value[c.id] = c.name })
  form.advisorId = advisors.value[0]?.id
  if (route.query.vehicleId) {
    form.vehicleId = Number(route.query.vehicleId)
    onVehicle(form.vehicleId)
  }
  if (route.query.appointmentId) form.appointmentId = Number(route.query.appointmentId)
})
</script>

<style scoped>
.up :deep(.el-upload--picture-card) { width: 76px; height: 57px; border-radius: 4px; }
.up-inner { font-size: 11px; color: var(--mut); line-height: 53px; }
.pics { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.pic { position: relative; }
.pic img { width: 76px; height: 57px; object-fit: cover; border-radius: 4px; border: 1px solid var(--line); }
.pic span {
  position: absolute; top: -6px; right: -6px; width: 16px; height: 16px; border-radius: 50%;
  background: var(--dan); color: #fff; font-size: 12px; line-height: 15px; text-align: center; cursor: pointer;
}
</style>
