<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listCrops, type Crop } from '@/api/crop'
import {
  addGrowthStage,
  getGrowthStageById,
  updateGrowthStage,
  type GrowthStage,
  type GrowthStagePayload,
  type UpdateGrowthStagePayload,
} from '@/api/growthStage'

interface GrowthStageFormModel {
  cropId?: number
  stageName: string
  stageCode?: string
  stageOrder?: number
  startDay?: number
  endDay?: number
  duration?: number
  lightHours?: number
  tempMin?: number
  tempMax?: number
  humidityMin?: number
  humidityMax?: number
  phMin?: number
  phMax?: number
  ecMin?: number
  ecMax?: number
  waterIntervalDays?: number
  fertilizerIntervalDays?: number
  managementAdvice?: string
  status?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [growthStage?: GrowthStage]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑作物生长期' : '新增作物生长期'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const cropOptions = ref<Crop[]>([])

const createEmptyForm = (): GrowthStageFormModel => ({
  cropId: undefined,
  stageName: '',
  stageCode: '',
  stageOrder: undefined,
  startDay: undefined,
  endDay: undefined,
  duration: undefined,
  lightHours: undefined,
  tempMin: undefined,
  tempMax: undefined,
  humidityMin: undefined,
  humidityMax: undefined,
  phMin: undefined,
  phMax: undefined,
  ecMin: undefined,
  ecMax: undefined,
  waterIntervalDays: undefined,
  fertilizerIntervalDays: undefined,
  managementAdvice: '',
  status: 1,
  remark: '',
})

const form = ref<GrowthStageFormModel>(createEmptyForm())

const formRules = computed<FormRules<GrowthStageFormModel>>(() => ({
  cropId: [{ required: true, message: '请选择作物', trigger: 'change' }],
  stageName: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }],
  stageOrder: [{ required: true, message: '请输入阶段顺序', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

const loadCropOptions = async () => {
  const result = await listCrops({ pageNum: 1, pageSize: 1000 })

  cropOptions.value = result.data.list.filter((item) => item.status !== 0)
}

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (growthStage: GrowthStage) => {
  form.value = {
    cropId: growthStage.cropId,
    stageName: growthStage.stageName || '',
    stageCode: growthStage.stageCode || '',
    stageOrder: growthStage.stageOrder,
    startDay: growthStage.startDay,
    endDay: growthStage.endDay,
    duration: growthStage.duration,
    lightHours: growthStage.lightHours,
    tempMin: growthStage.tempMin,
    tempMax: growthStage.tempMax,
    humidityMin: growthStage.humidityMin,
    humidityMax: growthStage.humidityMax,
    phMin: growthStage.phMin,
    phMax: growthStage.phMax,
    ecMin: growthStage.ecMin,
    ecMax: growthStage.ecMax,
    waterIntervalDays: growthStage.waterIntervalDays,
    fertilizerIntervalDays: growthStage.fertilizerIntervalDays,
    managementAdvice: growthStage.managementAdvice || '',
    status: growthStage.status ?? 1,
    remark: growthStage.remark || '',
  }
}

const loadGrowthStageDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getGrowthStageById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const removeEmptyOptionalFields = <T extends Record<string, unknown>>(data: T) => {
  Object.keys(data).forEach((key) => {
    if (data[key] === '' || data[key] === undefined || data[key] === null) {
      delete data[key]
    }
  })

  return data
}

const buildCreatePayload = () => {
  return removeEmptyOptionalFields({ ...form.value }) as GrowthStagePayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...form.value }) as UpdateGrowthStagePayload
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result =
      props.id === undefined || props.id === null
        ? await addGrowthStage(buildCreatePayload())
        : await updateGrowthStage(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑作物生长期成功' : '新增作物生长期成功')
    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => [form.value.startDay, form.value.endDay],
  ([startDay, endDay]) => {
    if (
      typeof startDay === 'number' &&
      typeof endDay === 'number' &&
      endDay >= startDay
    ) {
      form.value.duration = endDay - startDay + 1
    }
  },
)

watch(
  () => dialogVisible.value,
  (visible) => {
    if (!visible) {
      return
    }

    resetForm()
    void loadCropOptions()

    if (props.id !== undefined && props.id !== null) {
      void loadGrowthStageDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="620px"
    class="growth-stage-dialog"
    modal-class="growth-stage-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="growth-stage-form"
        :model="form"
        :rules="formRules"
        label-width="118px"
        autocomplete="off"
      >
        <el-form-item label="所属作物" prop="cropId">
          <el-select v-model="form.cropId" placeholder="请选择作物" filterable clearable>
            <el-option
              v-for="item in cropOptions"
              :key="item.id"
              :label="item.cropName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段名称" prop="stageName">
          <el-input v-model.trim="form.stageName" placeholder="请输入阶段名称" clearable />
        </el-form-item>
        <!-- <el-form-item label="阶段编码" prop="stageCode">
          <el-input v-model.trim="form.stageCode" placeholder="请输入阶段编码" clearable />
        </el-form-item> -->
        <el-form-item label="阶段顺序" prop="stageOrder">
          <el-input-number v-model="form.stageOrder" :min="1" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="开始天数" prop="startDay">
          <el-input-number v-model="form.startDay" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="结束天数" prop="endDay">
          <el-input-number v-model="form.endDay" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="持续天数" prop="duration">
          <el-input-number v-model="form.duration" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="光照时长" prop="lightHours">
          <el-input-number v-model="form.lightHours" :min="0" :max="24" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="最低温度" prop="tempMin">
          <el-input-number v-model="form.tempMin" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最高温度" prop="tempMax">
          <el-input-number v-model="form.tempMax" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最低湿度" prop="humidityMin">
          <el-input-number v-model="form.humidityMin" :min="0" :max="100" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最高湿度" prop="humidityMax">
          <el-input-number v-model="form.humidityMax" :min="0" :max="100" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最低PH" prop="phMin">
          <el-input-number v-model="form.phMin" :min="0" :max="14" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最高PH" prop="phMax">
          <el-input-number v-model="form.phMax" :min="0" :max="14" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最低EC" prop="ecMin">
          <el-input-number v-model="form.ecMin" :min="0" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="最高EC" prop="ecMax">
          <el-input-number v-model="form.ecMax" :min="0" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="浇水间隔" prop="waterIntervalDays">
          <el-input-number v-model="form.waterIntervalDays" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="施肥间隔" prop="fertilizerIntervalDays">
          <el-input-number v-model="form.fertilizerIntervalDays" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="管理建议" prop="managementAdvice" class="form-wide">
          <el-input
            v-model="form.managementAdvice"
            type="textarea"
            :rows="3"
            maxlength="400"
            show-word-limit
            placeholder="请输入该生长期的管理建议"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="form-wide">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.growth-stage-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.growth-stage-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.growth-stage-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.growth-stage-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.growth-stage-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.growth-stage-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.form-wide {
  grid-column: auto;
  align-items: center;
}

.growth-stage-form {
  padding: 2px 2px 0;
}

.growth-stage-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.growth-stage-form :deep(.el-select),
.growth-stage-form :deep(.el-input-number) {
  width: 100%;
}

.growth-stage-form :deep(.el-input__wrapper),
.growth-stage-form :deep(.el-select__wrapper),
.growth-stage-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.growth-stage-form :deep(.el-input__wrapper),
.growth-stage-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

@media (max-width: 720px) {
  :global(.growth-stage-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
