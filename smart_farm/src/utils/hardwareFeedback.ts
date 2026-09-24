export type HardwareFeedbackType = 'success' | 'warning' | 'info' | 'error'

export interface HardwareFeedback {
  text: string
  type: HardwareFeedbackType
}

/**
 * 将后端硬件应用状态转换为统一反馈，避免把“配置写入数据库”误报为“设备执行成功”。
 * 0待下发、1下发中、2设备确认、3执行失败，与设备计划接口约定保持一致。
 */
export const getHardwareApplyFeedback = (status?: number, failureReason?: string): HardwareFeedback => {
  if (status === 1) return { text: '指令下发中', type: 'info' }
  if (status === 2) return { text: '设备已确认', type: 'success' }
  if (status === 3) {
    return {
      text: failureReason ? `执行失败：${failureReason}` : '执行失败',
      type: 'error',
    }
  }
  return { text: '指令待下发', type: 'warning' }
}

/** 保存配置后的提示同时说明持久化结果和真实硬件状态。 */
export const getConfigurationSaveFeedback = (status?: number, failureReason?: string): HardwareFeedback => {
  const hardware = getHardwareApplyFeedback(status, failureReason)
  return { ...hardware, text: `配置已保存，${hardware.text}` }
}
