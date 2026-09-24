<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { preloadRoute } from '@/router'
import {
  ArrowDown,
  ArrowRight,
  Bell,
  ChatDotRound,
  Cpu,
  DataAnalysis,
  Grape,
  HomeFilled,
  MapLocation,
  Message,
  Monitor,
  OfficeBuilding,
  Picture,
  Setting,
  Tools,
  User,
  UserFilled,
  VideoCamera,
  WarningFilled,
} from '@element-plus/icons-vue'
import { getCurrentUser, hasPermission, subscribeCurrentUser } from '@/utils/auth'
import { systemSetting } from '@/stores/systemSetting'
import { getFileUrl } from '@/utils/utils'

interface MenuItem {
  title: string
  icon: Component
  path?: string
  permission?: string
  roles?: string[]
  children?: Array<{
    title: string
    path?: string
    permission?: string
    roles?: string[]
  }>
}

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const route = useRoute()
const router = useRouter()
const currentUser = ref(getCurrentUser())
const currentRoleCode = computed(() => (currentUser.value?.roleCode || currentUser.value?.role || '').toLowerCase())
const brandIconUrl = computed(() => getFileUrl(systemSetting.faviconUrl || systemSetting.logoUrl || ''))
const menuScrollRef = ref<HTMLElement | null>(null)
let unsubscribeCurrentUser: (() => void) | undefined

const MENU_OPENED_GROUPS_STORAGE_KEY = 'smart_farm_left_menu_opened_groups'
const MENU_SCROLL_TOP_STORAGE_KEY = 'smart_farm_left_menu_scroll_top'

const defaultOpenedGroups: Record<string, boolean> = {
  系统设置: false,
  用户管理: false,
  专家管理: false,
  农场管理: false,
  地块管理: false,
  作物管理: false,
  // 新增病虫害知识库一级菜单的默认状态，登录后保持收起，点击一级菜单后展开。
  病虫害知识库: false,
  设备管理: false,
  环境监测管理: false,
  图片管理: false,
  'AI 识别管理': false,
  模型信息管理: false,
  消息通知管理: false,
  日志管理: false,
}

const getStoredOpenedGroups = () => {
  try {
    const value = sessionStorage.getItem(MENU_OPENED_GROUPS_STORAGE_KEY)
    return value ? (JSON.parse(value) as Record<string, boolean>) : {}
  } catch {
    return {}
  }
}

const saveOpenedGroups = () => {
  sessionStorage.setItem(MENU_OPENED_GROUPS_STORAGE_KEY, JSON.stringify(openedGroups))
}

const getStoredScrollTop = () => {
  const value = sessionStorage.getItem(MENU_SCROLL_TOP_STORAGE_KEY)
  const scrollTop = Number(value)

  return Number.isFinite(scrollTop) && scrollTop >= 0 ? scrollTop : 0
}

const saveScrollTop = () => {
  const scrollTop = menuScrollRef.value?.scrollTop ?? 0
  sessionStorage.setItem(MENU_SCROLL_TOP_STORAGE_KEY, String(scrollTop))
}

const restoreScrollTop = () => {
  const menuScroll = menuScrollRef.value

  if (!menuScroll) {
    return
  }

  menuScroll.scrollTop = getStoredScrollTop()
}

const openedGroups = reactive<Record<string, boolean>>({
  ...defaultOpenedGroups,
  ...getStoredOpenedGroups(),
})

