<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getMaintenanceMessage, getMaintenanceDevices, publishMaintenanceMessage, updateMaintenanceMessage, type MaintenanceDeviceOption } from '@/api/maintenanceMsg'
const props = defineProps<{ modelValue: boolean; id?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()
const formRef = ref<FormInstance>(); const submitLoading = ref(false); const detailLoading = ref(false); const optionsLoading = ref(false)
const deviceOptions = ref<MaintenanceDeviceOption[]>([])
const form = reactive({ deviceId: undefined as number | undefined, title: '', content: '', level: 1, version: undefined as number | undefined })
const dialogVisible = computed({ get: () => props.modelValue, set: (value: boolean) => emit('update:modelValue', value) })
const isEdit = computed(() => props.id != null)
const selectedDevice = computed(() => deviceOptions.value.find(device => device.id === form.deviceId) ?? (isEdit.value ? deviceOptions.value[0] : undefined))
const contentPreview = computed(() => selectedDevice.value ? `${selectedDevice.value.plotName}地块的${selectedDevice.value.name}发生故障，请技术人员或农场主及时处理` : '')
watch(contentPreview, (next, previous) => { if (!isEdit.value && (!form.content || form.content === previous)) form.content = next })
const rules: FormRules = {
  content: [{ required: true, whitespace: true, message: '请输入消息内容', trigger: 'blur' }, { max: 10000, message: '消息内容不能超过10000个字符', trigger: 'blur' }],
  title: [{ required: true, whitespace: true, message: '请输入消息标题', trigger: 'blur' }, { max: 100, message: '消息标题不能超过100个字符', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请选择关联设备', trigger: 'change' }], level: [{ required: true, message: '请选择消息级别', trigger: 'change' }],
}
let optionRequest = 0
const loadDevices = async (keyword = '') => {
  const ownRequest = ++optionRequest; optionsLoading.value = true
  try { const result = await getMaintenanceDevices(keyword); if (ownRequest !== optionRequest) return; const chosen = selectedDevice.value; deviceOptions.value = result.data || []; if (chosen && !deviceOptions.value.some(item => item.id === chosen.id)) deviceOptions.value.unshift(chosen) }
  catch { ElMessage.error('设备选项加载失败') } finally { if (ownRequest === optionRequest) optionsLoading.value = false }
}
watch(() => props.modelValue, async (visible) => {
  if (!visible) { optionRequest++; return }
  Object.assign(form, { deviceId: undefined, title: '', content: '', level: 1, version: undefined }); deviceOptions.value = []; formRef.value?.clearValidate(); detailLoading.value = true
  try {
    if (props.id != null) { const message = (await getMaintenanceMessage(props.id)).data; Object.assign(form, { deviceId: message.deviceId, title: message.title, content: message.content, level: message.level, version: message.version }); deviceOptions.value = [{ id: message.deviceId ?? -1, name: message.deviceName || '设备', plotName: message.plotName }] }
    else await loadDevices()
  } catch { ElMessage.error('消息加载失败') } finally { detailLoading.value = false }
})
const saveMessage = async () => {
  if (submitLoading.value || !(await formRef.value?.validate().catch(() => false))) return
  submitLoading.value = true
  try { if (props.id != null) await updateMaintenanceMessage(props.id, { ...form, title: form.title.trim() }); else await publishMaintenanceMessage({ ...form, title: form.title.trim() }); ElMessage.success(isEdit.value ? '编辑维护消息成功' : '发布维护消息成功'); dialogVisible.value = false; emit('success') }
  finally { submitLoading.value = false }
}
</script>
<template>
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑维护消息' : '发布维护消息'" width="min(680px, 92vw)" class="message-dialog" destroy-on-close :close-on-click-modal="!submitLoading" :show-close="!submitLoading">
    <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-width="96px" :disabled="submitLoading">
      <el-form-item label="消息标题" prop="title"><el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入消息标题" /></el-form-item>
      <el-form-item label="设备" :prop="isEdit ? undefined : 'deviceId'"><el-select v-model="form.deviceId" filterable remote :remote-method="loadDevices" :loading="optionsLoading" :disabled="isEdit" placeholder="搜索设备或地块名称" style="width:100%"><el-option v-for="device in deviceOptions" :key="device.id" :value="device.id" :label="`${device.plotName} / ${device.name}`" /></el-select></el-form-item>
      <el-form-item label="消息级别" prop="level"><el-select v-model="form.level"><el-option label="普通" :value="1" /><el-option label="重要" :value="2" /><el-option label="紧急" :value="3" /></el-select></el-form-item>
      <el-form-item label="消息内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="4" maxlength="10000" show-word-limit placeholder="请输入消息内容；选择设备后可生成默认文案并修改" /></el-form-item>
      <el-form-item><span>默认正文可自由修改，保存后发送给对应农场主和绑定技术人员。</span></el-form-item>
    </el-form>
    <template #footer><el-button :disabled="submitLoading" @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="saveMessage">{{ isEdit ? '保存修改' : '确认发布' }}</el-button></template>
  </el-dialog>
</template>
<style scoped>
.message-dialog :deep(.el-select) { width: 100%; }
</style>
