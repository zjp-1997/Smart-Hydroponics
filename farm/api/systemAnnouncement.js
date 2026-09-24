import { get, request } from '@/utils/request.js'

// 公告接口由后端登录态确定接收用户，前端不传 userId。
const base = '/smart_plant/client/system-announcements'

// 将通知表的 isRead 转成维护消息页面使用的 readCount，统一列表样式与交互。
function normalizePage(page) {
  const source = page && Array.isArray(page.list) ? page.list : []
  return {
    ...page,
    list: source.map(item => ({ ...item, readCount: Number(item.isRead) || 0 })),
    total: Number(page && page.total) || 0,
  }
}

/** 按发布时间倒序读取当前用户公告，每次加载 20 条。 */
export const listSystemAnnouncements = (pageNum = 1) =>
  get(`${base}/list`, { pageNum, pageSize: 20 }).then(normalizePage)

/** 获取全部未读公告数，供消息入口和列表标题使用。 */
export const getSystemAnnouncementStatistics = () => get(`${base}/statistics`)

/** 单条和批量已读均交由后端按当前用户范围更新。 */
export const markSystemAnnouncementRead = id => request({ url: `${base}/${id}/read`, method: 'PUT' })
export const markAllSystemAnnouncementsRead = () => request({ url: `${base}/mark-all-read`, method: 'PUT' })
