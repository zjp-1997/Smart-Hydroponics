<script setup lang="ts">
import type { UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

// 只负责图片选择、加载态和预览展示，文件校验与上传事务仍由业务页面处理。
withDefaults(defineProps<{
  previewUrl?: string
  loading?: boolean
  alt?: string
  accept?: string
}>(), {
  previewUrl: '',
  loading: false,
  alt: '图片预览',
  accept: 'image/jpeg,image/png,image/webp',
})

const emit = defineEmits<{ select: [file: File] }>()

/** 组件只负责选择和预览；格式校验、上传接口与失败回滚仍由业务页面决定。 */
const handleChange: UploadProps['onChange'] = (uploadFile) => {
  if (uploadFile.raw) emit('select', uploadFile.raw)
}
</script>

<template>
  <el-upload
    v-loading="loading"
    class="image-upload-field"
    :accept="accept"
    :auto-upload="false"
    :show-file-list="false"
    :on-change="handleChange"
  >
    <img v-if="previewUrl" class="image-upload-preview" :src="previewUrl" :alt="alt" />
    <el-icon v-else class="image-upload-icon"><Plus /></el-icon>
  </el-upload>
</template>

<style scoped>
.image-upload-field :deep(.el-upload) {
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.image-upload-field :deep(.el-upload:hover) {
  border-color: #409eff;
}

.image-upload-icon {
  color: #94a3b8;
  font-size: 24px;
}

.image-upload-preview {
  width: 76px;
  height: 76px;
  display: block;
  object-fit: contain;
}
</style>
