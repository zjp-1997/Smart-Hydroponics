<script setup lang="ts">
// 专家评价弹窗只管理表单展示与保存事件，评价提交仍由列表页处理。
interface ReviewFormModel {
  realName: string
  userName: string
  rating: number
  content: string
}

defineProps<{ form: ReviewFormModel }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ save: [] }>()
</script>

<template>
  <el-dialog v-model="visible" title="编辑专家评价" width="520px">
    <el-form class="review-dialog-form" label-width="92px">
      <el-form-item label="专家姓名"><el-input v-model="form.realName" disabled /></el-form-item>
      <el-form-item label="评价用户"><el-input v-model="form.userName" disabled /></el-form-item>
      <el-form-item label="评分">
        <el-rate v-model="form.rating" allow-half />
        <span class="review-rate-number">{{ form.rating.toFixed(1) }}</span>
      </el-form-item>
      <el-form-item label="评价">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入评价内容"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="emit('save')">保存评价</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.review-dialog-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.review-rate-number {
  margin-left: 10px;
  color: #f59e0b;
  font-weight: 800;
}
</style>
