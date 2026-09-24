<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { decideFarmJoinRequest, listFarmJoinRequests, type FarmJoinRequest } from '@/api/user'

const visible = defineModel<boolean>({ default: false })
// 列表刷新（包括审核冲突后的重载）时通知父页重新统计，数量不受弹窗状态筛选影响。
const emit = defineEmits<{ success: []; refresh: [] }>()
const status = ref<number | undefined>(0)
const page = ref(1)
const rows = ref<FarmJoinRequest[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const statusNames = ['待审核', '已批准', '已拒绝', '已撤销']
let loadVersion = 0

async function load() {
  const version = ++loadVersion
  loading.value = true
  try {
    const result = await listFarmJoinRequests({ status: status.value, pageNum: page.value, pageSize: 10 })
    if (version !== loadVersion) return
    rows.value = result.data.list
    total.value = result.data.total
  } catch {
    if (version === loadVersion) {
      rows.value = []
      total.value = 0
    }
  } finally {
    if (version === loadVersion) {
      loading.value = false
      emit('refresh')
    }
  }
}

function filterChanged() {
  page.value = 1
  void load()
}

async function decide(row: FarmJoinRequest, nextStatus: number) {
  if (saving.value) return
  saving.value = true
  try {
    let reason = ''
    if (nextStatus === 1) {
      await ElMessageBox.confirm(
        `确认批准 ${row.username} 加入 ${row.ownerName || '该农场'}？该账号将获得${row.requestedRole === 'technician' ? '技术人员' : '普通用户'}权限。`,
        '批准入场申请', { confirmButtonText: '批准', cancelButtonText: '取消', type: 'warning' },
      )
    } else {
      const result = await ElMessageBox.prompt(
        nextStatus === 3 ? '撤销后账号将被禁用、登录失效且成员关系被移除。请填写原因。' : '请填写拒绝原因，申请人将无法登录。',
        nextStatus === 3 ? '撤销入场授权' : '拒绝入场申请',
        { confirmButtonText: '确认', cancelButtonText: '取消', inputType: 'textarea',
          inputValidator: (value) => Boolean(value?.trim()) && value.trim().length <= 500 || '请填写1至500字原因' },
      )
      reason = result.value.trim()
    }
    await decideFarmJoinRequest(row.userId, nextStatus, reason)
    ElMessage.success('操作成功')
    emit('success')
    await load()
  } catch (error) {
    // 请求错误由统一拦截器提示；取消对话框无需额外提示。
    if (error !== 'cancel' && error !== 'close') await load()
  } finally {
    saving.value = false
  }
}

watch(visible, (value) => {
  if (value) { page.value = 1; void load() }
})
</script>

<template>
  <el-dialog v-model="visible" title="入场申请审核" width="min(1100px, 95vw)" destroy-on-close>
    <el-alert title="注册申请不授予农场权限。请核实申请人身份后批准；拒绝和撤销为最终操作。" type="info" :closable="false" show-icon />
    <div class="request-toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable aria-label="申请状态" @change="filterChanged">
        <el-option v-for="(label, value) in statusNames" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" empty-text="暂无入场申请" max-height="480">
      <el-table-column prop="username" label="申请账号" min-width="140" />
      <el-table-column prop="ownerName" label="申请农场主" min-width="130" />
      <el-table-column label="申请角色" width="110">
        <template #default="{ row }">{{ row.requestedRole === 'technician' ? '技术人员' : '普通用户' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'info'">{{ statusNames[row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" min-width="170" />
      <el-table-column label="审核记录" min-width="200">
        <template #default="{ row }">
          <div v-if="row.reviewedAt">审核人 #{{ row.reviewerId }} · {{ row.reviewedAt }}</div>
          <div>{{ row.reviewReason || (row.status === 0 ? '等待审核' : '—') }}</div>
          <div v-if="row.revokedAt">撤销人 #{{ row.revokedBy }} · {{ row.revokedAt }}<br />{{ row.revokeReason }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="145" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="primary" :disabled="saving" @click="decide(row, 1)">批准</el-button>
            <el-button link type="danger" :disabled="saving" @click="decide(row, 2)">拒绝</el-button>
          </template>
          <el-button v-else-if="row.status === 1" link type="danger" :disabled="saving" @click="decide(row, 3)">撤销授权</el-button>
          <span v-else>已结束</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-dialog>
</template>

<style scoped>
.request-toolbar { display: flex; gap: 12px; margin: 16px 0; }
.request-toolbar .el-select { width: 160px; }
.el-pagination { margin-top: 16px; justify-content: flex-end; }
</style>
