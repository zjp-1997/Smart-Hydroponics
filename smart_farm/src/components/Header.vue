<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Bell, Grape, Menu, Search, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { downloadAdminAvatar, logoutAdmin } from '@/api/auth'
import {
  getHomeHeaderSummary,
  searchHomeResources,
  type HomeSearchResultItem,
} from '@/api/homeDashboard'
import { getCurrentUser, subscribeCurrentUser } from '@/utils/auth'
import { clearLocalStorage, getFileUrl } from '@/utils/utils'
import PageTabs from '@/components/PageTabs.vue'
import ProfileDialog from '@/components/ProfileDialog.vue'
import SystemSettingsDialog from '@/components/SystemSettingsDialog.vue'
import { usePageTabsStore } from '@/stores/pageTabs'
import { systemSetting } from '@/stores/systemSetting'

const router = useRouter()
const pageTabsStore = usePageTabsStore()

withDefaults(
  defineProps<{
    breadcrumbs?: string[]
  }>(),
  {
    breadcrumbs: () => ['首页', '工作台'],
  },
)

const emit = defineEmits<{
  toggleSidebar: []
}>()

// 使用 ref 保存用户信息，使个人中心保存后顶部昵称和头像能够立即刷新。
const currentUser = ref(getCurrentUser())
const headerAvatarUrl = ref('')
const searchKeyword = ref('')
const unreadNoticeCount = ref(0)
const profileDialogVisible = ref(false)
const systemSettingsDialogVisible = ref(false)
const roleLabelMap: Record<string, string> = {
  admin: '管理员',
  farm_owner: '农场主',
  technician: '技术人员',
  user: '普通用户',
}
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '当前用户')
const isAdmin = computed(() => (currentUser.value?.roleCode || currentUser.value?.role || '').toLowerCase() === 'admin')
const headerLogoUrl = computed(() => systemSetting.logoUrl ? getFileUrl(systemSetting.logoUrl) : '')
const roleName = computed(() => {
  const roleCode = currentUser.value?.roleCode || currentUser.value?.role || ''
  return currentUser.value?.roleName || roleLabelMap[roleCode] || 'Smart Plant'
})

let avatarRequestVersion = 0

const clearHeaderAvatar = () => {
  if (headerAvatarUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(headerAvatarUrl.value)
  }
  headerAvatarUrl.value = ''
}

/** 顶部头像同样通过请求层携带 JWT，避免受保护图片直接加载失败。 */
const loadHeaderAvatar = async (avatar = currentUser.value?.avatar) => {
  const requestVersion = ++avatarRequestVersion
  if (!avatar) {
    clearHeaderAvatar()
    return
  }

  try {
    const response = await downloadAdminAvatar(avatar)
    if (requestVersion !== avatarRequestVersion) return
    clearHeaderAvatar()
    headerAvatarUrl.value = URL.createObjectURL(response.data)
  } catch {
    if (requestVersion === avatarRequestVersion) clearHeaderAvatar()
  }
}

const searchRouteMap: Record<HomeSearchResultItem['type'], string> = {
  farmTask: '/farm-task/list',
  iotDevice: '/device/list',
  plot: '/plot/info',
}

/** 读取真实未读通知数；接口失败时保留 0，避免顶部栏阻塞其他页面。 */
const fetchHeaderSummary = async () => {
  try {
    const result = await getHomeHeaderSummary()
    unreadNoticeCount.value = Number(result.data.unreadNoticeCount || 0)
  } catch {
    unreadNoticeCount.value = 0
  }
}

/** Element Plus 自动完成回调，空关键字不请求后端。 */
const fetchSearchSuggestions = async (
  keyword: string,
  callback: (items: HomeSearchResultItem[]) => void,
) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    callback([])
    return
  }

  try {
    const result = await searchHomeResources(normalizedKeyword)
    callback(result.data.items || [])
  } catch {
    callback([])
  }
}

/** 点击搜索结果后进入对应业务列表，并携带关键字供列表页自动筛选。 */
const openSearchResult = (item: HomeSearchResultItem) => {
  searchKeyword.value = item.title
  void router.push({
    path: searchRouteMap[item.type],
    query: { keyword: item.title, source: 'home-search' },
  })
}

/** 回车时选择匹配度最高的结果；无结果时给出明确反馈。 */
const submitSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    return
  }
  try {
    const result = await searchHomeResources(keyword, 1)
    const firstItem = result.data.items?.[0]
    if (firstItem) {
      openSearchResult(firstItem)
      return
    }
    ElMessage.info('未找到相关任务、设备或地块')
  } catch {
    // 请求层已统一展示接口错误，此处不重复弹窗。
  }
}

const openNoticeCenter = () => {
  void router.push('/sys_msg/list')
}

const refreshCurrentUser = () => {
  currentUser.value = getCurrentUser()
}

let unsubscribeCurrentUser: (() => void) | undefined

/** 响应首页快捷操作事件，复用顶部栏中唯一的系统设置弹框实例。 */
const openSystemSettings = () => {
  if (isAdmin.value) {
    systemSettingsDialogVisible.value = true
    return
  }
  ElMessage.warning('只有管理员可以修改系统设置')
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出当前登录账号吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
    })

    try {
      // 通知后端使当前令牌失效；即使接口异常，也继续清理前端登录状态。
      await logoutAdmin()
    } catch {
      // 退出登录应以本地登录态清理为准，避免网络异常导致用户无法退出。
    }

    clearLocalStorage()
    pageTabsStore.resetTabs()
    ElMessage.success('已退出登录')
    await router.replace('/')
  } catch {
    ElMessage.info('已取消退出')
  }
}

