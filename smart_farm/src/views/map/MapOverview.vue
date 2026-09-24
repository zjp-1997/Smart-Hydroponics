<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  Aim,
  Close,
  FullScreen,
  Location,
  MapLocation,
  Minus,
  OfficeBuilding,
  Plus,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import LeftMenu from '@/components/LeftMenu.vue'
import { listFarms, type Farm } from '@/api/farm'
import { listPlots, type Plot } from '@/api/plot'
import { loadAmap, locateWithAmap, toAmapCoordinate } from '@/utils/amap'

type EntityKind = 'farm' | 'plot'
type EntityFilter = 'all' | EntityKind
type StatusFilter = 'all' | 'enabled' | 'disabled'

interface MapEntity {
  key: string
  id: number
  kind: EntityKind
  name: string
  code: string
  address: string
  longitude: number
  latitude: number
  enabled: boolean
  area?: number
  areaUnit: string
  farmName?: string
  cropName?: string
  plotCount?: number
  /** 地块边界使用 WGS-84 保存，渲染前逐点转换为高德坐标。 */
  boundaryPoints?: Array<{ longitude: number; latitude: number }>
}

/** 将业务 WGS-84 坐标转换成高德地图覆盖物使用的经纬度数组。 */
const toMapLngLat = (entity: Pick<MapEntity, 'longitude' | 'latitude'>): [number, number] => {
  const coordinate = toAmapCoordinate(entity.longitude, entity.latitude)
  return [coordinate.longitude, coordinate.latitude]
}

const DEFAULT_CENTER = toMapLngLat({ longitude: 121.4737, latitude: 31.2304 })
const DEFAULT_ZOOM = 9
/** 地图允许的最大缩放层级，进入页面和手动定位时都使用该层级。 */
const MAX_ZOOM = 18

const sidebarVisible = ref(false)
const loading = ref(false)
const mapLoading = ref(true)
const mapError = ref('')
const mapElement = ref<HTMLDivElement | null>(null)
const keyword = ref('')
const entityFilter = ref<EntityFilter>('all')
const statusFilter = ref<StatusFilter>('all')
const selectedEntity = ref<MapEntity | null>(null)
const lastUpdatedAt = ref('')
const allEntities = ref<MapEntity[]>([])

let map: AMap.Map | null = null
let entityMarkers: AMap.Marker[] = []
let entityPolygons: AMap.Polygon[] = []
let locationMarker: AMap.Marker | null = null
const markerByKey = new Map<string, AMap.Marker>()
const polygonByKey = new Map<string, AMap.Polygon>()

const filteredEntities = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return allEntities.value.filter((item) => {
    const matchesKind = entityFilter.value === 'all' || item.kind === entityFilter.value
    const matchesStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'enabled' ? item.enabled : !item.enabled)
    const searchable = `${item.name} ${item.code} ${item.address} ${item.farmName || ''} ${item.cropName || ''}`.toLowerCase()
    return matchesKind && matchesStatus && (!normalizedKeyword || searchable.includes(normalizedKeyword))
  })
})

const summary = computed(() => ({
  total: allEntities.value.length,
  farms: allEntities.value.filter((item) => item.kind === 'farm').length,
  plots: allEntities.value.filter((item) => item.kind === 'plot').length,
  enabled: allEntities.value.filter((item) => item.enabled).length,
}))

const selectedTypeLabel = computed(() => selectedEntity.value?.kind === 'farm' ? '农场' : '地块')

const parseCoordinate = (item: Pick<Farm | Plot, 'coordinate' | 'longitude' | 'latitude'>) => {
  const longitude = Number(item.longitude)
  const latitude = Number(item.latitude)
  if (Number.isFinite(longitude) && Number.isFinite(latitude) && longitude !== 0 && latitude !== 0) {
    return { longitude, latitude }
  }

  const values = String(item.coordinate || '').split(/[,，\s]+/).map(Number)
  if (values.length >= 2 && Number.isFinite(values[0]) && Number.isFinite(values[1])) {
    return { longitude: values[0]!, latitude: values[1]! }
  }
  return null
}

