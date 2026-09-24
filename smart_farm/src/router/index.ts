import { createRouter, createWebHistory, loadRouteLocation } from 'vue-router'
import { getAuthToken, getCurrentRoleCode, hasPermission } from '@/utils/auth'
import { refreshCurrentAdminSession } from '@/services/adminSession'

// 所有页面均按路由懒加载，避免管理端启动时把全部业务页面装入首屏包。
const LoginView = () => import('@/views/LoginView.vue')
const HomeView = () => import('@/views/home/Home.vue')
const UserListView = () => import('@/views/user/ListView.vue')
const UserOauthListView = () => import('@/views/oauth/ListView.vue')
const ExpertListView = () => import('@/views/expert/ListView.vue')
const FarmListView = () => import('@/views/farm/ListView.vue')
const PlotListView = () => import('@/views/plot/ListView.vue')
const PlantingBatchListView = () => import('@/views/plantingBatch/ListView.vue')
const CropListView = () => import('@/views/crop/ListView.vue')
const CropTypeListView = () => import('@/views/cropType/ListView.vue')
const GrowthStageListView = () => import('@/views/growthStage/ListView.vue')
const DiseasePestListView = () => import('@/views/diseasePest/ListView.vue')
const DiseaseControlListView = () => import('@/views/diseaseControl/ListView.vue')
const DeviceListView = () => import('@/views/device/ListView.vue')
const DeviceFaultListView = () => import('@/views/deviceFault/ListView.vue')
const DeviceTypeListView = () => import('@/views/deviceType/ListView.vue')
// 设备计划页面集中管理四类IoT策略和摄像头图像采集计划。
const DevicePlanListView = () => import('@/views/devicePlan/ListView.vue')
const DeviceDataListView = () => import('@/views/deviceData/ListView.vue')
const CameraDeviceListView = () => import('@/views/cameraDevice/ListView.vue')
const AiRecognitionTypeListView = () => import('@/views/aiRecognitionType/ListView.vue')
const AiRecognitionRecordListView = () => import('@/views/aiRecognitionRecord/ListView.vue')
const FarmTaskListView = () => import('@/views/farmTask/ListView.vue')
const FarmMsgListView = () => import('@/views/farmMsg/ListView.vue')
const MaintenanceMsgListView = () => import('@/views/maintenanceMsg/ListView.vue')
const AlertEventListView = () => import('@/views/alertEvent/ListView.vue')
const SysMsgListView = () => import('@/views/sysMsg/ListView.vue')
const RoleListView = () => import('@/views/role/ListView.vue')
const PermissionListView = () => import('@/views/permission/ListView.vue')
const LoginLogListView = () => import('@/views/loginLog/ListView.vue')
const OperationLogListView = () => import('@/views/operationLog/ListView.vue')
// 错误日志页面按需加载，避免增加首页工作台首屏体积。
const ErrorLogListView = () => import('@/views/errorLog/ListView.vue')
const WarehouseListView = () => import('@/views/warehouse/ListView.vue')
const PhonePictureListView = () => import('@/views/phonePicture/ListView.vue')
const CameraPictureListView = () => import('@/views/cameraPicture/ListView.vue')
const ModelListView = () => import('@/views/model/ListView.vue')
const AiChatListView = () => import('@/views/aiChat/ListView.vue')
const ConsultationListView = () => import('@/views/consultation/ListView.vue')
const MapOverviewView = () => import('@/views/map/MapOverview.vue')

