<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { harvestPlot, type PlotHarvestPayload } from '@/api/plot'
import { getCurrentUser } from '@/utils/auth'

const props = defineProps<{
  modelValue: boolean
  plotId?: number | null
  plotName?: string
  yieldUnit?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = ref<PlotHarvestPayload>({
  harvester: '',
  yieldAmount: 0,
  yieldUnit: 'kg',
  actualHarvestAt: '',
})

const rules: FormRules<PlotHarvestPayload> = {
  harvester: [
    { required: true, message: '请输入采收人', trigger: 'blur' },
    { max: 50, message: '采收人长度不能超过50个字符', trigger: 'blur' },
  ],
  yieldAmount: [
    { required: true, message: '请输入实际产量', trigger: 'change' },
    { type: 'number', min: 0, message: '实际产量不能小于0', trigger: 'change' },
  ],
  yieldUnit: [{ required: true, message: '请选择产量单位', trigger: 'change' }],
  actualHarvestAt: [{ required: true, message: '请选择实际采收日期', trigger: 'change' }],
}

const formatToday = () => {
  const today = new Date()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${today.getFullYear()}-${month}-${day}`
}

const resetForm = () => {
  const user = getCurrentUser()
  form.value = {
    harvester: user?.nickname || user?.username || '',
    yieldAmount: 0,
    yieldUnit: props.yieldUnit || 'kg',
    actualHarvestAt: formatToday(),
  }
  formRef.value?.clearValidate()
}

const submit = async () => {
  if (!props.plotId || !formRef.value || !(await formRef.value.validate().catch(() => false))) return
  submitting.value = true
  try {
    await harvestPlot(props.plotId, form.value)
    ElMessage.success('采收成功，地块已恢复为空闲状态')
    dialogVisible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

watch(() => props.modelValue, (visible) => {
  if (visible) resetForm()
})
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="`采收地块${plotName ? `：${plotName}` : ''}`" width="520px"
    destroy-on-close append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="采收人" prop="harvester">
        <el-input v-model.trim="form.harvester" maxlength="50" placeholder="请输入采收人" />
      </el-form-item>
      <el-form-item label="实际产量" prop="yieldAmount">
        <el-input-number v-model="form.yieldAmount" :min="0" :precision="2" :step="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="产量单位" prop="yieldUnit">
        <el-select v-model="form.yieldUnit">
          <el-option label="kg" value="kg" />
          <el-option label="斤" value="斤" />
          <el-option label="吨" value="吨" />
        </el-select>
      </el-form-item>
      <el-form-item label="实际采收日期" prop="actualHarvestAt">
        <el-date-picker v-model="form.actualHarvestAt" type="date" value-format="YYYY-MM-DD"
          :disabled-date="(date: Date) => date.getTime() > Date.now()" placeholder="请选择实际采收日期" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认采收</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:deep(.el-input-number),
:deep(.el-select),
:deep(.el-date-editor) {
  width: 100%;
}
</style>