const toFarmEntity = (item: Farm): MapEntity | null => {
  const coordinate = parseCoordinate(item)
  if (!coordinate || item.id == null) return null
  return {
    key: `farm-${item.id}`,
    id: item.id,
    kind: 'farm',
    name: item.farmName,
    code: item.farmCode || '-',
    address: item.address || '暂无详细地址',
    ...coordinate,
    enabled: item.status !== 0,
    area: item.totalArea,
    areaUnit: item.areaUnit || '亩',
    plotCount: item.plotCount || 0,
  }
}

const toPlotEntity = (item: Plot): MapEntity | null => {
  const coordinate = parseCoordinate(item)
  if (!coordinate || item.id == null) return null
  return {
    key: `plot-${item.id}`,
    id: item.id,
    kind: 'plot',
    name: item.plotName,
    code: item.plotCode || '-',
    address: item.address || '暂无详细地址',
    ...coordinate,
    enabled: item.status !== 0,
    area: item.area,
    areaUnit: item.areaUnit || '亩',
    farmName: item.farmName,
    cropName: item.currentCropName,
    boundaryPoints: item.boundaryPoints || [],
  }
}

/** 创建与原页面视觉一致的圆形点位 DOM，避免切换地图引擎后改变 UI。 */
const createMarkerContent = (entity: MapEntity) => {
  const markerElement = document.createElement('span')
  markerElement.className = [
    'amap-entity-marker',
    `amap-entity-marker--${entity.kind}`,
    entity.enabled ? '' : 'amap-entity-marker--disabled',
  ].filter(Boolean).join(' ')
  markerElement.setAttribute('aria-hidden', 'true')
  return markerElement
}

const selectEntity = (entity: MapEntity, pan = true) => {
  selectedEntity.value = entity
  // 有边界时展示完整地块范围；历史数据没有边界时继续定位到原中心点。
  const polygon = polygonByKey.get(entity.key)
  if (pan && polygon) map?.setFitView([polygon], false, [90, 90, 90, 390], 18)
  else if (pan) map?.setZoomAndCenter(Math.max(map.getZoom(), 14), toMapLngLat(entity))
  markerByKey.forEach((marker) => marker.setTop(false))
  markerByKey.get(entity.key)?.setTop(true)
  // 重置所有地块样式，再用蓝色强调当前选中边界。
  polygonByKey.forEach((item, key) => {
    const target = allEntities.value.find((candidate) => candidate.key === key)
    item.setOptions({
      strokeColor: target?.enabled === false ? '#94a3b8' : '#10b981',
      fillColor: target?.enabled === false ? '#cbd5e1' : '#34d399',
      strokeWeight: 2,
      fillOpacity: target?.enabled === false ? 0.12 : 0.16,
      zIndex: 10,
    })
  })
  polygon?.setOptions({
    strokeColor: '#2563eb',
    fillColor: '#60a5fa',
    strokeWeight: 3,
    fillOpacity: 0.22,
    zIndex: 20,
  })
}

const renderMarkers = () => {
  if (!map) return
  // 筛选条件变化后同时移除旧点位和旧边界，避免覆盖物重复残留。
  if (entityMarkers.length) map.remove(entityMarkers)
  if (entityPolygons.length) map.remove(entityPolygons)
  entityMarkers = []
  entityPolygons = []
  markerByKey.clear()
  polygonByKey.clear()

  filteredEntities.value.forEach((entity) => {
    if (entity.kind === 'plot' && entity.boundaryPoints && entity.boundaryPoints.length >= 3) {
      const path = entity.boundaryPoints.map((point) => {
        const coordinate = toAmapCoordinate(point.longitude, point.latitude)
        return [coordinate.longitude, coordinate.latitude] as [number, number]
      })
      const polygon = new AMap.Polygon({
        map: map!,
        path,
        strokeColor: entity.enabled ? '#10b981' : '#94a3b8',
        strokeWeight: 2,
        fillColor: entity.enabled ? '#34d399' : '#cbd5e1',
        fillOpacity: entity.enabled ? 0.16 : 0.12,
        lineJoin: 'round',
        cursor: 'pointer',
        zIndex: 10,
      })
      polygon.on('click', () => selectEntity(entity, false))
      entityPolygons.push(polygon)
      polygonByKey.set(entity.key, polygon)
    }
    const marker = new AMap.Marker({
      map: map!,
      position: toMapLngLat(entity),
      anchor: 'center',
      content: createMarkerContent(entity),
      title: entity.name,
      cursor: 'pointer',
      topWhenClick: true,
    })
    marker.on('click', () => selectEntity(entity, false))
    entityMarkers.push(marker)
    markerByKey.set(entity.key, marker)
  })

  if (selectedEntity.value && !filteredEntities.value.some((item) => item.key === selectedEntity.value?.key)) {
    selectedEntity.value = null
  } else if (selectedEntity.value) {
    selectEntity(selectedEntity.value, false)
  }
}

