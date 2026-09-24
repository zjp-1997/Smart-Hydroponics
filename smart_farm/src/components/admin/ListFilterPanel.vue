<script setup lang="ts">
// 统一列表页的筛选卡片、操作区和管理按钮区，具体筛选字段由页面通过插槽提供。
defineOptions({ inheritAttrs: false })

withDefaults(defineProps<{
  model: Record<string, unknown>
  labelWidth?: string | number
  showManage?: boolean
}>(), {
  labelWidth: '76px',
  showManage: true,
})
</script>

<template>
  <section class="filter-card">
    <el-form class="filter-form" :model="model" :label-width="labelWidth" v-bind="$attrs">
      <slot />
      <div class="filter-actions"><slot name="actions" /></div>
      <div v-if="showManage && $slots.manage" class="manage-actions"><slot name="manage" /></div>
    </el-form>
  </section>
</template>
