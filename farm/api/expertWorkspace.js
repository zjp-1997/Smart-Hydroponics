import { get, post, put, uploadFile } from '@/utils/request.js'

// 专家接口不接受专家 ID；后端从登录态关联 expert_profile.user_id。
const base = '/smart_plant/client/expert-workspace'

/** 读取身份认证状态、本人会话、历史消息和发送文字回复。 */
export const getExpertProfile = () => get(`${base}/profile`)
export const saveExpertCertification = data => put(`${base}/profile`, data)
export const submitExpertCertification = () => post(`${base}/profile/submit`)
export const uploadExpertCertificate = filePath =>
  uploadFile({ url: `${base}/profile/certificate`, filePath, name: 'file' })
export const getExpertSessions = () => get(`${base}/sessions`)
export const getExpertSessionDetail = (id, { beforeId, afterId, pageSize = 30 } = {}) =>
  get(`${base}/sessions/${encodeURIComponent(id)}`, {
    ...(beforeId ? { beforeId } : {}), ...(afterId ? { afterId } : {}), pageSize
  })
export const replyExpertSession = (id, content, messageType = 1, mediaUrl) =>
  post(`${base}/sessions/${encodeURIComponent(id)}/reply`, { content, messageType, mediaUrl })

/** 专家附件上传使用专家工作台路径，避免开放咨询发起人的上传接口。 */
export const uploadExpertReplyAttachment = (filePath, kind, displayName) =>
  uploadFile({ url: `${base}/attachments`, filePath, formData: { kind, displayName } })