const handleCommand = (command: string) => {
  if (command === 'profile') {
    profileDialogVisible.value = true
    return
  }
  if (command === 'settings') {
    openSystemSettings()
    return
  }
  if (command === 'logout') {
    void handleLogout()
  }
}

onMounted(() => {
  void fetchHeaderSummary()
  // 令牌刷新、路由校准和个人资料修改统一通过用户缓存订阅更新头部。
  unsubscribeCurrentUser = subscribeCurrentUser((user) => {
    currentUser.value = user
    void loadHeaderAvatar(user?.avatar)
  })
  window.addEventListener('open-system-settings', openSystemSettings)
})

onBeforeUnmount(() => {
  avatarRequestVersion += 1
  clearHeaderAvatar()
  unsubscribeCurrentUser?.()
  window.removeEventListener('open-system-settings', openSystemSettings)
})
</script>

<template>
  <div class="header-stack">
    <header class="top-header">
      <div class="header-left">
        <el-button class="menu-button" :icon="Menu" circle @click="emit('toggleSidebar')" />
        <div class="header-logo">
          <img v-if="headerLogoUrl" :src="headerLogoUrl" :alt="`${systemSetting.systemName} Logo`" />
          <el-icon v-else><Grape /></el-icon>
        </div>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">
            {{ item }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="header-right">
        <!-- <el-autocomplete
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索任务、设备或地块"
          :prefix-icon="Search"
          :fetch-suggestions="fetchSearchSuggestions"
          :trigger-on-focus="false"
          :debounce="300"
          value-key="title"
          clearable
          @select="openSearchResult"
          @keyup.enter="submitSearch"
        >
          <template #default="{ item }">
            <div class="search-option">
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </div>
          </template>
        </el-autocomplete> -->

        <el-badge :value="unreadNoticeCount" :max="99" class="notice-badge">
          <el-button
            :icon="Bell"
            circle
            :aria-label="`打开消息中心，${unreadNoticeCount} 条未读消息`"
            @click="openNoticeCenter"
          />
        </el-badge>

        <el-dropdown trigger="click" @command="handleCommand">
          <button class="user-profile" type="button">
            <span class="avatar">
              <img v-if="headerAvatarUrl" :src="headerAvatarUrl" :alt="`${displayName}头像`" />
              <el-icon v-else><UserFilled /></el-icon>
            </span>
            <span class="user-info">
              <strong>{{ displayName }}</strong>
              <small>{{ roleName }}</small>
            </span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="isAdmin" command="settings">系统设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <PageTabs />
    <!-- 个人中心作为全局弹框挂载在顶部组件中，不改变现有业务页面布局。 -->
    <ProfileDialog v-model="profileDialogVisible" @updated="refreshCurrentUser" />
    <!-- 系统设置与个人中心使用同级全局弹框，避免切换路由打断当前工作。 -->
    <SystemSettingsDialog v-model="systemSettingsDialogVisible" />
  </div>
</template>

<style scoped>
.header-stack {
  position: sticky;
  top: 0;
  z-index: 30;
}

.top-header {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(16px);
}

.header-left,
.header-right,
.user-profile {
  display: flex;
  align-items: center;
}

.header-left {
  min-width: 0;
  gap: 14px;
}

.header-logo {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 12px;
  color: #ffffff;
  background: transparent;
}

.header-logo img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.menu-button {
  display: none;
}

.header-right {
  min-width: 0;
  flex: 1 1 auto;
  justify-content: flex-end;
  gap: 12px;
}

.search-input {
  width: 260px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.search-option {
  display: grid;
  gap: 2px;
  padding: 5px 0;
}

.search-option strong {
  overflow: hidden;
  color: #1e293b;
  font-size: 13px;
  text-overflow: ellipsis;
}

.search-option small {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  text-overflow: ellipsis;
}

.notice-badge :deep(.el-badge__content) {
  border: 0;
  background: #f43f5e;
}

.notice-badge {
  flex: 0 0 auto;
}

.user-profile {
  min-width: 0;
  max-width: 200px;
  flex: 0 0 auto;
  gap: 10px;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.avatar {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: #259aaa;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  min-width: 0;
  display: grid;
  text-align: left;
  gap: 4px;
}

.user-info strong,
.user-info small {
  display: block;
  max-width: 140px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.user-info strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.user-info small {
  color: #64748b;
  font-size: 11px;

}

@media (max-width: 960px) {
  .top-header {
    padding: 0 18px;
  }

  .menu-button {
    display: inline-flex;
  }

  .header-logo {
    display: none;
  }

  .search-input {
    width: min(40vw, 220px);
  }
}

@media (max-width: 640px) {
  .top-header {
    height: auto;
    min-height: 68px;
    flex-wrap: wrap;
    padding: 12px 14px;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
  }

  .search-input {
    width: min(100%, 220px);
  }

  .user-info {
    display: none;
  }
}
</style>