const ROUTE_PERMISSION_MAP: Record<string, string> = {
  mapOverview: 'map:view',
  userList: 'user:manage',
  userOauthList: 'user_oauth:manage',
  roleList: 'role:manage',
  permissionList: 'permission:manage',
  expertList: 'expert:manage',
  expertAudit: 'expert:audit',
  expertEvaluation: 'expert_review:manage',
  farmList: 'farm:manage',
  plotList: 'plot:manage',
  plantingBatchList: 'planting_batch:manage',
  cropList: 'crop:manage',
  cropTypeList: 'crop_type:manage',
  growthStageList: 'growth_stage:manage',
  diseasePestList: 'disease_pest:manage',
  diseaseControlList: 'disease_control:manage',
  deviceList: 'iot_device:manage',
  deviceFaultList: 'iot_device_fault:manage',
  deviceTypeList: 'device_type:manage',
  devicePlanList: 'iot_device_plan:manage',
  cameraDeviceList: 'camera_device:manage',
  environmentDataList: 'sensor_data:manage',
  waterQualityDataList: 'sensor_data:manage',
  lightDataList: 'sensor_data:manage',
  pumpDataList: 'sensor_data:manage',
  aiRecognitionTypeList: 'ai_recognition_type:manage',
  aiRecognitionRecordList: 'ai_recognition_record:manage',
  farmTaskList: 'farm_task:manage',
  farmMsgList: 'farm_msg:manage',
  // 路由守卫与侧栏、后端接口使用同一个维护消息权限码。
  maintenanceMsgList: 'maintenance_msg:manage',
  alertEventList: 'alert_event:manage',
  sysMsgList: 'sys_msg:manage',
  warehouseList: 'warehouse:manage',
  operationLogList: 'operation_log:view',
  errorLogList: 'error_log:manage',
  loginLogList: 'login_log:view',
  phonePictureList: 'crop_image:manage',
  cameraPictureList: 'camera_image:manage',
  modelList: 'model:manage',
  aiChatList: 'ai_chat:manage',
  consultationList: 'consultation:manage',
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView,
      meta: { hidePageTab: true },
    },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
      meta: { pageTitle: '首页工作台', fixedPageTab: true },
    },
    {
      path: '/map/overview',
      name: 'mapOverview',
      component: MapOverviewView,
      meta: { pageTitle: '地图总览' },
    },
    {
      path: '/user/list',
      name: 'userList',
      component: UserListView,
      meta: { pageTitle: '用户信息管理' },
    },
    {
      path: '/user/oauth',
      name: 'userOauthList',
      component: UserOauthListView,
      meta: { pageTitle: '第三方账号管理' },
    },
    {
      path: '/system/role',
      name: 'roleList',
      component: RoleListView,
      meta: { pageTitle: '角色管理' },
    },
    {
      path: '/system/menu',
      alias: '/system/permission',
      name: 'permissionList',
      component: PermissionListView,
      meta: { pageTitle: '菜单管理' },
    },
    {
      path: '/expert/list',
      name: 'expertList',
      component: ExpertListView,
      meta: { pageTitle: '专家信息管理' },
    },
    {
      path: '/expert/audit',
      name: 'expertAudit',
      component: ExpertListView,
      meta: { pageTitle: '专家认证审核' },
    },
    {
      path: '/expert/qualification',
      redirect: '/expert/audit',
    },
    {
      path: '/expert/evaluation',
      name: 'expertEvaluation',
      component: ExpertListView,
      meta: { pageTitle: '专家评价管理' },
    },
    {
      path: '/farm/list',
      name: 'farmList',
      component: FarmListView,
      meta: { pageTitle: '农场信息管理' },
    },
    {
      path: '/plot/list',
      redirect: '/plot/info',
    },
    {
      path: '/plot/info',
      name: 'plotList',
      component: PlotListView,
      meta: { pageTitle: '地块信息管理' },
    },
    {
      path: '/plot/batch',
      name: 'plantingBatchList',
      component: PlantingBatchListView,
      meta: { pageTitle: '种植批次管理' },
    },
    {
      path: '/crop/list',
      name: 'cropList',
      component: CropListView,
      meta: { pageTitle: '作物信息管理' },
    },
    {
      path: '/crop/type',
      name: 'cropTypeList',
      component: CropTypeListView,
      meta: { pageTitle: '作物类型管理' },
    },
    {
      path: '/crop/growth-stage',
      name: 'growthStageList',
      component: GrowthStageListView,
      meta: { pageTitle: '作物生长期管理' },
    },
    // 病虫害基本信息管理页面，对接 smart_plant /disease-pest 系列接口。
    {
      path: '/diseasePest/list',
      name: 'diseasePestList',
      component: DiseasePestListView,
      meta: { pageTitle: '病虫害信息管理' },
    },
    // 防治措施管理页面，对接 smart_plant /disease-control 系列接口。
    {
      path: '/disease-control/list',
      name: 'diseaseControlList',
      component: DiseaseControlListView,
      meta: { pageTitle: '防治措施管理' },
    },
    // 设备信息管理页面，对接 smart_plant /iot-device 系列接口。
    {
      path: '/device/list',
      name: 'deviceList',
      component: DeviceListView,
      meta: { pageTitle: '设备信息管理' },
    },
    // 设备故障管理页面，对接 smart_plant /iot-device-fault 系列接口。
    {
      path: '/device/fault',
      name: 'deviceFaultList',
      component: DeviceFaultListView,
      meta: { pageTitle: '设备故障管理' },
    },
    {
      path: '/device/type',
      name: 'deviceTypeList',
      component: DeviceTypeListView,
      meta: { pageTitle: '设备类型管理' },
    },
    {
      // 独立路由便于深链接、权限控制和保留筛选状态。
      path: '/device/plan',
      name: 'devicePlanList',
      component: DevicePlanListView,
      meta: { pageTitle: '设备计划管理' },
    },
    {
      path: '/monitor/camera',
      name: 'cameraDeviceList',
      component: CameraDeviceListView,
      meta: { pageTitle: '监控管理' },
    },
    {
      path: '/environment-data/list',
      name: 'environmentDataList',
      component: DeviceDataListView,
      meta: { dataMode: 'environment', pageTitle: '环境数据管理' },
    },
    {
      path: '/water-quality-data/list',
      name: 'waterQualityDataList',
      component: DeviceDataListView,
      meta: { dataMode: 'waterQuality', pageTitle: '水质数据管理' },
    },
    {
      path: '/light-data/list',
      name: 'lightDataList',
      component: DeviceDataListView,
      meta: { dataMode: 'light', pageTitle: '补光灯数据管理' },
    },
    {
      path: '/pump-data/list',
      name: 'pumpDataList',
      component: DeviceDataListView,
      meta: { dataMode: 'pump', pageTitle: '水泵数据管理' },
    },
    {
      path: '/ai-recognition/type',
      name: 'aiRecognitionTypeList',
      component: AiRecognitionTypeListView,
      meta: { pageTitle: 'AI识别类型管理' },
    },
    {
      path: '/ai-recognition/record',
      name: 'aiRecognitionRecordList',
      component: AiRecognitionRecordListView,
      meta: { pageTitle: 'AI识别记录管理' },
    },
    {
      path: '/farm-task/list',
      name: 'farmTaskList',
      component: FarmTaskListView,
      // 农事任务管理用于任务计划、执行状态和反馈闭环维护。
      meta: { pageTitle: '农事任务管理' },
    },
    {
      // 独立维护消息页面，与后端 maintenance-msg 接口对应。
      path: '/maintenance-msg/list',
      name: 'maintenanceMsgList',
      component: MaintenanceMsgListView,
      meta: { pageTitle: '维护消息管理' },
    },
    {
      path: '/farm-msg/list',
      name: 'farmMsgList',
      component: FarmMsgListView,
      // 农事消息管理保留公告式列表体验，底层复用 farm-task 数据。
      meta: { pageTitle: '农事消息管理' },
    },
    {
      path: '/alert-event/list',
      name: 'alertEventList',
      component: AlertEventListView,
      meta: { pageTitle: '预警事件管理' },
    },
    {
      path: '/sys_msg/list',
      name: 'sysMsgList',
      component: SysMsgListView,
      meta: { pageTitle: '系统公告' },
    },
    {
      path: '/warehouse/list',
      name: 'warehouseList',
      component: WarehouseListView,
      meta: { pageTitle: '仓库管理' },
    },
    {
      path: '/log/operation',
      name: 'operationLogList',
      component: OperationLogListView,
      meta: { pageTitle: '操作日志' },
    },
    {
      path: '/log/error',
      name: 'errorLogList',
      component: ErrorLogListView,
      meta: { pageTitle: '错误日志' },
    },
    {
      path: '/log/login',
      name: 'loginLogList',
      component: LoginLogListView,
      meta: { pageTitle: '登录日志' },
    },
    {
      path: '/image/phone',
      name: 'phonePictureList',
      component: PhonePictureListView,
      meta: { pageTitle: '手机图片管理' },
    },
    {
      path: '/image/camera',
      name: 'cameraPictureList',
      component: CameraPictureListView,
      meta: { pageTitle: '摄像头图片管理' },
    },
    // 模型管理页面，对接 smart_plant /model 系列接口。
    {
      path: '/model/list',
      name: 'modelList',
      component: ModelListView,
      meta: { pageTitle: '模型管理' },
    },
    // 对话管理页面，对接 smart_plant /ai-chat 系列接口。
    {
      path: '/model/chat',
      name: 'aiChatList',
      component: AiChatListView,
      meta: { pageTitle: '对话管理' },
    },
    // 咨询管理页面，对接 smart_plant /consultation 系列接口。
    {
      path: '/consultation/list',
      name: 'consultationList',
      component: ConsultationListView,
      meta: { pageTitle: '咨询管理' },
    }
  ],
})

