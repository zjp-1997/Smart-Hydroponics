<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { listOperationLogs, type OperationLog } from '@/api/operationLog'

const sidebarVisible = ref(false)
const tableLoading = ref(false)
const logs = ref<OperationLog[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  operatorName: '',
  operation: '',
  timeRange: [] as string[],
})

const closeSidebar = () => {
  sidebarVisible.value = false
}

const fetchLogs = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const [startTime, endTime] = searchForm.timeRange
    const result = await listOperationLogs({
      operatorName: searchForm.operatorName || undefined,
      operation: searchForm.operation || undefined,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      pageNum,
      pageSize: size,
    })
    logs.value = result.data.list
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.operatorName = ''
  searchForm.operation = ''
  searchForm.timeRange = []
  void fetchLogs(1)
}

onMounted(() => {
  void fetchLogs()
})
</script>

<template>
  <div class="page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />
    <div class="shell">
      <Header :breadcrumbs="['首页', '日志管理', '操作日志']" @toggle-sidebar="sidebarVisible = true" />
      <main class="content">
        <section class="panel">
          <el-form class="filters" :model="searchForm" label-width="76px">
            <el-form-item label="操作人">
              <el-input v-model="searchForm.operatorName" placeholder="请输入操作人" clearable />
            </el-form-item>
            <el-form-item label="操作内容">
              <el-input v-model="searchForm.operation" placeholder="请输入操作内容" clearable />
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="searchForm.timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" range-separator="至" clearable />
            </el-form-item>
            <div class="actions">
              <el-button type="primary" :icon="Search" @click="fetchLogs(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-panel">
          <el-table v-loading="tableLoading" :data="logs" height="100%" row-key="id">
            <el-table-column prop="operatorName" label="操作人" min-width="140" align="center" />
            <el-table-column prop="operation" label="操作内容" min-width="260" align="center" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP地址" min-width="150" align="center" />
            <el-table-column prop="operateTime" label="操作时间" min-width="180" align="center" />
          </el-table>
          <div class="pager">
            <span>共 {{ total.toLocaleString() }} 条</span>
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" background layout="sizes, prev, pager, next, jumper" :total="total" :page-sizes="[10, 20, 50, 100]" @current-change="(page: number) => fetchLogs(page)" @size-change="(size: number) => fetchLogs(1, size)" />
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.page { height: 100vh; overflow: hidden; background: #f5f7fb; }
.shell { height: 100vh; display: flex; flex-direction: column; margin-left: 264px; overflow: hidden; }
.content { height: calc(100vh - 72px); display: flex; flex-direction: column; gap: 10px; padding: 14px; overflow: hidden; }
.panel, .table-panel { background: #fff; box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04); }
.panel { flex: 0 0 auto; padding: 14px 18px; }
.filters { display: grid; grid-template-columns: repeat(3, minmax(220px, 1fr)) auto; gap: 14px 18px; align-items: center; }
.filters :deep(.el-form-item) { margin-bottom: 0; }
.filters :deep(.el-date-editor) { width: 100%; }
.actions { display: flex; gap: 10px; }
.table-panel { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 14px 18px; }
.pager { flex: 0 0 auto; display: flex; justify-content: space-between; align-items: center; padding-top: 14px; }
@media (max-width: 960px) { .shell { margin-left: 0; } .filters { grid-template-columns: 1fr; } }
</style>