const menuItemsSource: MenuItem[] = [
  { title: '首页工作台', icon: HomeFilled, path: '/home', permission: 'home:view' },
  {
    title: '系统设置',
    icon: Setting,
    children: [
      { title: '角色管理', path: '/system/role', permission: 'role:manage' },
      { title: '菜单管理', path: '/system/menu', permission: 'permission:manage' },
      { title: '系统配置', permission: 'system_config:manage' },
    ],
  },
  {
    title: '用户管理',
    icon: User,
    children: [
      { title: '用户信息管理', path: '/user/list', permission: 'user:manage' },
      { title: '第三方账号管理', path: '/user/oauth', permission: 'user_oauth:manage' },
    ],
  },
  {
    title: '专家管理',
    icon: UserFilled,
    children: [
      { title: '专家信息管理', path: '/expert/list', permission: 'expert:manage' },
      { title: '专家认证审核', path: '/expert/audit', permission: 'expert:audit' },
      { title: '专家评价管理', path: '/expert/evaluation', permission: 'expert_review:manage' },
    ],
  },
  {
    title: '农场管理',
    icon: OfficeBuilding,
    children: [
      { title: '农场信息管理', path: '/farm/list', permission: 'farm:manage' },
    ],
  },
  {
    title: '地块管理',
    icon: MapLocation,
    children: [
      { title: '地块信息管理', path: '/plot/info', permission: 'plot:manage' },
      { title: '种植批次管理', path: '/plot/batch', permission: 'planting_batch:manage' },
    ],
  },
  {
    title: '作物管理',
    icon: Grape,
    children: [
      { title: '作物类型管理', path: '/crop/type', permission: 'crop_type:manage' },
      { title: '作物生长期管理', path: '/crop/growth-stage', permission: 'growth_stage:manage' },
      { title: '作物信息管理', path: '/crop/list', permission: 'crop:manage' }
    ],
  },
  {
    title: '病虫害知识库',
    icon: WarningFilled,
    // 两个二级菜单分别进入病虫害基本信息和防治措施管理页面。
    children: [
      { title: '病虫害信息管理', path: '/diseasePest/list', permission: 'disease_pest:manage' },
      { title: '防治措施管理', path: '/disease-control/list', permission: 'disease_control:manage' },
    ],
  },
  {
    title: '设备管理',
    icon: Cpu,
    children: [
      { title: '设备类型管理', path: '/device/type', permission: 'device_type:manage' },
      { title: '设备信息管理', path: '/device/list', permission: 'iot_device:manage' },
      { title: '设备故障管理', path: '/device/fault', permission: 'iot_device_fault:manage' },
      // 设备采集、自动控制和摄像头图像采集计划统一从该入口维护。
      { title: '设备计划管理', path: '/device/plan', permission: 'iot_device_plan:manage' },
    ],
  },
  { title: '监控管理', icon: VideoCamera, path: '/monitor/camera', permission: 'camera_device:manage' },
  {
    title: '环境监测管理',
    icon: Monitor,
    children: [
      { title: '环境数据管理', path: '/environment-data/list', permission: 'sensor_data:manage' },
      { title: '水质数据管理', path: '/water-quality-data/list', permission: 'water_quality_data:manage' },
      { title: '补光灯数据管理', path: '/light-data/list', permission: 'light_data:manage' },
      { title: '水泵数据管理', path: '/pump-data/list', permission: 'pump_data:manage' },
    ],
  },
  { title: '地图总览', icon: MapLocation, path: '/map/overview', permission: 'map:view' },
  {
    title: '图片管理',
    icon: Picture,
    children: [
      { title: '手机图片管理', path: '/image/phone', permission: 'crop_image:manage' },
      { title: '摄像头图片管理', path: '/image/camera', permission: 'camera_image:manage' },
    ],
  },
  {
    title: 'AI 识别管理',
    icon: DataAnalysis,
    children: [
      { title: 'AI识别类型管理', path: '/ai-recognition/type', permission: 'ai_recognition_type:manage' },
      { title: 'AI识别记录管理', path: '/ai-recognition/record', permission: 'ai_recognition_record:manage' },
    ],
  },
  {
    title: '模型信息管理',
    icon: ChatDotRound,
    // 模型配置与AI咨询历史拆分为两个二级菜单，便于权限控制和日常运营查看。
    children: [
      { title: '模型管理', path: '/model/list', permission: 'model:manage' },
      { title: '对话管理', path: '/model/chat', permission: 'ai_chat:manage' },
    ],
  },
  // 农事任务管理作为独立一级菜单，负责任务计划、执行状态和反馈闭环。
  { title: '农事任务管理', icon: Tools, path: '/farm-task/list', permission: 'farm_task:manage' },
  { title: '预警事件管理', icon: Bell, path: '/alert-event/list', permission: 'alert_event:manage' },
  {
    title: '消息通知管理',
    icon: Message,
    // 消息通知管理承载系统公告、农事消息与独立维护消息菜单。
    children: [
      { title: '系统公告', path: '/sys_msg/list', permission: 'sys_msg:manage' },
      { title: '农事消息管理', path: '/farm-msg/list', permission: 'farm_msg:manage' },
      { title: '维护消息管理', path: '/maintenance-msg/list', permission: 'maintenance_msg:manage' },
    ],
  },
  { title: '咨询管理', icon: ChatDotRound, path: '/consultation/list', permission: 'consultation:manage' },
  { title: '仓库管理', icon: OfficeBuilding, path: '/warehouse/list', permission: 'warehouse:manage' },
  {
    title: '日志管理',
    icon: Setting,
    // 错误日志与现有操作、登录日志归入同一菜单组。
    children: [
      { title: '操作日志', path: '/log/operation', permission: 'operation_log:view' },
      { title: '错误日志', path: '/log/error', permission: 'error_log:manage' },
      { title: '登录日志', path: '/log/login', permission: 'login_log:view' },
    ],
  },
]

const canShowByAccess = (roles?: string[], permission?: string) => {
  const roleCode = currentRoleCode.value

  if (roleCode === 'admin') {
    return true
  }
  if (permission) {
    return hasPermission(permission, currentUser.value)
  }

  return !roles?.length || roles.includes(roleCode)
}

const menuItems = computed(() =>
  menuItemsSource
    .map((item) => ({
      ...item,
      children: item.children?.filter((child) => canShowByAccess(child.roles, child.permission)),
    }))
    .filter((item) => item.children ? item.children.length > 0 && canShowByAccess(item.roles, item.permission) : canShowByAccess(item.roles, item.permission)),
)

const isActive = (item: MenuItem) => {
  return Boolean(item.path && route.path === item.path)
}

