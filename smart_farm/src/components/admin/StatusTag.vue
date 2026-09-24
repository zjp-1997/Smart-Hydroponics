<script setup lang="ts">
import type { TagProps } from 'element-plus'

withDefaults(defineProps<{
  label?: string
  type?: TagProps['type']
}>(), {
  label: '-',
  type: 'info',
})
</script>

<template>
  <!-- 状态同时使用文字与语义色，避免只依赖颜色传达含义。 -->
  <el-tag :type="type" effect="light" round>{{ label }}</el-tag>
</template>
