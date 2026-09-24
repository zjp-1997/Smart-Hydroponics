<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Location, RefreshLeft } from '@element-plus/icons-vue'
import { listCrops, type Crop } from '@/api/crop'
import {
  addPlantingBatch,
  updatePlantingBatch,
  updatePlantingBatchStatus,
  type PlantingBatchPayload,
  type UpdatePlantingBatchPayload,
} from '@/api/plantingBatch'
import {
  addPlot,
  getPlotById,
  uploadPlotCropImage,
  updatePlot,
  type Plot,
  type PlotBoundaryPoint,
  type PlotPayload,
  type UpdatePlotPayload,
} from '@/api/plot'
import { listFarms, type Farm } from '@/api/farm'
import { getCurrentUserId } from '@/utils/auth'
import { loadAmap, locateWithAmap, reverseGeocodeWithAmap, toAmapCoordinate, toWgs84Coordinate } from '@/utils/amap'
import { getFileUrl } from '@/utils/utils'
import ImageUploadField from '@/components/form/ImageUploadField.vue'

interface PlotFormModel {
  userId?: number
  farmId?: number
  plotName: string
  cropImage?: string
  type?: number
  area?: number
  areaUnit?: string
  address?: string
  coordinate?: string
  boundaryPoints: PlotBoundaryPoint[]
  status?: number
  remark?: string
  currentBatchId?: number
  currentPlantingStatus?: 0 | 1
  currentCropId?: number
  currentPlantedAt?: string
  currentExpectedHarvestAt?: string
  currentExpectedYieldAmount?: number
  currentYieldUnit?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [plot?: Plot]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑地块' : '新增地块'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const farmLoading = ref(false)
const uploadLoading = ref(false)
const locating = ref(false)
const boundaryMapLoading = ref(false)
const boundaryMapError = ref('')
const boundaryMapElement = ref<HTMLDivElement | null>(null)
const drawingBoundary = ref(false)
const boundaryAreaSquareMeters = ref(0)
const farmOptions = ref<Farm[]>([])
const cropOptions = ref<Crop[]>([])
const currentImagePreview = ref('')
let farmRequestSequence = 0
let boundaryMap: AMap.Map | null = null
let boundaryPolygon: AMap.Polygon | null = null
let boundaryDraftLine: AMap.Polyline | null = null
let boundaryVertexMarkers: AMap.CircleMarker[] = []

const typeOptions = [
  { label: '水培种植', value: 1 },
  { label: '大棚种植', value: 2 },
  { label: '室外种植', value: 3 },
]

const areaUnitOptions = ['亩', '平方米', '公顷']
const yieldUnitOptions = ['kg', '斤', '吨']

const plantingStatusOptions = [
  { label: '空闲', value: 0 },
  { label: '种植中', value: 1 },
]

/** 将填写面积统一换算成平方米，用于提示边界面积与业务面积的明显偏差。 */
const declaredAreaSquareMeters = computed(() => {
  if (form.value.area == null) return 0
  if (form.value.areaUnit === '公顷') return form.value.area * 10000
  if (form.value.areaUnit === '亩') return form.value.area * 666.6666667
  return form.value.area
})

const boundaryAreaDiffers = computed(() => {
  if (!boundaryAreaSquareMeters.value || !declaredAreaSquareMeters.value) return false
  return Math.abs(boundaryAreaSquareMeters.value - declaredAreaSquareMeters.value)
    / declaredAreaSquareMeters.value > 0.2
})

const createEmptyForm = (): PlotFormModel => ({
  userId: undefined,
  farmId: undefined,
  plotName: '',
  cropImage: '',
  type: 2,
  area: undefined,
  areaUnit: '亩',
  address: '',
  coordinate: '',
  boundaryPoints: [],
  status: 1,
  remark: '',
  currentBatchId: undefined,
  currentPlantingStatus: 1,
  currentCropId: undefined,
  currentPlantedAt: '',
  currentExpectedHarvestAt: '',
  currentExpectedYieldAmount: undefined,
  currentYieldUnit: 'kg',
})

const form = ref<PlotFormModel>(createEmptyForm())

const formRules = computed<FormRules<PlotFormModel>>(() => ({
  farmId: [{ required: true, message: '请选择所属农场', trigger: 'change' }],
  plotName: [
    { required: true, message: '请输入地块名称', trigger: 'blur' },
    { min: 1, max: 100, message: '地块名称长度不能超过 100 个字符', trigger: 'blur' },
  ],
  cropImage: [{ max: 500, message: '作物图片地址长度不能超过 500 个字符', trigger: 'change' }],
  type: [{ required: true, message: '请选择地块类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  area: [{ type: 'number', min: 0, message: '地块面积不能小于 0', trigger: 'change' }],
  currentPlantingStatus: [{ required: true, message: '请选择种植状态', trigger: 'change' }],
  currentCropId:
    form.value.currentPlantingStatus === 1
      ? [{ required: true, message: '请选择作物名称', trigger: 'change' }]
      : [],
  currentExpectedYieldAmount: [{ type: 'number', min: 0, message: '预计产量不能小于 0', trigger: 'change' }],
  address: [{ max: 255, message: '地址长度不能超过 255 个字符', trigger: 'blur' }],
  coordinate: [
    {
      pattern: /^$|^-?(180(\.0+)?|1[0-7]\d(\.\d+)?|\d{1,2}(\.\d+)?),\s*-?(90(\.0+)?|[1-8]?\d(\.\d+)?)$/,
      message: '经纬度格式必须为经度,纬度',
      trigger: 'blur',
    },
  ],
}))

const getFarmOptionLabel = (farm: Farm) => {
  return farm.farmCode ? `${farm.farmName}（${farm.farmCode}）` : farm.farmName
}

const loadFarmOptions = async () => {
  const requestSequence = ++farmRequestSequence
  farmOptions.value = []
  farmLoading.value = true
  try {
    const result = await listFarms({ status: 1, pageNum: 1, pageSize: 1000 })
    if (requestSequence === farmRequestSequence) {
      farmOptions.value = result.data.list
    }
  } finally {
    if (requestSequence === farmRequestSequence) {
      farmLoading.value = false
    }
  }
}

const loadCropOptions = async () => {
  const cropResult = await listCrops({ pageNum: 1, pageSize: 1000, status: 1 })
  cropOptions.value = cropResult.data.list
}

const resetForm = () => {
  form.value = createEmptyForm()
  farmOptions.value = []
  currentImagePreview.value = ''
  boundaryAreaSquareMeters.value = 0
  formRef.value?.clearValidate()
}

const fillForm = (plot: Plot) => {
  form.value = {
    userId: plot.userId,
    farmId: plot.farmId,
    plotName: plot.plotName || '',
    cropImage: plot.cropImage || plot.currentCropImage || '',
    type: plot.type ?? 2,
    area: plot.area,
    areaUnit: plot.areaUnit || '亩',
    address: plot.address || '',
    coordinate: plot.coordinate || formatCoordinate(plot.longitude, plot.latitude),
    boundaryPoints: (plot.boundaryPoints || []).map((point) => ({ ...point })),
    status: plot.status ?? 1,
    remark: plot.remark || '',
    currentBatchId: plot.currentBatchId,
    currentPlantingStatus: plot.currentBatchId ? 1 : 0,
    currentCropId: plot.currentCropId,
    currentPlantedAt: plot.currentPlantedAt || '',
    currentExpectedHarvestAt: plot.currentExpectedHarvestAt || '',
    currentExpectedYieldAmount: plot.currentExpectedYieldAmount,
    currentYieldUnit: plot.currentYieldUnit || 'kg',
  }
  currentImagePreview.value = resolveImagePreview(plot.cropImage || plot.currentCropImage)
}

const resolveImagePreview = (value?: string) => {
  const normalizedValue = value?.trim()
  if (!normalizedValue) {
    return ''
  }
  if (normalizedValue.startsWith('data:image/') || normalizedValue.startsWith('blob:')) {
    return normalizedValue
  }
  if (
    normalizedValue.startsWith('smart_farm_crop_image:') ||
    normalizedValue.startsWith('smart_farm_planting_batch_crop_image:')
  ) {
    return localStorage.getItem(normalizedValue) || ''
  }
  return getFileUrl(normalizedValue)
}

const formatCoordinate = (longitude?: number, latitude?: number) => {
  if (longitude === undefined || longitude === null || latitude === undefined || latitude === null) {
    return ''
  }

  return `${longitude},${latitude}`
}

const handleFarmChange = (farmId?: number) => {
  form.value.userId = farmOptions.value.find((farm) => farm.id === farmId)?.userId
}

const markCurrentPlantingActive = () => {
  if (form.value.currentPlantingStatus !== 1) {
    form.value.currentPlantingStatus = 1
  }
}

const handleCurrentPlantingFieldChange = (value?: unknown) => {
  if (value !== undefined && value !== null && value !== '') {
    markCurrentPlantingActive()
  }
}

const handleCropChange = () => {
  if (form.value.currentCropId) {
    markCurrentPlantingActive()
  }
}

const fillAddressByCoordinate = async (coordinate: string) => {
  try {
    // 表单保存 WGS-84 坐标，查询地址前由公共高德工具统一转换为 GCJ-02。
    const [longitude, latitude] = coordinate.split(/[,，\s]+/).map(Number)
    if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return
    form.value.address = await reverseGeocodeWithAmap(longitude!, latitude!)
  } catch {
    ElMessage.warning('已获取经纬度，地址自动填写失败')
  }
}

const locateCoordinate = async () => {
  locating.value = true
  try {
    // 开发和生产环境统一调用高德定位插件，并以 WGS-84 格式保存业务坐标。
    const location = await locateWithAmap()
    const coordinate = `${location.longitude.toFixed(6)},${location.latitude.toFixed(6)}`
    form.value.coordinate = coordinate
    if (location.address) {
      form.value.address = location.address
    } else {
      await fillAddressByCoordinate(coordinate)
    }
    void formRef.value?.validateField('coordinate')
    void formRef.value?.validateField('address')
  } catch {
    ElMessage.error('定位失败，请检查浏览器定位权限或高德地图配置')
  } finally {
    locating.value = false
  }
}

/** 将业务边界转换为高德坐标数组，保证边界和底图位置一致。 */
const toAmapBoundaryPath = () => form.value.boundaryPoints.map((point) => {
  const coordinate = toAmapCoordinate(point.longitude, point.latitude)
  return [coordinate.longitude, coordinate.latitude] as [number, number]
})

/** 根据当前顶点刷新多边形；少于三个点时只保留待绘制状态。 */
const renderBoundaryPolygon = () => {
  if (!boundaryMap) return
  const path = toAmapBoundaryPath()
  if (boundaryVertexMarkers.length) boundaryMap.remove(boundaryVertexMarkers)
  boundaryVertexMarkers = path.map((point) => new AMap.CircleMarker({
    map: boundaryMap!,
    center: point,
    radius: 5,
    strokeColor: '#ffffff',
    strokeWeight: 2,
    fillColor: '#10b981',
    fillOpacity: 1,
    zIndex: 30,
  }))
  if (path.length >= 2 && path.length < 3) {
    if (!boundaryDraftLine) {
      boundaryDraftLine = new AMap.Polyline({
        map: boundaryMap,
        path,
        strokeColor: '#10b981',
        strokeWeight: 3,
        lineJoin: 'round',
      })
    } else {
      boundaryDraftLine.setPath(path)
    }
  } else {
    boundaryDraftLine?.setMap(null)
    boundaryDraftLine = null
  }
  if (path.length < 3) {
    boundaryPolygon?.setMap(null)
    boundaryPolygon = null
    boundaryAreaSquareMeters.value = 0
    return
  }
  if (!boundaryPolygon) {
    boundaryPolygon = new AMap.Polygon({
      map: boundaryMap,
      path,
      strokeColor: '#10b981',
      strokeWeight: 3,
      fillColor: '#34d399',
      fillOpacity: 0.18,
      lineJoin: 'round',
    })
  } else {
    boundaryPolygon.setPath(path)
  }
  boundaryAreaSquareMeters.value = boundaryPolygon.getArea()
}

/** 使用顶点平均位置更新地块中心点，保证绘制后无需再次手工填写经纬度。 */
const syncCoordinateFromBoundary = () => {
  if (form.value.boundaryPoints.length < 3) return
  const total = form.value.boundaryPoints.reduce(
    (result, point) => ({
      longitude: result.longitude + point.longitude,
      latitude: result.latitude + point.latitude,
    }),
    { longitude: 0, latitude: 0 },
  )
  form.value.coordinate = `${(total.longitude / form.value.boundaryPoints.length).toFixed(6)},${(total.latitude / form.value.boundaryPoints.length).toFixed(6)}`
}

/** 绘制模式下点击地图即可追加一个顶点，并把高德坐标还原为 WGS-84 后保存。 */
const handleBoundaryMapClick = (event: AMap.MapsEvent<'click', AMap.Map>) => {
  if (!drawingBoundary.value) return
  const point = toWgs84Coordinate(event.lnglat.getLng(), event.lnglat.getLat())
  form.value.boundaryPoints.push({
    longitude: Number(point.longitude.toFixed(6)),
    latitude: Number(point.latitude.toFixed(6)),
  })
  syncCoordinateFromBoundary()
  renderBoundaryPolygon()
}

/** 初始化弹框内的边界绘制地图，并优先定位到已有边界或地块中心点。 */
const initializeBoundaryMap = async () => {
  if (!boundaryMapElement.value || boundaryMap) return
  boundaryMapLoading.value = true
  boundaryMapError.value = ''
  try {
    const AMapApi = await loadAmap()
    const centerValues = String(form.value.coordinate || '').split(/[,，\s]+/).map(Number)
    const center = centerValues.length >= 2 && Number.isFinite(centerValues[0]) && Number.isFinite(centerValues[1])
      ? toAmapCoordinate(centerValues[0]!, centerValues[1]!)
      : toAmapCoordinate(121.4737, 31.2304)
    boundaryMap = new AMapApi.Map(boundaryMapElement.value, {
      center: [center.longitude, center.latitude],
      zoom: form.value.boundaryPoints.length >= 3 ? 16 : 14,
      resizeEnable: true,
      viewMode: '2D',
    })
    boundaryMap.on('click', handleBoundaryMapClick)
    renderBoundaryPolygon()
    if (boundaryPolygon) boundaryMap.setFitView([boundaryPolygon], false, [30, 30, 30, 30], 18)
  } catch {
    boundaryMapError.value = '边界地图加载失败，请检查高德地图配置'
  } finally {
    boundaryMapLoading.value = false
  }
}

/** 切换绘制状态；已有边界保留，用户可继续补充顶点。 */
const toggleBoundaryDrawing = () => {
  drawingBoundary.value = !drawingBoundary.value
}

/** 撤销最后一个顶点，便于修正误点。 */
const undoBoundaryPoint = () => {
  form.value.boundaryPoints.pop()
  syncCoordinateFromBoundary()
  renderBoundaryPolygon()
}

/** 清空边界会随保存请求提交空数组，从而同步清除数据库中的旧边界。 */
const clearBoundary = () => {
  form.value.boundaryPoints = []
  renderBoundaryPolygon()
}

/** 销毁弹框地图，释放事件和 DOM 引用。 */
const destroyBoundaryMap = () => {
  boundaryMap?.off('click', handleBoundaryMapClick)
  boundaryMap?.destroy()
  boundaryMap = null
  boundaryPolygon = null
  boundaryDraftLine = null
  boundaryVertexMarkers = []
  drawingBoundary.value = false
}

const beforeUploadImage = (file: File) => {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.error('作物图片仅支持 JPG、PNG、WEBP 格式')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('作物图片不能超过5MB')
    return false
  }
  return true
}

const handleImageChange = async (file: File) => {
  if (!beforeUploadImage(file)) {
    return
  }

  const previousImage = form.value.cropImage
  const previousPreview = currentImagePreview.value
  uploadLoading.value = true
  try {
    const result = await uploadPlotCropImage(file)
    form.value.cropImage = result.data.imageUrl
    currentImagePreview.value = getFileUrl(result.data.imageUrl)
    ElMessage.success('作物图片上传成功')
  } catch {
    form.value.cropImage = previousImage
    currentImagePreview.value = previousPreview
  } finally {
    uploadLoading.value = false
  }
}

const loadPlotDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getPlotById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const removeEmptyOptionalFields = <T extends Record<string, unknown>>(data: T) => {
  Object.keys(data).forEach((key) => {
    if (data[key] === '' || data[key] === undefined || data[key] === null) {
      delete data[key]
    }
  })

  return data
}

const buildCurrentBatchFields = () => {
  if (form.value.currentPlantingStatus !== 1) {
    return form.value.currentBatchId
      ? {
          currentBatchId: form.value.currentBatchId,
          currentBatchStatus: 0,
        }
      : {}
  }

  return {
    currentBatchId: form.value.currentBatchId,
    currentCropId: form.value.currentCropId,
    currentPlantedAt: form.value.currentPlantedAt,
    currentExpectedHarvestAt: form.value.currentExpectedHarvestAt,
    currentBatchStatus: 1,
    currentExpectedYieldAmount: form.value.currentExpectedYieldAmount,
    currentYieldUnit: form.value.currentYieldUnit,
  }
}

const buildCreatePayload = () => {
  return removeEmptyOptionalFields({
    userId: form.value.userId || getCurrentUserId(),
    farmId: form.value.farmId,
    plotName: form.value.plotName,
    cropImage: form.value.cropImage,
    type: form.value.type,
    area: form.value.area,
    areaUnit: form.value.areaUnit,
    address: form.value.address,
    coordinate: form.value.coordinate,
    boundaryPoints: form.value.boundaryPoints,
    status: form.value.status,
    remark: form.value.remark,
    ...buildCurrentBatchFields(),
  }) as PlotPayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({
    id,
    userId: form.value.userId || getCurrentUserId(),
    farmId: form.value.farmId,
    plotName: form.value.plotName,
    cropImage: form.value.cropImage,
    type: form.value.type,
    area: form.value.area,
    areaUnit: form.value.areaUnit,
    address: form.value.address,
    coordinate: form.value.coordinate,
    boundaryPoints: form.value.boundaryPoints,
    status: form.value.status,
    remark: form.value.remark,
    ...buildCurrentBatchFields(),
  }) as UpdatePlotPayload
}

const buildPlantingBatchPayload = (plotId: number) => {
  return removeEmptyOptionalFields({
    plotId,
    cropId: form.value.currentCropId,
    userId: form.value.userId || getCurrentUserId(),
    plantedAt: form.value.currentPlantedAt,
    expectedHarvestAt: form.value.currentExpectedHarvestAt,
    expectedYieldAmount: form.value.currentExpectedYieldAmount,
    cropImage: form.value.cropImage,
    status: 1,
    yieldUnit: form.value.currentYieldUnit,
  })
}

const saveCurrentPlantingBatch = async (plotId?: number, savedBatchId?: number) => {
  if (!plotId) {
    return
  }

  const batchId = savedBatchId || form.value.currentBatchId

  if (form.value.currentPlantingStatus !== 1) {
    if (batchId) {
      await updatePlantingBatchStatus(batchId, 4)
    }
    return
  }

  const payload = buildPlantingBatchPayload(plotId)

  if (batchId) {
    const updateResult = await updatePlantingBatch({
      id: batchId,
      ...payload,
    } as UpdatePlantingBatchPayload)
    form.value.currentBatchId = updateResult.data.id || batchId
  } else {
    const addResult = await addPlantingBatch(payload as PlantingBatchPayload)
    form.value.currentBatchId = addResult.data.id
  }
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  if (form.value.boundaryPoints.length > 0 && form.value.boundaryPoints.length < 3) {
    ElMessage.warning('地块边界至少需要选择 3 个顶点')
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result =
      props.id === undefined || props.id === null
        ? await addPlot(buildCreatePayload())
        : await updatePlot(buildUpdatePayload(props.id))

    await saveCurrentPlantingBatch(
      result.data.id || props.id || undefined,
      result.data.currentBatchId || form.value.currentBatchId,
    )

    ElMessage.success(isEdit.value ? '编辑地块成功' : '新增地块成功')
    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => dialogVisible.value,
  async (visible) => {
    if (!visible) {
      destroyBoundaryMap()
      return
    }

    resetForm()
    await Promise.all([loadFarmOptions(), loadCropOptions()])

    if (props.id !== undefined && props.id !== null) {
      await loadPlotDetail(props.id)
    }
    await nextTick()
    await initializeBoundaryMap()
  },
)

onBeforeUnmount(destroyBoundaryMap)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="720px"
    class="plot-dialog"
    modal-class="plot-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="plot-form"
        :model="form"
        :rules="formRules"
        label-width="104px"
        autocomplete="off"
      >
        <el-form-item label="所属农场" prop="farmId">
          <el-select
            v-model="form.farmId"
            :loading="farmLoading"
            placeholder="请选择所属农场"
            filterable
            clearable
            @change="handleFarmChange"
          >
            <el-option
              v-for="item in farmOptions"
              :key="item.id"
              :label="getFarmOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="地块名称" prop="plotName">
          <el-input v-model.trim="form.plotName" placeholder="请输入地块名称" clearable />
        </el-form-item>
        <el-form-item label="作物图片" prop="cropImage">
          <ImageUploadField
            :preview-url="currentImagePreview"
            :loading="uploadLoading"
            alt="作物图片"
            @select="handleImageChange"
          />
        </el-form-item>
        <el-form-item label="地块类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择地块类型">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="地块面积" prop="area">
          <el-input-number v-model="form.area" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="面积单位" prop="areaUnit">
          <el-select v-model="form.areaUnit" placeholder="请选择面积单位" filterable>
            <el-option v-for="item in areaUnitOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="种植状态" prop="currentPlantingStatus">
          <el-radio-group v-model="form.currentPlantingStatus">
            <el-radio-button
              v-for="item in plantingStatusOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作物名称" prop="currentCropId">
          <el-select v-model="form.currentCropId" placeholder="请选择作物名称" filterable clearable @change="handleCropChange">
            <el-option v-for="item in cropOptions" :key="item.id" :label="item.cropName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="生长阶段">
          <el-input model-value="由作物生长期配置自动判断" disabled />
        </el-form-item>
        <el-form-item label="种植日期" prop="currentPlantedAt">
          <el-date-picker
            v-model="form.currentPlantedAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择种植日期"
            @change="handleCurrentPlantingFieldChange"
          />
        </el-form-item>
        <el-form-item label="已生长">
          <el-input model-value="根据种植日期自动计算" disabled />
        </el-form-item>
        <el-form-item label="预计采摘日期" prop="currentExpectedHarvestAt">
          <el-date-picker
            v-model="form.currentExpectedHarvestAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择预计采摘日期"
            @change="handleCurrentPlantingFieldChange"
          />
        </el-form-item>
        <el-form-item label="预计产量" prop="currentExpectedYieldAmount">
          <el-input-number
            v-model="form.currentExpectedYieldAmount"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            @change="handleCurrentPlantingFieldChange"
          />
        </el-form-item>
        <el-form-item label="产量单位" prop="currentYieldUnit">
          <el-select v-model="form.currentYieldUnit" placeholder="请选择产量单位" filterable allow-create>
            <el-option v-for="item in yieldUnitOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model.trim="form.address" maxlength="255" placeholder="请输入地址或通过定位自动填写" clearable />
        </el-form-item>
        <el-form-item label="经纬度" prop="coordinate">
          <div class="coordinate-row">
            <el-input v-model.trim="form.coordinate" placeholder="经度,纬度" clearable />
            <el-button :icon="Location" :loading="locating" title="自动定位" @click="locateCoordinate" />
          </div>
        </el-form-item>
        <el-form-item label="地块边界">
          <div class="boundary-editor">
            <div class="boundary-toolbar">
              <el-button type="primary" plain @click="toggleBoundaryDrawing">
                {{ drawingBoundary ? '完成绘制' : (form.boundaryPoints.length ? '继续绘制' : '开始绘制') }}
              </el-button>
              <el-button :icon="RefreshLeft" :disabled="!form.boundaryPoints.length" @click="undoBoundaryPoint">
                撤销一点
              </el-button>
              <el-button :icon="Delete" :disabled="!form.boundaryPoints.length" @click="clearBoundary">
                清除边界
              </el-button>
              <span class="boundary-count">
                {{ form.boundaryPoints.length }} 个顶点
                <template v-if="boundaryAreaSquareMeters"> · 约 {{ boundaryAreaSquareMeters.toFixed(0) }} ㎡</template>
              </span>
            </div>
            <div v-loading="boundaryMapLoading" class="boundary-map-shell">
              <div ref="boundaryMapElement" class="boundary-map" aria-label="地块边界绘制地图"></div>
              <div v-if="boundaryMapError" class="boundary-map-error" role="alert">{{ boundaryMapError }}</div>
              <div v-else-if="drawingBoundary" class="boundary-map-tip">依次点击地块边缘，至少选择 3 个点</div>
            </div>
            <p class="boundary-help">边界用于地图总览范围展示；未绘制时仍按上方经纬度显示点位。</p>
            <p v-if="boundaryAreaDiffers" class="boundary-warning" role="status">
              绘制面积与填写面积相差超过 20%，请核对边界或面积信息。
            </p>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" :disabled="uploadLoading || locating" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.plot-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.plot-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.plot-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.plot-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.plot-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.plot-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.plot-form {
  padding: 2px 2px 0;
}

.plot-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.plot-form :deep(.el-select),
.plot-form :deep(.el-input-number),
.plot-form :deep(.el-date-editor) {
  width: 100%;
}

.coordinate-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 8px;
}

.coordinate-row :deep(.el-button) {
  width: 40px;
  height: 38px;
  padding: 0;
}

.boundary-editor {
  width: 100%;
}

.boundary-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.boundary-toolbar :deep(.el-button) {
  margin-left: 0;
}

.boundary-count {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.boundary-map-shell {
  position: relative;
  height: 280px;
  overflow: hidden;
  border: 1px solid #dbe4ee;
  border-radius: 10px;
  background: #eef2f4;
}

.boundary-map {
  width: 100%;
  height: 100%;
}

.boundary-map-tip,
.boundary-map-error {
  position: absolute;
  right: 12px;
  bottom: 12px;
  left: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  color: #065f46;
  background: rgba(236, 253, 245, 0.94);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);
  font-size: 12px;
  text-align: center;
  pointer-events: none;
}

.boundary-map-error {
  color: #b91c1c;
  background: rgba(254, 242, 242, 0.96);
}

.boundary-help {
  margin: 7px 0 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.boundary-warning {
  margin: 5px 0 0;
  color: #b45309;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 760px) {
  .boundary-count {
    width: 100%;
    margin-left: 0;
  }

  .boundary-map-shell {
    height: 230px;
  }
}

.plot-form :deep(.el-input__wrapper),
.plot-form :deep(.el-select__wrapper),
.plot-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.plot-form :deep(.el-input__wrapper),
.plot-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

@media (max-width: 720px) {
  :global(.plot-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
