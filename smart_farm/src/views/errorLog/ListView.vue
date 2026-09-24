<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getErrorLogDetail,
  listErrorLogs,
  updateErrorLogHandle,
  type ErrorLog,
} from '@/api/errorLog'

const sidebarVisible = ref(false)
const tableLoading = ref(false)
const submitLoading = ref(false)
const logs = ref<ErrorLog[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const handleVisible = ref(false)
const currentDetail = ref<ErrorLog>()

// 筛选项布局与操作日志保持一致，并补充错误状态筛选。
const searchForm = reactive({
  traceId: '',
  operatorName: '',
  requestUri: '',
  handleStatus: undefined as number | undefined,
  timeRange: [] as string[],
})

const handleForm = reactive({
  id: 0,
  handleStatus: 1 as 0 | 1 | 2,
  handleRemark: '',
  version: 0,
})

const closeSidebar = () => {
  sidebarVisible.value = false
}

/** 分页读取轻量列表；异常堆栈在详情弹框中按需加载。 */
const fetchLogs = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const [startTime, endTime] = searchForm.timeRange
    const result = await listErrorLogs({
      traceId: searchForm.traceId || undefined,
      operatorName: searchForm.operatorName || undefined,
      requestUri: searchForm.requestUri || undefined,
      handleStatus: searchForm.handleStatus,
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
  searchForm.traceId = ''
  searchForm.operatorName = ''
  searchForm.requestUri = ''
  searchForm.handleStatus = undefined
  searchForm.timeRange = []
  void fetchLogs(1)
}

const openDetail = async (row: ErrorLog) => {
  const result = await getErrorLogDetail(row.id)
  currentDetail.value = result.data
  detailVisible.value = true
}

const openHandle = (row: ErrorLog) => {
  handleForm.id = row.id
  handleForm.handleStatus = row.handleStatus
  handleForm.handleRemark = row.handleRemark || ''
  handleForm.version = row.version
  handleVisible.value = true
}

/** 提交后端受控修改接口，异常事实字段不会随表单发送。 */
const submitHandle = async () => {
  submitLoading.value = true
  try {
    await updateErrorLogHandle(handleForm.id, {
      handleStatus: handleForm.handleStatus,
      handleRemark: handleForm.handleRemark.trim() || undefined,
      version: handleForm.version,
    })
    ElMessage.success('错误日志处理状态已更新')
    handleVisible.value = false
    await fetchLogs()
  } finally {
    submitLoading.value = false
  }
}

const statusText = (status: number) => ['未处理', '已处理', '已忽略'][status] || '未知'
const statusType = (status: number) => (status === 0 ? 'danger' : status === 1 ? 'success' : 'info')

onMounted(() => {
  void fetchLogs()
})
</script>

<template>
  <div class="page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />
    <div class="shell">
      <Header :breadcrumbs="['首页', '日志管理', '错误日志']" @toggle-sidebar="sidebarVisible = true" />
      <main class="content">
        <section class="panel">
          <el-form class="filters" :model="searchForm" label-width="76px">
            <el-form-item label="追踪编号">
              <el-input v-model="searchForm.traceId" placeholder="请输入追踪编号" clearable />
            </el-form-item>
            <el-form-item label="操作人">
              <el-input v-model="searchForm.operatorName" placeholder="请输入操作人" clearable />
            </el-form-item>
            <el-form-item label="请求地址">
              <el-input v-model="searchForm.requestUri" placeholder="请输入请求地址" clearable />
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="searchForm.handleStatus" placeholder="全部" clearable>
                <el-option label="未处理" :value="0" />
                <el-option label="已处理" :value="1" />
                <el-option label="已忽略" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="发生时间">
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
            <el-table-column prop="traceId" label="追踪编号" min-width="190" align="center" show-overflow-tooltip />
            <el-table-column prop="errorMessage" label="错误信息" min-width="220" align="center" show-overflow-tooltip />
            <el-table-column label="请求接口" min-width="230" align="center" show-overflow-tooltip>
              <template #default="{ row }">{{ row.requestMethod || '-' }} {{ row.requestUri || '-' }}</template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" min-width="110" align="center">
              <template #default="{ row }">{{ row.operatorName || '匿名请求' }}</template>
            </el-table-column>
            <el-table-column label="处理状态" min-width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusType(row.handleStatus)">{{ statusText(row.handleStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发生时间" min-width="170" align="center" />
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openHandle(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <span>共 {{ total.toLocaleString() }} 条</span>
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" background layout="sizes, prev, pager, next, jumper" :total="total" :page-sizes="[10, 20, 50, 100]" @current-change="(page: number) => fetchLogs(page)" @size-change="(size: number) => fetchLogs(1, size)" />
          </div>
        </section>
      </main>
    </div>

    <el-dialog v-model="detailVisible" title="错误日志详情" width="760px" destroy-on-close>
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="追踪编号" :span="2">{{ currentDetail.traceId }}</el-descriptions-item>
        <el-descriptions-item label="异常类型" :span="2">{{ currentDetail.exceptionType }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentDetail.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户端IP">{{ currentDetail.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ currentDetail.requestUri || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentDetail.operatorName || '匿名请求' }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ currentDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">{{ currentDetail.errorMessage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">{{ currentDetail.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理信息" :span="2">
          {{ statusText(currentDetail.handleStatus) }} · {{ currentDetail.handlerName || '暂无处理人' }} · {{ currentDetail.handleRemark || '暂无备注' }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="currentDetail" class="stack-section">
        <div class="stack-title">异常堆栈</div>
        <pre>{{ currentDetail.stackTrace || '无堆栈信息' }}</pre>
      </div>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理错误日志" width="520px" destroy-on-close>
      <el-form :model="handleForm" label-width="86px">
        <el-form-item label="处理状态" required>
          <el-radio-group v-model="handleForm.handleStatus">
            <el-radio-button :value="0">未处理</el-radio-button>
            <el-radio-button :value="1">已处理</el-radio-button>
            <el-radio-button :value="2">已忽略</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="请记录原因、处理措施或忽略依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitHandle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 页面骨架、筛选区和分页区复用操作日志的视觉规格。 */
.page { height: 100vh; overflow: hidden; background: #f5f7fb; }
.shell { height: 100vh; display: flex; flex-direction: column; margin-left: 264px; overflow: hidden; }
.content { height: calc(100vh - 72px); display: flex; flex-direction: column; gap: 10px; padding: 14px; overflow: hidden; }
.panel, .table-panel { background: #fff; box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04); }
.panel { flex: 0 0 auto; padding: 14px 18px; }
.filters { display: grid; grid-template-columns: repeat(3, minmax(220px, 1fr)); gap: 14px 18px; align-items: center; }
.filters :deep(.el-form-item) { margin-bottom: 0; }
.filters :deep(.el-date-editor), .filters :deep(.el-select) { width: 100%; }
.actions { display: flex; gap: 10px; }
.table-panel { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 14px 18px; }
.pager { flex: 0 0 auto; display: flex; justify-content: space-between; align-items: center; padding-top: 14px; }
.stack-section { margin-top: 18px; }
.stack-title { margin-bottom: 8px; color: #303133; font-weight: 600; }
.stack-section pre { max-height: 300px; margin: 0; padding: 14px; overflow: auto; border-radius: 4px; background: #101827; color: #d7e1ee; font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
@media (max-width: 960px) { .shell { margin-left: 0; } .filters { grid-template-columns: 1fr; } }
</style>
