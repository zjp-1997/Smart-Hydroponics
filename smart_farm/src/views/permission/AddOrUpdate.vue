<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { addPermission, getPermissionById, updatePermission, type PermissionPayload, type UpdatePermissionPayload } from '@/api/permission'
import type { Permission } from '@/api/role'

interface PermissionFormModel { permissionName: string; permissionCode: string; type: number; path?: string; component?: string; status: number; sort: number }
const props = defineProps<{ modelValue: boolean; id?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()
const formRef = ref<FormInstance>()
const detailLoading = ref(false)
const submitLoading = ref(false)
const emptyForm = (): PermissionFormModel => ({ permissionName: '', permissionCode: '', type: 1, path: '', component: '', status: 1, sort: 100 })
const form = ref(emptyForm())
const isEdit = computed(() => props.id != null)
const dialogVisible = computed({ get: () => props.modelValue, set: (value: boolean) => emit('update:modelValue', value) })
const dialogTitle = computed(() => (isEdit.value ? '编辑菜单' : '新增菜单'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))
const formRules: FormRules<PermissionFormModel> = {
  permissionName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }, { min: 2, max: 80, message: '菜单名称长度为 2-80 个字符', trigger: 'blur' }],
  permissionCode: [{ required: true, message: '请输入菜单编码', trigger: 'blur' }, { min: 2, max: 100, message: '菜单编码长度为 2-100 个字符', trigger: 'blur' }, { pattern: /^[A-Za-z][A-Za-z0-9_:-]*$/, message: '菜单编码需以字母开头，仅支持字母、数字、下划线、冒号或短横线', trigger: 'blur' }],
}
const fillForm = (p: Permission) => { form.value = { permissionName: p.permissionName || '', permissionCode: p.permissionCode || '', type: 1, path: p.path || '', component: p.component || '', status: p.status ?? 1, sort: p.sort ?? 100 } }
watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  form.value = emptyForm()
  formRef.value?.clearValidate()
  if (props.id == null) return
  detailLoading.value = true
  try { fillForm((await getPermissionById(props.id)).data) } finally { detailLoading.value = false }
})
const buildPayload = () => {
  const payload = { permissionName: form.value.permissionName.trim(), permissionCode: form.value.permissionCode.trim(), type: 1, path: form.value.path?.trim(), component: form.value.component?.trim(), status: form.value.status, sort: form.value.sort } as Record<string, unknown>
  Object.keys(payload).forEach((key) => { if (payload[key] === '') delete payload[key] })
  return payload as unknown as PermissionPayload
}
const submitForm = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return
  submitLoading.value = true
  try {
    if (props.id != null) await updatePermission({ id: props.id, ...buildPayload() } as UpdatePermissionPayload)
    else await addPermission(buildPayload())
    ElMessage.success(isEdit.value ? '编辑菜单成功' : '新增菜单成功')
    dialogVisible.value = false
    emit('success')
  } finally { submitLoading.value = false }
}
</script>
<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" class="permission-dialog" modal-class="permission-dialog-overlay" align-center destroy-on-close>
    <div v-loading="detailLoading">
      <el-form ref="formRef" class="permission-form" :model="form" :rules="formRules" label-width="96px" autocomplete="off">
        <el-form-item label="菜单名称" prop="permissionName"><el-input v-model.trim="form.permissionName" placeholder="请输入菜单名称" clearable /></el-form-item>
        <el-form-item label="菜单编码" prop="permissionCode"><el-input v-model.trim="form.permissionCode" placeholder="例如 user:manage、warehouse:manage" clearable /></el-form-item>
        <el-form-item label="路由路径" prop="path"><el-input v-model.trim="form.path" placeholder="例如 /user/list" clearable /></el-form-item>
        <el-form-item label="组件路径" prop="component"><el-input v-model.trim="form.component" placeholder="例如 user/ListView" clearable /></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="form.status"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></el-form-item>
        <el-form-item label="排序" prop="sort"><el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" /></el-form-item>
      </el-form>
    </div>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="submitForm">{{ submitText }}</el-button></template>
  </el-dialog>
</template>
<style scoped>
:global(.permission-dialog) { max-height: calc(100vh - 48px); display: flex; flex-direction: column; border-radius: 10px; overflow: hidden; margin: 0 auto; }
:global(.permission-dialog-overlay .el-overlay-dialog) { overflow: hidden; }
:global(.permission-dialog .el-dialog__header) { flex: 0 0 auto; margin: 0; padding: 20px 24px 16px; border-bottom: 1px solid #eef2f7; }
:global(.permission-dialog .el-dialog__title) { color: #0f172a; font-size: 18px; font-weight: 800; }
:global(.permission-dialog .el-dialog__body) { flex: 1 1 auto; min-height: 0; padding: 22px 24px 6px; background: #fbfdff; overflow-y: auto; overflow-x: hidden; }
:global(.permission-dialog .el-dialog__footer) { flex: 0 0 auto; padding: 14px 24px 20px; border-top: 1px solid #eef2f7; background: #ffffff; }
.permission-form { display: grid; grid-template-columns: 1fr; gap: 2px; padding: 2px 2px 0; }
.permission-form :deep(.el-form-item) { margin-bottom: 17px; }
.permission-form :deep(.el-input__wrapper), .permission-form :deep(.el-select__wrapper) { min-height: 38px; border-radius: 6px; box-shadow: 0 0 0 1px #e2e8f0 inset; }
.permission-form :deep(.el-input__wrapper.is-focus), .permission-form :deep(.el-select__wrapper.is-focused) { box-shadow: 0 0 0 1px #409eff inset; }
.permission-form :deep(.el-input-number) { width: 100%; }
</style>
