import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/** 地块边界顶点，业务接口统一使用 WGS-84 坐标。 */
export interface PlotBoundaryPoint {
  longitude: number
  latitude: number
}

export interface Plot {
  id?: number
  farmId?: number
  farmName?: string
  farmCode?: string
  userId: number
  username?: string
  nickname?: string
  plotName: string
  plotCode?: string
  cropImage?: string
  type?: number
  area?: number
  areaUnit?: string
  address?: string
  coordinate?: string
  longitude?: number
  latitude?: number
  /** 按顺序连接并自动闭合的地块边界顶点。 */
  boundaryPoints?: PlotBoundaryPoint[]
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
  currentBatchId?: number
  currentBatchNo?: string
  currentCropId?: number
  currentCropName?: string
  currentCropVariety?: string
  currentCropImage?: string
  currentPlantingArea?: number
  currentPlantingAreaUnit?: string
  currentPlantedAt?: string
  currentExpectedHarvestAt?: string
  currentActualHarvestAt?: string
  currentGrowthStageId?: number
  currentGrowthStageName?: string
  currentGrowthStageOrder?: number
  currentBatchStatus?: number
  currentExpectedYieldAmount?: number
  currentGrownDays?: number
  currentYieldAmount?: number
  currentYieldUnit?: string
  currentBatchRemark?: string
}

export interface PlotPayload {
  userId: number
  farmId: number
  plotName: string
  cropImage?: string
  type?: number
  area?: number
  areaUnit?: string
  address?: string
  coordinate?: string
  /** 空数组表示主动清除已有边界。 */
  boundaryPoints?: PlotBoundaryPoint[]
  status?: number
  remark?: string
  currentBatchId?: number
  currentCropId?: number
  currentPlantedAt?: string
  currentExpectedHarvestAt?: string
  currentGrowthStageId?: number
  currentBatchStatus?: number
  currentExpectedYieldAmount?: number
  currentGrownDays?: number
  currentYieldUnit?: string
}

export interface UpdatePlotPayload extends Partial<PlotPayload> {
  id: number
}

export interface PlotListParams {
  plotName?: string
  plotCode?: string
  farmId?: number
  userId?: number
  type?: number
  status?: number
  pageNum: number
  pageSize: number
}

export interface PlotStatistics {
  plotTotalCount: number
  idlePlotCount: number
  cropTypeCount: number
  plantingArea: number
  areaUnit: string
  plotTotalTrend?: number
  idlePlotTrend?: number
  cropTypeTrend?: number
  plantingAreaTrend?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PlotAddressResolveResult {
  coordinate: string
  address: string
}

export interface PlotCropImageUploadResult {
  imageUrl: string
  imageSize: number
}

export interface PlotHarvestPayload {
  harvester: string
  yieldAmount: number
  yieldUnit: string
  actualHarvestAt: string
}

/** 分页查询地块列表，对应 smart_plant: GET /plot/list。 */
export const listPlots = (params: PlotListParams) => {
  return get<ApiResult<PageInfo<Plot>>>('/plot/list', params as unknown as Record<string, unknown>)
}

/** 查询当前账号数据权限范围内的地块经营统计。 */
export const getPlotStatistics = () => {
  return get<ApiResult<PlotStatistics>>('/plot/statistics')
}

/** 根据地块 ID 查询详情，对应 smart_plant: GET /plot/{id}。 */
export const getPlotById = (id: number) => {
  return get<ApiResult<Plot>>(`/plot/${id}`)
}

/** 新增地块，对应 smart_plant: POST /plot/add。 */
export const addPlot = (data: PlotPayload) => {
  return post<ApiResult<Plot>>('/plot/add', data as unknown as Record<string, unknown>)
}

/** 上传地块作物图片，文件保存到后端 plot-images 目录。 */
export const uploadPlotCropImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  return http<ApiResult<PlotCropImageUploadResult>>({
    url: '/plot/upload-crop-image',
    method: 'post',
    data: formData,
  })
}

/** 编辑地块，对应 smart_plant: PUT /plot。 */
export const updatePlot = (data: UpdatePlotPayload) => {
  return http<ApiResult<Plot>>({ url: '/plot', method: 'put', data })
}

/** 删除单个地块，对应 smart_plant: DELETE /plot/{id}。 */
export const deletePlot = (id: number) => {
  return http<ApiResult<void>>({ url: `/plot/${id}`, method: 'delete' })
}

/** 批量删除地块，对应 smart_plant: DELETE /plot/batch。 */
export const batchDeletePlots = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/plot/batch', method: 'delete', data: ids })
}

/** 启用或停用地块，对应 smart_plant: PUT /plot/{id}/status。 */
export const updatePlotStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/plot/${id}/status`,
    method: 'put',
    params: { status },
  })
}

/** 完成地块当前种植批次的采收，并使地块恢复空闲。 */
export const harvestPlot = (id: number, data: PlotHarvestPayload) => {
  return http<ApiResult<unknown>>({ url: `/plot/${id}/harvest`, method: 'put', data })
}

/** 根据地块经纬度反向解析地址，复用后端统一的位置解析能力。 */
export const resolvePlotAddress = (coordinate: string, coordinateSystem = 'wgs84') => {
  return get<ApiResult<PlotAddressResolveResult>>('/plot/location/address', {
    coordinate,
    coordinateSystem,
  })
}
