<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getFarmMessageById, publishFarmMessage, updateFarmMessage, type FarmMessagePayload } from '@/api/farmMsg'
import { listFarmTasks, type FarmTask } from '@/api/farmTask'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
type FarmMessageForm = { taskId: number | ''; title: string; content: string; level: number; recipientIds: number[] }
const props = defineProps<{ modelValue: boolean; id?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()
const formRef = ref<FormInstance>(); const detailLoading = ref(false); const submitLoading = ref(false)
const taskOptions = ref<FarmTask[]>([]); const userOptions = ref<SmartPlantUser[]>([])
const form = reactive<FarmMessageForm>({ taskId: '', title: '', content: '', level: 1, recipientIds: [] })
const dialogVisible = computed({ get: () => props.modelValue, set: (value: boolean) => emit('update:modelValue', value) })
const isEdit = computed(() => props.id != null)
const levelOptions = [{ label: '普通', value: 1 }, { label: '重要', value: 2 }, { label: '紧急', value: 3 }]
const rules: FormRules<FarmMessageForm> = { taskId: [{ required: true, message: '请选择关联农事任务', trigger: 'change' }], title: [{ required: true, message: '请输入农事消息标题', trigger: 'blur' }, { max: 100, message: '农事消息标题不能超过100个字符', trigger: 'blur' }], content: [{ required: true, message: '请输入农事消息内容', trigger: 'blur' }], level: [{ required: true, message: '请选择消息级别', trigger: 'change' }] }
const getTaskOptionLabel = (task: FarmTask) => `${task.taskTitle}${task.plotName ? ` / ${task.plotName}` : ''}`
watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  Object.assign(form, { taskId: '', title: '', content: '', level: 1, recipientIds: [] }); formRef.value?.clearValidate(); detailLoading.value = true
  try {
    const [tasks, users] = await Promise.all([listFarmTasks({ pageNum: 1, pageSize: 500 }), listSmartPlantUsers({ pageNum: 1, pageSize: 500, status: 1 })])
    taskOptions.value = tasks.data.list || []; userOptions.value = users.data.list || []
    if (props.id != null) { const message = (await getFarmMessageById(props.id)).data; Object.assign(form, { taskId: message.taskId, title: message.title || '', content: message.content || '', level: message.level || 1 }) }
  } finally { detailLoading.value = false }
})
const buildPayload = (): FarmMessagePayload => ({ taskId: Number(form.taskId), title: form.title.trim(), content: form.content.trim(), level: form.level, recipientIds: form.recipientIds.length ? form.recipientIds : undefined })
const submitForm = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return
  submitLoading.value = true
  try { if (props.id != null) await updateFarmMessage(props.id, buildPayload()); else await publishFarmMessage(buildPayload()); ElMessage.success(isEdit.value ? '编辑农事消息成功' : '发布农事消息成功'); dialogVisible.value = false; emit('success') }
  finally { submitLoading.value = false }
}
</script>
<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑农事消息' : '发布农事消息'" width="680px" class="message-dialog" destroy-on-close>
    <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="关联任务" prop="taskId"><el-select v-model="form.taskId" filterable clearable><el-option v-for="task in taskOptions" :key="task.id" :label="getTaskOptionLabel(task)" :value="task.id" /></el-select></el-form-item>
      <el-form-item label="消息标题" prop="title"><el-input v-model="form.title" maxlength="100" show-word-limit /></el-form-item>
      <el-form-item label="消息级别" prop="level"><el-select v-model="form.level"><el-option v-for="option in levelOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item>
      <el-form-item v-if="!isEdit" label="接收人"><el-select v-model="form.recipientIds" placeholder="默认发送给任务负责人和归属用户" multiple filterable clearable><el-option v-for="user in userOptions" :key="user.id" :label="user.nickname || user.username" :value="user.id" /></el-select></el-form-item>
      <el-form-item label="消息内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="8" maxlength="2000" show-word-limit /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="submitForm">{{ isEdit ? '保存修改' : '确认发布' }}</el-button></template>
  </el-dialog>
</template>
<style scoped>
.message-dialog :deep(.el-select) { width: 100%; }
</style>
