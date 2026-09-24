/**
 * 将同一时刻的重复异步操作合并为一个 Promise。
 * 令牌刷新和用户信息同步使用该工具，避免并发请求重复访问后端。
 */
export const createSingleFlight = <T>(operation: () => Promise<T>) => {
  let activeTask: Promise<T> | null = null

  return () => {
    if (activeTask) {
      return activeTask
    }

    activeTask = operation().finally(() => {
      activeTask = null
    })
    return activeTask
  }
}