const fitToResults = () => {
  if (!map || filteredEntities.value.length === 0) {
    map?.setZoomAndCenter(DEFAULT_ZOOM, DEFAULT_CENTER)
    return
  }
  // 为左侧筛选面板保留视野边距，并限制自动缩放的最大级别。
  map.setFitView([...entityPolygons, ...entityMarkers], false, [60, 80, 60, 370], 16)
}

const refreshMapSize = () => {
  // 高德地图已开启 resizeEnable；重新设置中心可立即同步外层布局尺寸变化。
  if (map) map.setZoomAndCenter(map.getZoom(), map.getCenter())
}

const zoomIn = () => map?.zoomIn()
const zoomOut = () => map?.zoomOut()

const fetchMapData = async () => {
  loading.value = true
  try {
    const [farmResult, plotResult] = await Promise.allSettled([
      listFarms({ pageNum: 1, pageSize: 1000 }),
      listPlots({ pageNum: 1, pageSize: 1000 }),
    ])
    const farms = farmResult.status === 'fulfilled' ? farmResult.value.data.list : []
    const plots = plotResult.status === 'fulfilled' ? plotResult.value.data.list : []
    allEntities.value = [
      ...farms.map(toFarmEntity).filter((item): item is MapEntity => Boolean(item)),
      ...plots.map(toPlotEntity).filter((item): item is MapEntity => Boolean(item)),
    ]
    lastUpdatedAt.value = new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
    }).format(new Date())
    await nextTick()
    renderMarkers()
    fitToResults()
    if (farmResult.status === 'rejected' || plotResult.status === 'rejected') {
      ElMessage.warning('部分地图数据加载失败，已展示可用数据')
    }
  } finally {
    loading.value = false
  }
}

const locateMe = async () => {
  if (!map) return false
  try {
    // 地图工具栏与页面首次进入共用定位逻辑，并在定位成功后放大至允许的最大层级。
    const location = await locateWithAmap()
    const currentLocation = toMapLngLat(location)
    locationMarker?.setMap(null)
    const markerElement = document.createElement('span')
    markerElement.className = 'amap-location-marker'
    markerElement.setAttribute('aria-hidden', 'true')
    locationMarker = new AMap.Marker({
      map,
      position: currentLocation,
      anchor: 'center',
      content: markerElement,
      title: '我的位置',
      zIndex: 200,
    })
    map.setZoomAndCenter(MAX_ZOOM, currentLocation)
    return true
  } catch {
    ElMessage.warning('无法获取当前位置，请检查浏览器定位权限或高德地图配置')
    return false
  }
}

const initializeMap = async () => {
  if (!mapElement.value || map) return
  mapLoading.value = true
  mapError.value = ''
  try {
    // 使用官方 Loader 和 Web JS API 创建地图，替代无 Key 的瓦片直连方式。
    const AMapApi = await loadAmap()
    map = new AMapApi.Map(mapElement.value, {
      center: DEFAULT_CENTER,
      zoom: DEFAULT_ZOOM,
      zooms: [3, MAX_ZOOM],
      resizeEnable: true,
      viewMode: '2D',
    })
    mapLoading.value = false
  } catch {
    mapLoading.value = false
    mapError.value = '高德地图加载失败，请检查网络、Key、安全密钥或域名白名单'
  }
}

