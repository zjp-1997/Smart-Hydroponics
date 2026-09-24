<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { addFarmTask, getFarmTaskById, updateFarmTask, type FarmTaskPayload } from '@/api/farmTask'
import { listPlots, type Plot } from '@/api/plot'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'

/** 表单只维护任务计划信息；执行状态必须通过详情页的业务动作流转。 */
type TaskForm = { plotId: number | ''; taskTitle: string; taskType: number; taskContent: string; priority: number; deadlineTime: string; executorId: number | ''; remark: string }
const props = defineProps<{ modelValue: boolean; id?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()
const formRef = ref<FormInstance>()
const detailLoading = ref(false)
const submitLoading = ref(false)
const plotOptions = ref<Plot[]>([])
const userOptions = ref<SmartPlantUser[]>([])
const emptyForm = (): TaskForm => ({ plotId: '', taskTitle: '', taskType: 5, taskContent: '', priority: 2, deadlineTime: '', executorId: '', remark: '' })
const form = ref(emptyForm())
const dialogVisible = computed({ get: () => props.modelValue, set: (value: boolean) => emit('update:modelValue', value) })
const isEdit = computed(() => props.id != null)
const rules: FormRules<TaskForm> = {
  plotId: [{ required: true, message: '请选择所属地块', trigger: 'change' }], taskTitle: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }], priority: [{ required: true, message: '请选择优先级', trigger: 'change' }], deadlineTime: [{ required: true, message: '请选择截至时间', trigger: 'change' }],
}
const taskTypeMap: Record<number, string> = { 1: '浇水', 2: '施肥', 3: '打药', 4: '采收', 5: '巡检', 6: '除草', 7: '补光', 8: '其他' }
const priorityMap: Record<number, string> = { 1: '低', 2: '普通', 3: '高', 4: '紧急' }
const loadOptions = async () => {
  const [plots, users] = await Promise.all([listPlots({ pageNum: 1, pageSize: 200, status: 1 }), listSmartPlantUsers({ pageNum: 1, pageSize: 200, status: 1 })])
  plotOptions.value = plots.data.list || []; userOptions.value = users.data.list || []
}
watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  form.value = emptyForm(); formRef.value?.clearValidate(); detailLoading.value = true
  try {
    await loadOptions()
    if (props.id != null) {
      const task = (await getFarmTaskById(props.id)).data
      form.value = { plotId: task.plotId, taskTitle: task.taskTitle, taskType: task.taskType, taskContent: task.taskContent || '', priority: task.priority || 2, deadlineTime: task.deadlineTime || '', executorId: task.executorId || '', remark: task.remark || '' }
    }
  } finally { detailLoading.value = false }
})
// 请求体不携带状态，避免普通编辑绕过“开始/完成”状态机。
const buildPayload = (): FarmTaskPayload => ({ plotId: Number(form.value.plotId), taskTitle: form.value.taskTitle.trim(), taskType: form.value.taskType, taskContent: form.value.taskContent.trim() || undefined, priority: form.value.priority, deadlineTime: form.value.deadlineTime || undefined, executorId: form.value.executorId === '' ? undefined : Number(form.value.executorId), remark: form.value.remark.trim() || undefined })
const submitForm = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return
  submitLoading.value = true
  try {
    if (props.id != null) await updateFarmTask({ id: props.id, ...buildPayload() }); else await addFarmTask(buildPayload())
    ElMessage.success(isEdit.value ? '编辑农事任务成功' : '新增农事任务成功'); dialogVisible.value = false; emit('success')
  } finally { submitLoading.value = false }
}
</script>
<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑农事任务' : '新增农事任务'" width="720px" destroy-on-close>
    <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-width="96px" class="task-form">
      <el-form-item label="所属地块" prop="plotId"><el-select v-model="form.plotId" filterable clearable><el-option v-for="plot in plotOptions" :key="plot.id" :label="`${plot.plotName}${plot.plotCode ? `（${plot.plotCode}）` : ''}`" :value="plot.id" /></el-select></el-form-item>
      <el-form-item label="任务标题" prop="taskTitle"><el-input v-model.trim="form.taskTitle" clearable /></el-form-item>
      <el-form-item label="任务类型" prop="taskType"><el-select v-model="form.taskType"><el-option v-for="(label, value) in taskTypeMap" :key="value" :label="label" :value="Number(value)" /></el-select></el-form-item>
      <el-form-item label="优先级" prop="priority"><el-select v-model="form.priority"><el-option v-for="(label, value) in priorityMap" :key="value" :label="label" :value="Number(value)" /></el-select></el-form-item>
      <el-form-item label="执行人"><el-select v-model="form.executorId" filterable clearable><el-option v-for="user in userOptions" :key="user.id" :label="user.nickname || user.username" :value="user.id" /></el-select></el-form-item>
      <el-form-item label="截至时间" prop="deadlineTime"><el-date-picker v-model="form.deadlineTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable /></el-form-item>
      <el-form-item label="任务内容" class="form-full"><el-input v-model="form.taskContent" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="备注" class="form-full"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button></template>
  </el-dialog>
</template>
<style scoped>
.task-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px 18px; }
.task-form :deep(.el-form-item) { margin-bottom: 0; }
.task-form :deep(.el-select), .task-form :deep(.el-date-editor.el-input), .task-form :deep(.el-input), .task-form :deep(.el-textarea) { width: 100%; }
.form-full { grid-column: 1 / -1; }
@media (max-width: 768px) { .task-form { grid-template-columns: 1fr; } }
</style>