const routePreloadTasks = new Map<string, Promise<void>>()

/**
 * 提前下载并解析懒加载页面组件，点击菜单时即可直接完成路由切换。
 * 动态 import 与浏览器模块缓存共同保证同一路由只会加载一次。
 */
export const preloadRoute = (path?: string): Promise<void> => {
  if (!path) {
    return Promise.resolve()
  }

  const resolvedRoute = router.resolve(path)
  const preloadKey = resolvedRoute.path
  const existingTask = routePreloadTasks.get(preloadKey)

  if (existingTask) {
    return existingTask
  }

  const task = loadRouteLocation(resolvedRoute)
    .then(() => undefined)
    .catch(() => {
      // 预加载失败不影响正常导航，并允许后续点击时由路由器重新加载。
      routePreloadTasks.delete(preloadKey)
    })

  routePreloadTasks.set(preloadKey, task)
  return task
}

let backgroundPreloadStarted = false

const startBackgroundRoutePreload = () => {
  if (backgroundPreloadStarted || typeof window === 'undefined') {
    return
  }

  backgroundPreloadStarted = true
  const paths = router
    .getRoutes()
    .filter((route) => route.name !== 'login' && route.components && !route.redirect)
    .map((route) => route.path)

  const preloadNext = () => {
    const path = paths.shift()
    if (!path) {
      return
    }

    void preloadRoute(path).finally(scheduleNext)
  }

  const scheduleNext = () => {
    const requestIdleCallback = (
      window as unknown as { requestIdleCallback?: Window['requestIdleCallback'] }
    ).requestIdleCallback

    if (requestIdleCallback) {
      requestIdleCallback(preloadNext, { timeout: 1500 })
      return
    }
    globalThis.setTimeout(preloadNext, 100)
  }

  scheduleNext()
}