const retryMap = async () => {
  // 销毁失败实例后重新走官方 Loader，并恢复当前筛选结果覆盖物。
  map?.destroy()
  map = null
  entityMarkers = []
  entityPolygons = []
  locationMarker = null
  markerByKey.clear()
  polygonByKey.clear()
  mapError.value = ''
  await initializeMap()
  renderMarkers()
  fitToResults()
  await locateMe()
}

watch([keyword, entityFilter, statusFilter], renderMarkers)

onMounted(async () => {
  await initializeMap()
  await fetchMapData()
  // 用户从菜单进入地图总览后，最终视野应停留在本人位置，而不是地块自动适配视野。
  await locateMe()
})

onBeforeUnmount(() => {
  // 页面卸载时释放地图实例及覆盖物事件，避免后台页面持有 DOM 引用。
  map?.destroy()
  map = null
  entityMarkers = []
  entityPolygons = []
  locationMarker = null
  markerByKey.clear()
  polygonByKey.clear()
})
</script>

<template>
  <div class="admin-page map-page">
    <LeftMenu :visible="sidebarVisible" @close="sidebarVisible = false" />
    <div class="admin-shell">
      <Header :breadcrumbs="['首页', '地图总览']" @toggle-sidebar="sidebarVisible = !sidebarVisible" />

      <main class="admin-content map-content">
        <section class="map-heading" aria-labelledby="map-page-title">
          <div>
            <div class="eyebrow"><span aria-hidden="true"></span> 农业地理信息中心</div>
            <h1 id="map-page-title">地图总览</h1>
            <p>集中查看农场与地块分布、经营状态及种植信息</p>
          </div>
          <div class="map-summary" aria-label="地图数据概览">
            <div><strong>{{ summary.farms }}</strong><span>农场</span></div>
            <div><strong>{{ summary.plots }}</strong><span>地块</span></div>
            <div><strong>{{ summary.enabled }}</strong><span>启用点位</span></div>
            <div class="update-state"><i></i><span>{{ lastUpdatedAt ? `${lastUpdatedAt} 更新` : '正在同步' }}</span></div>
          </div>
        </section>

        <section v-loading="loading || mapLoading" class="map-workspace" aria-label="农场地块地图"
          :aria-busy="loading || mapLoading">
          <div ref="mapElement" class="map-canvas" aria-label="交互式地图"></div>

          <div v-if="mapError" class="map-error" role="alert">
            <span class="map-error-icon"><el-icon><MapLocation /></el-icon></span>
            <strong>地图暂时无法显示</strong>
            <p>{{ mapError }}</p>
            <el-button type="primary" :icon="Refresh" @click="retryMap">重新加载</el-button>
          </div>

          <aside class="map-panel filter-panel" aria-label="地图筛选">
            <div class="panel-title-row">
              <div>
                <span class="panel-kicker">位置检索</span>
                <h2>农场与地块</h2>
              </div>
              <el-button :icon="Refresh" circle aria-label="刷新地图数据" :loading="loading" @click="fetchMapData" />
            </div>

            <el-input v-model="keyword" class="map-search" :prefix-icon="Search" clearable placeholder="搜索名称、编号或地址" aria-label="搜索地图点位" />

            <div class="segmented" aria-label="点位类型筛选">
              <button v-for="item in [{ label: '全部', value: 'all' }, { label: '农场', value: 'farm' }, { label: '地块', value: 'plot' }]"
                :key="item.value" type="button" :class="{ active: entityFilter === item.value }"
                :aria-pressed="entityFilter === item.value" @click="entityFilter = item.value as EntityFilter">
                {{ item.label }}
              </button>
            </div>

            <div class="filter-row">
              <span>运行状态</span>
              <el-select v-model="statusFilter" aria-label="按运行状态筛选">
                <el-option label="全部状态" value="all" />
                <el-option label="已启用" value="enabled" />
                <el-option label="已停用" value="disabled" />
              </el-select>
            </div>

            <div class="result-meta">
              <span>可定位点位</span>
              <strong>{{ filteredEntities.length }} / {{ summary.total }}</strong>
            </div>

            <div class="result-list" role="list">
              <button v-for="item in filteredEntities.slice(0, 30)" :key="item.key" type="button" role="listitem"
                class="result-item" :class="{ selected: selectedEntity?.key === item.key }" @click="selectEntity(item)">
                <span class="result-icon" :class="item.kind">
                  <el-icon><OfficeBuilding v-if="item.kind === 'farm'" /><MapLocation v-else /></el-icon>
                </span>
                <span class="result-copy">
                  <strong>{{ item.name }}</strong>
                  <small>{{ item.farmName || item.address }}</small>
                </span>
                <span class="status-dot" :class="{ disabled: !item.enabled }" :aria-label="item.enabled ? '已启用' : '已停用'"></span>
              </button>
              <el-empty v-if="!filteredEntities.length" :image-size="72" description="暂无匹配的可定位点位" />
            </div>
          </aside>

          <div class="map-toolbar" aria-label="地图工具">
            <el-button :icon="Plus" circle aria-label="放大地图" @click="zoomIn" />
            <el-button :icon="Minus" circle aria-label="缩小地图" @click="zoomOut" />
            <el-button :icon="Aim" circle aria-label="查看全部点位" @click="fitToResults" />
            <el-button :icon="Location" circle aria-label="定位到我的位置并放大" @click="locateMe()" />
            <el-button :icon="FullScreen" circle aria-label="刷新地图尺寸" @click="refreshMapSize" />
          </div>

          <aside v-if="selectedEntity" class="map-panel detail-panel" aria-label="点位详情">
            <button class="close-button" type="button" aria-label="关闭详情" @click="selectedEntity = null">
              <el-icon><Close /></el-icon>
            </button>
            <div class="detail-type"><span :class="selectedEntity.kind"></span>{{ selectedTypeLabel }}</div>
            <h2>{{ selectedEntity.name }}</h2>
            <p>{{ selectedEntity.address }}</p>
            <div class="detail-grid">
              <div><span>编号</span><strong>{{ selectedEntity.code }}</strong></div>
              <div><span>运行状态</span><strong :class="selectedEntity.enabled ? 'text-success' : 'text-muted'">{{ selectedEntity.enabled ? '已启用' : '已停用' }}</strong></div>
              <div><span>面积</span><strong>{{ selectedEntity.area != null ? `${selectedEntity.area}${selectedEntity.areaUnit}` : '-' }}</strong></div>
              <div v-if="selectedEntity.kind === 'farm'"><span>地块数量</span><strong>{{ selectedEntity.plotCount || 0 }} 个</strong></div>
              <div v-else><span>当前作物</span><strong>{{ selectedEntity.cropName || '暂未种植' }}</strong></div>
              <div><span>经度</span><strong>{{ selectedEntity.longitude.toFixed(6) }}</strong></div>
              <div><span>纬度</span><strong>{{ selectedEntity.latitude.toFixed(6) }}</strong></div>
            </div>
          </aside>

          <div class="map-legend" aria-label="地图图例">
            <span><i class="farm"></i>农场</span>
            <span><i class="plot"></i>地块</span>
            <span><i class="disabled"></i>已停用</span>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.map-page {
  --map-blue: #2f80ed;
  --map-green: #10b981;
  --map-ink: #0f172a;
}