const toggleGroup = (title: string) => {
  saveScrollTop()
  openedGroups[title] = !openedGroups[title]
  saveOpenedGroups()

  void nextTick(() => {
    restoreScrollTop()
  })
}

const navigateTo = async (path?: string) => {
  if (!path) {
    return
  }

  saveScrollTop()
  await router.push(path)
  emit('close')
}

const warmRoute = (path?: string) => {
  void preloadRoute(path)
}

onMounted(() => {
  // 权限缓存更新后立即重算菜单，无需等待页面重新加载。
  unsubscribeCurrentUser = subscribeCurrentUser((user) => {
    currentUser.value = user
  })
  void nextTick(() => {
    restoreScrollTop()
    window.requestAnimationFrame(restoreScrollTop)
  })
})

onBeforeUnmount(() => {
  unsubscribeCurrentUser?.()
  saveScrollTop()
})

</script>

<template>
  <div class="sidebar-mask" :class="{ show: visible }" @click="emit('close')" />

  <aside class="left-menu" :class="{ open: visible }">
    <div class="brand">
      <div class="brand-icon">
        <img v-if="brandIconUrl" :src="brandIconUrl" :alt="`${systemSetting.systemName}网站图标`" />
        <el-icon v-else>
          <Grape />
        </el-icon>
      </div>
      <div>
        <strong>Smart Plant</strong>
        <span>智慧水培后台</span>
      </div>
    </div>

    <nav ref="menuScrollRef" class="menu-scroll" aria-label="后台菜单" @scroll.passive="saveScrollTop">
      <template v-for="item in menuItems" :key="item.title">
        <button v-if="item.children" class="menu-item" :class="{ active: isActive(item) }" type="button"
          @click="toggleGroup(item.title)">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
          <el-icon class="menu-arrow">
            <component :is="openedGroups[item.title] ? ArrowDown : ArrowRight" />
          </el-icon>
        </button>

        <button v-else class="menu-item" :class="{ active: isActive(item) }" type="button"
          @pointerenter="warmRoute(item.path)" @focus="warmRoute(item.path)" @click="navigateTo(item.path)">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </button>

        <div v-if="item.children && openedGroups[item.title]" class="submenu">
          <button v-for="child in item.children" :key="child.title" class="submenu-item"
            :class="{ active: child.path === route.path }" type="button"
            @pointerenter="warmRoute(child.path)" @focus="warmRoute(child.path)" @click="navigateTo(child.path)">
            {{ child.title }}
          </button>
        </div>
      </template>
    </nav>
  </aside>
</template>

<style scoped>
.left-menu {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 50;
  width: 264px;
  display: flex;
  flex-direction: column;
  color: #0f172a;
  background: #ffffff;
  box-shadow: 10px 0 30px rgba(15, 23, 42, 0.08);
}

.brand {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid #eef2f7;
}

.brand-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: transparent;
  font-size: 20px;
}

.brand-icon img {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.brand strong,
.brand span {
  display: block;
}

.brand strong {
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.brand span {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.menu-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 14px 10px 24px;
}

.menu-scroll::-webkit-scrollbar {
  width: 4px;
}

.menu-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #cbd5e1;
}

.menu-item,
.submenu-item {
  width: 100%;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0;
  text-align: left;
}

.menu-item {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
  padding: 0 12px;
  border-radius: 10px;
  color: #334155;
  transition: background 0.2s ease, color 0.2s ease;
}

.menu-item span {
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}

.menu-item .el-icon,
.submenu-item .el-icon {
  font-size: 14px;
}

.menu-item:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.menu-item.active {
  color: #ffffff;
  background: #409eff;
}

.menu-item.active {
  box-shadow: none;
}

.menu-item .el-icon:first-child {
  color: #409eff;
}

.menu-item.active .el-icon,
.menu-item.active .menu-arrow {
  color: #ffffff;
}

.menu-arrow {
  margin-left: auto;
  color: #94a3b8;
}

.submenu {
  margin: -2px 0 8px;
}

.submenu-item {
  min-height: 42px;
  display: flex;
  align-items: center;
  margin: 0 0 4px;
  padding: 0 12px 0 38px;
  border-radius: 10px;
  color: #64748b;
  transition: background 0.2s ease, color 0.2s ease;
}

.submenu-item:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.submenu-item.active {
  color: #ffffff;
  background: #409eff;
  font-weight: 500;
}

.sidebar-mask {
  display: none;
}

@media (max-width: 960px) {
  .left-menu {
    transform: translateX(-100%);
    transition: transform 0.24s ease;
  }

  .left-menu.open {
    transform: translateX(0);
  }

  .sidebar-mask {
    position: fixed;
    inset: 0;
    z-index: 45;
    display: block;
    pointer-events: none;
    background: rgba(15, 23, 42, 0);
    transition: background 0.24s ease;
  }

  .sidebar-mask.show {
    pointer-events: auto;
    background: rgba(15, 23, 42, 0.42);
  }
}
</style>
