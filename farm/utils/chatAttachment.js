/** 拍照和相册共用 uni.chooseImage；H5 相机入口由浏览器能力决定。 */
function chooseImage(sourceType) {
  return new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sourceType: [sourceType],
      success: result => resolve({ path: result.tempFilePaths?.[0], name: result.tempFiles?.[0]?.name || '[图片]' }),
      fail: () => resolve(null),
    })
  })
}

/** App 系统文件选择后复制到应用沙箱，uni.uploadFile 才能稳定读取文件。 */
function chooseAppFile() {
  return new Promise((resolve) => {
    if (typeof plus.io.chooseFile !== 'function') {
      uni.showToast({ title: '当前运行基座不支持文件选择', icon: 'none' })
      resolve(null)
      return
    }
    plus.io.chooseFile({ title: '选择聊天文件', multiple: false }, (result) => {
      const candidate = result.files?.[0] || result.tempFiles?.[0]
      const selected = typeof candidate === 'string' ? candidate : candidate?.path
      if (!selected) { resolve(null); return }
      plus.io.resolveLocalFileSystemURL(selected, (entry) => {
        const originalName = (entry.name || String(selected).split('/').pop() || '文件').replace(/[\\/]/g, '_')
        plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (filesystem) => {
          const copyName = `chat-${Date.now()}-${originalName}`
          entry.copyTo(filesystem.root, copyName, (copied) => resolve({
            path: copied.toLocalURL(),
            name: originalName,
            // 上传结束后清理沙箱副本，避免聊天附件长期占用手机空间。
            cleanup: () => copied.remove(() => {}, () => {}),
          }), () => { uni.showToast({ title: '文件读取失败', icon: 'none' }); resolve(null) })
        }, () => { uni.showToast({ title: '文件读取失败', icon: 'none' }); resolve(null) })
      }, () => { uni.showToast({ title: '文件读取失败', icon: 'none' }); resolve(null) })
    }, () => resolve(null))
  })
}

/** H5、微信小程序和 App 分别使用可用的文件选择入口，取消选择不发送消息。 */
function chooseFile() {
  return new Promise((resolve) => {
    // #ifdef H5
    uni.chooseFile({ count: 1, success: result => resolve({ path: result.tempFilePaths?.[0], name: result.tempFiles?.[0]?.name || '文件' }), fail: () => resolve(null) })
    // #endif
    // #ifdef MP-WEIXIN
    wx.chooseMessageFile({ count: 1, type: 'file', success: result => resolve({ path: result.tempFiles?.[0]?.path, name: result.tempFiles?.[0]?.name || '文件' }), fail: () => resolve(null) })
    // #endif
    // #ifdef APP-PLUS
    chooseAppFile().then(resolve)
    // #endif
  })
}

/** 两侧聊天页共用选择逻辑，拍照和相册发送图片，文件入口发送文档。 */
export function chooseChatAttachment(label) {
  if (label === '拍照') return chooseImage('camera')
  if (label === '图片') return chooseImage('album')
  if (label === '文件') return chooseFile()
  return Promise.resolve(null)
}
