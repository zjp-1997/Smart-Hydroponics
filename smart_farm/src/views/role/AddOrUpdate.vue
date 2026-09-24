<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addRole,
  getRoleById,
  updateRole,
  type Role,
  type RolePayload,
  type UpdateRolePayload,
} from '@/api/role'

interface RoleFormModel {
  roleName: string
  roleCode: string
  status: number
  remark: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [role?: Role]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑角色' : '新增角色'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)

const createEmptyForm = (): RoleFormModel => ({
  roleName: '',
  roleCode: '',
  status: 1,
  remark: '',
})

const form = ref<RoleFormModel>(createEmptyForm())

const formRules = computed<FormRules<RoleFormModel>>(() => ({
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '角色名称长度为 2-50 个字符', trigger: 'blur' },
  ],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { min: 2, max: 50, message: '角色编码长度为 2-50 个字符', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_:-]*$/,
      message: '角色编码需以字母开头，仅支持字母、数字、下划线、冒号或短横线',
      trigger: 'blur',
    },
  ],
}))

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (role: Role) => {
  form.value = {
    roleName: role.roleName || '',
    roleCode: role.roleCode || '',
    status: role.status ?? 1,
    remark: role.remark || role.description || '',
  }
}

const loadRoleDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getRoleById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const buildCreatePayload = () => {
  const remark = form.value.remark.trim()
  return {
    roleName: form.value.roleName.trim(),
    roleCode: form.value.roleCode.trim(),
    description: remark,
    remark,
    status: form.value.status,
    sort: 100,
  } as RolePayload
}

const buildUpdatePayload = (id: number) => {
  return { id, ...buildCreatePayload() } as UpdateRolePayload
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result = isEdit.value
      ? await updateRole(buildUpdatePayload(props.id!))
      : await addRole(buildCreatePayload())

    ElMessage.success(isEdit.value ? '编辑角色成功' : '新增角色成功')
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

    if (isEdit.value) {
      void loadRoleDetail(props.id!)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="role-dialog"
    modal-class="role-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="role-form"
        :model="form"
        :rules="formRules"
        label-width="96px"
        autocomplete="off"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model.trim="form.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model.trim="form.roleCode" placeholder="例如 admin、expert" clearable />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入角色说明"
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
:global(.role-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.role-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.role-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.role-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.role-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.role-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.role-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2px;
  padding: 2px 2px 0;
}

.role-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.role-form :deep(.el-input__wrapper),
.role-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.role-form :deep(.el-input__wrapper) {
  min-height: 38px;
}

.role-form :deep(.el-input__wrapper.is-focus),
.role-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

@media (max-width: 720px) {
  :global(.role-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