.map-content {
  gap: 14px;
  padding: 18px;
  background: #f4f7fb;
}

.map-heading {
  min-height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.eyebrow,
.panel-kicker {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.eyebrow { display: flex; align-items: center; gap: 7px; }
.eyebrow span { width: 7px; height: 7px; border-radius: 50%; background: var(--map-green); box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.12); }
.map-heading h1 { margin: 5px 0 2px; color: var(--map-ink); font-size: 24px; line-height: 1.2; }
.map-heading p { margin: 0; color: #64748b; font-size: 13px; }

.map-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid #e5edf6;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.map-summary > div:not(.update-state) { min-width: 78px; padding: 3px 14px; border-right: 1px solid #edf2f7; }
.map-summary strong, .map-summary span { display: block; }
.map-summary strong { color: var(--map-ink); font-size: 18px; line-height: 1.2; }
.map-summary span { margin-top: 2px; color: #64748b; font-size: 11px; }
.map-summary .update-state { display: flex; align-items: center; gap: 7px; padding: 0 10px; white-space: nowrap; }
.update-state i { width: 7px; height: 7px; border-radius: 50%; background: var(--map-green); }

.map-workspace {
  position: relative;
  flex: 1 1 auto;
  min-height: 460px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #eef2f4;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.09);
}

.map-canvas { position: absolute; inset: 0; z-index: 0; background: #eef2f4; }
.map-canvas :deep(.amap-entity-marker),
.map-canvas :deep(.amap-location-marker) { display: block; box-sizing: border-box; border: 3px solid #fff; border-radius: 50%; box-shadow: 0 2px 8px rgba(15, 23, 42, 0.24); }
.map-canvas :deep(.amap-entity-marker--farm) { width: 20px; height: 20px; background: #1677ff; }
.map-canvas :deep(.amap-entity-marker--plot) { width: 14px; height: 14px; border-width: 2px; background: #00b578; }
.map-canvas :deep(.amap-entity-marker--disabled) { background: #94a3b8; }
.map-canvas :deep(.amap-location-marker) { width: 16px; height: 16px; background: #1677ff; }

.map-error {
  position: absolute;
  inset: 50% auto auto 50%;
  z-index: 520;
  width: min(340px, calc(100% - 40px));
  padding: 28px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.15);
  text-align: center;
  transform: translate(-50%, -50%);
  backdrop-filter: blur(12px);
}
.map-error-icon { width: 48px; height: 48px; display: grid; place-items: center; margin: 0 auto 12px; border-radius: 14px; color: #1677ff; background: #eaf3ff; font-size: 24px; }
.map-error strong { color: #0f172a; font-size: 16px; }
.map-error p { margin: 8px 0 18px; color: #64748b; font-size: 12px; line-height: 1.6; }

.map-panel {
  position: absolute;
  z-index: 500;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.93);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(16px);
}

.filter-panel { top: 16px; bottom: 16px; left: 16px; width: 316px; display: flex; flex-direction: column; padding: 18px; }
.panel-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.panel-title-row h2 { margin: 4px 0 0; color: var(--map-ink); font-size: 19px; }
.panel-title-row :deep(.el-button) { width: 42px; height: 42px; color: #475569; }
.map-search { margin-top: 16px; }
.map-search :deep(.el-input__wrapper) { min-height: 44px; border-radius: 10px; box-shadow: 0 0 0 1px #dce5ef inset; }

.segmented { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-top: 12px; padding: 4px; border-radius: 10px; background: #eef3f8; }
.segmented button { min-height: 36px; border: 0; border-radius: 7px; color: #64748b; background: transparent; cursor: pointer; font-size: 12px; font-weight: 700; transition: 0.2s ease; }
.segmented button:hover { color: #2563eb; }
.segmented button.active { color: #1d4ed8; background: #fff; box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08); }
.segmented button:focus-visible, .result-item:focus-visible, .close-button:focus-visible { outline: 3px solid rgba(47, 128, 237, 0.35); outline-offset: 2px; }

.filter-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 13px; }
.filter-row > span { color: #475569; font-size: 12px; font-weight: 700; }
.filter-row :deep(.el-select) { width: 124px; }
.filter-row :deep(.el-select__wrapper) { min-height: 38px; border-radius: 9px; }
.result-meta { display: flex; justify-content: space-between; margin: 16px 2px 8px; color: #64748b; font-size: 11px; }
.result-meta strong { color: #334155; }

.result-list { flex: 1; min-height: 0; overflow-y: auto; padding-right: 3px; }
.result-list::-webkit-scrollbar { width: 4px; }
.result-list::-webkit-scrollbar-thumb { border-radius: 99px; background: #cbd5e1; }
.result-item { width: 100%; min-height: 58px; display: flex; align-items: center; gap: 10px; margin-bottom: 5px; padding: 8px; border: 1px solid transparent; border-radius: 11px; background: transparent; cursor: pointer; text-align: left; transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease; }
.result-item:hover { background: #f3f7fb; }
.result-item.selected { border-color: rgba(47, 128, 237, 0.24); background: #edf5ff; transform: translateX(2px); }
.result-icon { width: 38px; height: 38px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 10px; font-size: 18px; }
.result-icon.farm { color: #2563eb; background: #eaf2ff; }
.result-icon.plot { color: #059669; background: #e8faf3; }
.result-copy { min-width: 0; flex: 1; }
.result-copy strong, .result-copy small { display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.result-copy strong { color: #1e293b; font-size: 13px; }
.result-copy small { margin-top: 4px; color: #94a3b8; font-size: 11px; }
.status-dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: var(--map-green); box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1); }
.status-dot.disabled { background: #94a3b8; box-shadow: 0 0 0 4px rgba(148, 163, 184, 0.12); }

.map-toolbar { position: absolute; z-index: 500; top: 16px; right: 16px; display: grid; gap: 8px; }
.map-toolbar :deep(.el-button) { width: 44px; height: 44px; margin: 0; border: 1px solid rgba(255, 255, 255, 0.86); color: #334155; background: rgba(255, 255, 255, 0.94); box-shadow: 0 8px 20px rgba(15, 23, 42, 0.13); backdrop-filter: blur(12px); }
.map-toolbar :deep(.el-button:hover) { color: #2563eb; transform: translateY(-1px); }

.detail-panel { right: 72px; bottom: 58px; width: 318px; padding: 22px; }
.close-button { position: absolute; top: 12px; right: 12px; width: 36px; height: 36px; display: grid; place-items: center; border: 0; border-radius: 9px; color: #64748b; background: transparent; cursor: pointer; }
.close-button:hover { color: #0f172a; background: #f1f5f9; }
.detail-type { display: flex; align-items: center; gap: 7px; color: #64748b; font-size: 11px; font-weight: 800; letter-spacing: 0.08em; }
.detail-type span { width: 8px; height: 8px; border-radius: 50%; }
.detail-type span.farm { background: var(--map-blue); }
.detail-type span.plot { background: var(--map-green); }
.detail-panel h2 { margin: 10px 36px 5px 0; color: var(--map-ink); font-size: 20px; }
.detail-panel > p { margin: 0; color: #64748b; font-size: 12px; line-height: 1.55; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 18px; margin-top: 18px; padding-top: 17px; border-top: 1px solid #e8eef5; }
.detail-grid span, .detail-grid strong { display: block; }
.detail-grid span { color: #94a3b8; font-size: 10px; }
.detail-grid strong { overflow: hidden; margin-top: 4px; color: #334155; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.detail-grid .text-success { color: #059669; }
.detail-grid .text-muted { color: #64748b; }

.map-legend { position: absolute; z-index: 500; left: 350px; bottom: 16px; display: flex; gap: 16px; padding: 9px 13px; border: 1px solid rgba(255, 255, 255, 0.75); border-radius: 10px; color: #475569; background: rgba(255, 255, 255, 0.9); box-shadow: 0 7px 18px rgba(15, 23, 42, 0.1); backdrop-filter: blur(10px); font-size: 11px; font-weight: 700; }
.map-legend span { display: flex; align-items: center; gap: 6px; }
.map-legend i { width: 9px; height: 9px; border: 2px solid #fff; border-radius: 50%; box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08); }
.map-legend .farm { background: var(--map-blue); }
.map-legend .plot { background: var(--map-green); }
.map-legend .disabled { background: #94a3b8; }

@media (max-width: 1100px) {
  .map-summary > div:not(.update-state) { min-width: 62px; padding: 3px 9px; }
  .update-state { display: none !important; }
  .detail-panel { width: 280px; }
}

@media (max-width: 760px) {
  .map-content { overflow: auto; padding: 12px; }
  .map-heading { min-height: auto; align-items: flex-start; flex-direction: column; gap: 12px; }
  .map-summary { width: 100%; box-sizing: border-box; }
  .map-summary > div:not(.update-state) { flex: 1; min-width: 0; }
  .map-workspace { flex: none; min-height: 720px; }
  .filter-panel { top: 10px; right: 10px; bottom: auto; left: 10px; width: auto; max-height: 340px; padding: 14px; }
  .map-toolbar { top: 365px; right: 10px; }
  .detail-panel { right: 10px; bottom: 54px; left: 10px; width: auto; }
  .map-legend { left: 10px; bottom: 10px; }
}

@media (prefers-reduced-motion: reduce) {
  .result-item, .segmented button, .map-toolbar :deep(.el-button) { transition: none; }
}
</style>
