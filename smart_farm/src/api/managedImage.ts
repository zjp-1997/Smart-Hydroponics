import { http } from '@/utils/request'

export type ManagedImageCategory =
  | 'crop'
  | 'disease'
  | 'user-avatar'
  | 'oauth-avatar'
  | 'planting-batch'
  // 仓库物资图片由后端保存到独立目录，并将持久化地址写入 imageUrl。
  | 'warehouse'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface ManagedImageUploadResult {
  imageUrl: string
  imageSize: number
}

/** Uploads a management image and returns a durable backend URL for database persistence. */
export const uploadManagedImage = (file: File, category: ManagedImageCategory) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)

  return http<ApiResult<ManagedImageUploadResult>>({
    // 仓库使用仓库控制器内的稳定上传入口，其他业务继续复用公共上传接口。
    url: category === 'warehouse' ? '/warehouse/item/image/upload' : '/managed-image/upload',
    method: 'post',
    data: formData,
  })
}
