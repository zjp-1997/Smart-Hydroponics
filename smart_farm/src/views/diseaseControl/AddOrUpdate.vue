<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import {
  addDiseaseControl,
  getDiseaseControlById,
  updateDiseaseControl,
  type DiseaseControl,
  type DiseaseControlPayload,
  type UpdateDiseaseControlPayload,
} from '@/api/diseaseControl'
import { listDiseasePests, type DiseasePest } from '@/api/diseasePest'

interface DiseaseControlFormModel {
  diseaseId?: number
  controlType?: number
  controlCategory?: number
  method?: string
  drugName?: string
  usageMethod?: string
  dosageSpec?: string
  safetyIntervalDays?: number
  precautions?: string
  suitableStage?: string
  status?: number
  sortOrder?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [control?: DiseaseControl]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑防治措施' : '新增防治措施'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const diseaseOptions = ref<DiseasePest[]>([])

const controlTypeOptions = [
  { label: '预防', value: 1 },
  { label: '治疗', value: 2 },
]

/** 防治手段与预防/治疗类型相互独立，用于 farm 详情页分组展示。 */
const controlCategoryOptions = [
  { label: '农业防治', value: 1 },
  { label: '物理防治', value: 2 },
  { label: '化学防治', value: 3 },
  { label: '生物防治', value: 4 },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const createEmptyForm = (): DiseaseControlFormModel => ({
  diseaseId: undefined,
  controlType: 1,
  controlCategory: undefined,
  method: '',
  drugName: '',
  usageMethod: '',
  dosageSpec: '',
  safetyIntervalDays: undefined,
  precautions: '',
  suitableStage: '',
  status: 1,
  sortOrder: 0,
  remark: '',
})

const form = ref<DiseaseControlFormModel>(createEmptyForm())

const formRules = computed<FormRules<DiseaseControlFormModel>>(() => ({
  diseaseId: [{ required: true, message: '请选择关联病虫害', trigger: 'change' }],
  controlType: [{ required: true, message: '请选择防治类型', trigger: 'change' }],
  controlCategory: [{ required: true, message: '请选择防治手段', trigger: 'change' }],
  method: [
    { required: true, message: '请输入措施内容', trigger: 'blur' },
    { max: 500, message: '措施内容不能超过 500 个字符', trigger: 'blur' },
  ],
  drugName: [{ max: 100, message: '药剂名称不能超过 100 个字符', trigger: 'blur' }],
  usageMethod: [{ max: 255, message: '使用方法不能超过 255 个字符', trigger: 'blur' }],
  suitableStage: [{ max: 100, message: '适用阶段不能超过 100 个字符', trigger: 'blur' }],
  dosageSpec: [{ max: 255, message: '用量规格不能超过 255 个字符', trigger: 'blur' }],
  precautions: [{ max: 500, message: '注意事项不能超过 500 个字符', trigger: 'blur' }],
}))

const loadDiseases = async () => {
  const result = await listDiseasePests({ pageNum: 1, pageSize: 1000, status: 1 })
  diseaseOptions.value = result.data.list
}

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (control: DiseaseControl) => {
  form.value = {
    diseaseId: control.diseaseId,
    controlType: control.controlType ?? 1,
    controlCategory: control.controlCategory,
    method: control.method || '',
    drugName: control.drugName || '',
    usageMethod: control.usageMethod || '',
    dosageSpec: control.dosageSpec || '',
    safetyIntervalDays: control.safetyIntervalDays,
    precautions: control.precautions || '',
    suitableStage: control.suitableStage || '',
    status: control.status ?? 1,
    sortOrder: control.sortOrder ?? 0,
    remark: control.remark || '',
  }
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getDiseaseControlById(id)
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

/** 组装新增防治措施 payload，只保留后端 disease_control 表需要的字段。 */
const buildCreatePayload = () => {
  return removeEmptyOptionalFields({ ...form.value }) as unknown as DiseaseControlPayload
}

/** 组装编辑防治措施 payload，编辑时必须提交 id 供后端定位记录。 */
const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...form.value }) as unknown as UpdateDiseaseControlPayload
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
        ? await addDiseaseControl(buildCreatePayload())
        : await updateDiseaseControl(buildUpdatePayload(props.id))

    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => dialogVisible.value,
  (visible) => {
    if (!visible) {
      return
    }

    resetForm()
    void loadDiseases()

    if (props.id !== undefined && props.id !== null) {
      void loadDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="700px"
    class="control-dialog"
    modal-class="control-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="control-form"
        :model="form"
        :rules="formRules"
        label-width="112px"
        autocomplete="off"
      >
        <el-form-item label="病虫害名称" prop="diseaseId">
          <el-select v-model="form.diseaseId" placeholder="病虫害名称" filterable>
            <el-option
              v-for="item in diseaseOptions"
              :key="item.id"
              :label="item.cropTypeName ? `${item.name}（${item.cropTypeName}）` : item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="防治类型" prop="controlType">
          <el-select v-model="form.controlType" placeholder="请选择防治类型">
            <el-option v-for="item in controlTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="防治手段" prop="controlCategory">
          <el-select v-model="form.controlCategory" placeholder="请选择防治手段">
            <el-option
              v-for="item in controlCategoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="药剂名称" prop="drugName">
          <el-input v-model.trim="form.drugName" placeholder="请输入药剂名称，可为空" clearable />
        </el-form-item>
        <el-form-item label="适用阶段" prop="suitableStage">
          <el-input v-model.trim="form.suitableStage" placeholder="如：苗期、花期、结果期" clearable />
        </el-form-item>
        <el-form-item label="用量规格" prop="dosageSpec">
          <el-input v-model.trim="form.dosageSpec" placeholder="如：5%乳油，1500—2000倍液" clearable />
        </el-form-item>
        <el-form-item label="安全间隔期" prop="safetyIntervalDays">
          <el-input-number v-model="form.safetyIntervalDays" :min="0" :max="365" controls-position="right" />
          <span class="input-suffix">天</span>
        </el-form-item>
        <el-form-item label="展示顺序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button v-for="item in statusOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="措施内容" prop="method" class="form-wide">
          <el-input
            v-model="form.method"
            type="textarea"
            :rows="4"
            placeholder="请输入具体预防或治疗措施"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="使用方法" prop="usageMethod" class="form-wide">
          <el-input
            v-model="form.usageMethod"
            type="textarea"
            :rows="3"
            placeholder="请输入药剂或措施的使用方法"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="注意事项" prop="precautions" class="form-wide">
          <el-input
            v-model="form.precautions"
            type="textarea"
            :rows="3"
            placeholder="请输入安全间隔、禁限用或轮换用药提示"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="form-wide">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="255"
            show-word-limit
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
:global(.control-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.control-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.control-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.control-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.control-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  max-height: none;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.control-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:global(.control-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: #cbd5e1;
}

:global(.control-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.control-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
  padding: 2px 2px 0;
}

.control-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.control-form :deep(.el-select) {
  width: 100%;
}

.control-form :deep(.el-input-number) {
  width: calc(100% - 30px);
}

.input-suffix {
  margin-left: 8px;
  color: #64748b;
}

.control-form :deep(.el-input__wrapper),
.control-form :deep(.el-select__wrapper),
.control-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.control-form :deep(.el-input__wrapper),
.control-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.control-form :deep(.el-input__wrapper.is-focus),
.control-form :deep(.el-select__wrapper.is-focused),
.control-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.form-wide {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  :global(.control-dialog) {
    width: calc(100vw - 28px) !important;
  }

  .control-form {
    grid-template-columns: 1fr;
  }
}
</style>
