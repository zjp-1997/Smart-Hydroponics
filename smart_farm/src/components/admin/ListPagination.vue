<script setup lang="ts">
// 统一列表页分页器的双向绑定、页容量选项和响应式布局。
withDefaults(defineProps<{
  total: number
  pageSizes?: number[]
}>(), {
  pageSizes: () => [10, 20, 50, 100],
})

const currentPage = defineModel<number>('currentPage', { required: true })
const pageSize = defineModel<number>('pageSize', { required: true })
const emit = defineEmits<{
  currentChange: [page: number]
  sizeChange: [size: number]
}>()
</script>

<template>
  <div class="pagination-row">
    <span>共 {{ total.toLocaleString() }} 条</span>
    <div class="pagination-box">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        layout="sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="pageSizes"
        @current-change="emit('currentChange', $event)"
        @size-change="emit('sizeChange', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.pagination-row {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  color: #334155;
  font-size: 14px;
}

.pagination-box {
  min-width: 0;
  display: flex;
  flex: 1;
  justify-content: flex-end;
}

.pagination-box :deep(.el-pagination) {
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
