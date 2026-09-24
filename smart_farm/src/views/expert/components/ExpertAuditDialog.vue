<script setup lang="ts">
import StatusTag from '@/components/admin/StatusTag.vue'

// 专家审核弹窗集中展示申请资料，并向列表页提交审核动作。
interface AuditFormModel {
  auditStatus: number
  realName: string
  organization: string
  jobTitle: string
  specialty: string
  introduction: string
  avatarUrl: string
  certificateName: string
  certificateUrl: string
  remark: string
}

defineProps<{
  title: string
  form: AuditFormModel
  pending: boolean
  statusLabel: string
  statusType: 'info' | 'warning' | 'success' | 'danger'
}>()

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ decision: [status: 2 | 3] }>()
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="520px">
    <el-form class="audit-dialog-form" label-width="92px">
      <el-form-item label="审核状态"><StatusTag :type="statusType" :label="statusLabel" /></el-form-item>
      <el-form-item label="专家头像">
        <el-image
          v-if="form.avatarUrl"
          class="audit-avatar"
          :src="form.avatarUrl"
          :alt="`${form.realName || '专家'}的头像`"
          fit="cover"
          :preview-src-list="[form.avatarUrl]"
          preview-teleported
        />
        <span v-else class="audit-empty">暂无头像</span>
      </el-form-item>
      <el-form-item label="专家姓名"><el-input v-model="form.realName" disabled /></el-form-item>
      <el-form-item label="所属机构"><el-input v-model="form.organization" disabled /></el-form-item>
      <el-form-item label="职称"><el-input v-model="form.jobTitle" disabled /></el-form-item>
      <el-form-item label="擅长方向"><el-input v-model="form.specialty" disabled /></el-form-item>
      <el-form-item label="证书名称"><el-input v-model="form.certificateName" disabled /></el-form-item>
      <el-form-item label="认证材料">
        <el-image
          v-if="form.certificateUrl"
          class="audit-certificate"
          :src="form.certificateUrl"
          :alt="`${form.realName || '专家'}的资质证书`"
          fit="contain"
          :preview-src-list="[form.certificateUrl]"
          preview-teleported
        />
        <span v-else class="audit-empty">暂无认证材料</span>
      </el-form-item>
      <el-form-item label="专家简介">
        <el-input v-model="form.introduction" type="textarea" :rows="3" disabled />
      </el-form-item>
      <el-form-item label="审核意见">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          :disabled="!pending"
          :placeholder="pending ? '请输入审核意见' : '暂无审核意见'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <template v-if="pending">
        <el-button type="danger" plain @click="emit('decision', 3)">不通过</el-button>
        <el-button type="primary" @click="emit('decision', 2)">通过</el-button>
      </template>
      <el-button v-else type="primary" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.audit-dialog-form :deep(.el-input.is-disabled .el-input__wrapper),
.audit-dialog-form :deep(.el-textarea.is-disabled .el-textarea__inner) {
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.audit-certificate {
  width: 150px;
  height: 92px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.audit-avatar {
  width: 72px;
  height: 72px;
  border: 1px solid #e2e8f0;
  border-radius: 50%;
  background: #ffffff;
}

.audit-empty {
  height: 34px;
  display: inline-flex;
  align-items: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>
