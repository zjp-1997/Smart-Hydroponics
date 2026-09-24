<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getSystemAnnouncementById,
  publishSystemAnnouncement,
  updateSystemAnnouncement,
  type SystemAnnouncementPayload,
} from '@/api/sysMsg'

const props = defineProps<{ modelValue: boolean; id?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()

const formRef = ref<FormInstance>()
const detailLoading = ref(false)
const submitLoading = ref(false)
const form = reactive<SystemAnnouncementPayload>({ title: '', content: '', level: 1 })
const isEdit = computed(() => props.id != null)
const dialogTitle = computed(() => (isEdit.value ? '编辑公告' : '发布公告'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认发布'))
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})
const levelOptions = [
  { label: '普通', value: 1 },
  { label: '重要', value: 2 },
  { label: '紧急', value: 3 },
]
const rules: FormRules<SystemAnnouncementPayload> = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '公告标题不能超过100个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  level: [{ required: true, message: '请选择公告级别', trigger: 'change' }],
}

const resetForm = () => {
  Object.assign(form, { title: '', content: '', level: 1 })
  formRef.value?.clearValidate()
}

const loadDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const result = await getSystemAnnouncementById(id)
    Object.assign(form, {
      title: result.data.title || '',
      content: result.data.content || '',
      level: result.data.level || 1,
    })
  } finally {
    detailLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    resetForm()
    if (props.id != null) void loadDetail(props.id)
  },
)

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (props.id != null) await updateSystemAnnouncement(props.id, form)
    else await publishSystemAnnouncement(form)
    ElMessage.success(isEdit.value ? '编辑公告成功' : '发布公告成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" class="announcement-dialog" destroy-on-close>
    <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-width="86px">
      <el-form-item label="公告标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
      </el-form-item>
      <el-form-item label="公告级别" prop="level">
        <el-select v-model="form.level" placeholder="请选择公告级别">
          <el-option v-for="option in levelOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="公告内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="8" maxlength="2000" show-word-limit placeholder="请输入公告内容" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ submitText }}</el-button>
    </template>
  </el-dialog>
</template>
<style scoped>
.announcement-dialog :deep(.el-select) { width: 100%; }
</style>