/** 使用服务器最新权限判断路由；管理员仍由后端保留最终授权边界。 */
const canAccessRoute = (routeName: unknown) => {
  const roleCode = getCurrentRoleCode()
  const routePermission = ROUTE_PERMISSION_MAP[String(routeName)]
  if (!roleCode || roleCode === 'admin') return true
  if (routePermission) return hasPermission(routePermission)
  return routeName === 'home'
}

router.beforeEach(async (to) => {
  if (to.name === 'login') {
    return true
  }

  if (!getAuthToken()) {
    return { name: 'login' }
  }

  try {
    // 路由切换时按时间窗口校准用户信息，并与其他同步触发器共享同一请求。
    await refreshCurrentAdminSession()
  } catch {
    // 网络异常时继续使用缓存权限；401由请求层统一清理并跳转登录页。
  }

  if (!canAccessRoute(to.name)) {
    return { name: 'home' }
  }

  return true
})

/** 权限被后台修改或窗口重新激活后，重新校准菜单并退出已失权页面。 */
const revalidateCurrentRoute = async () => {
  if (!getAuthToken()) return
  try {
    await refreshCurrentAdminSession(true)
    const currentRoute = router.currentRoute.value
    if (!canAccessRoute(currentRoute.name)) {
      await router.replace({ name: 'home' })
    }
  } catch {
    // 请求层已经区分并提示401、403和网络失败，此处不重复提示。
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('admin-permission-denied', () => void revalidateCurrentRoute())
  window.addEventListener('focus', () => void revalidateCurrentRoute())
}

router.afterEach((to) => {
  if (to.name !== 'login' && getAuthToken()) {
    startBackgroundRoutePreload()
  }
})

export default router
