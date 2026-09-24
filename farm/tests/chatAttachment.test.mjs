import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

// 用 H5 条件编译结果验证实际运行分支，避免 Node 直接执行微信和 App 专有 API。
let source = await readFile(new URL('../utils/chatAttachment.js', import.meta.url), 'utf8')
source = source.replace(/    \/\/ #ifdef (MP-WEIXIN|APP-PLUS)[\s\S]*?    \/\/ #endif\r?\n/g, '')
const { chooseChatAttachment } = await import(`data:text/javascript,${encodeURIComponent(source)}`)

const selectedSources = []
globalThis.uni = {
  chooseImage(options) {
    selectedSources.push(options.sourceType[0])
    options.success({ tempFilePaths: ['/tmp/photo.jpg'], tempFiles: [{ name: 'photo.jpg' }] })
  },
  chooseFile(options) {
    options.success({ tempFilePaths: ['/tmp/report.pdf'], tempFiles: [{ name: 'report.pdf' }] })
  },
}

assert.equal((await chooseChatAttachment('拍照')).path, '/tmp/photo.jpg')
assert.equal((await chooseChatAttachment('图片')).path, '/tmp/photo.jpg')
assert.deepEqual(selectedSources, ['camera', 'album'])
assert.deepEqual(await chooseChatAttachment('文件'), { path: '/tmp/report.pdf', name: 'report.pdf' })

// 模拟 App 的系统选择和沙箱复制，确保上传路径可读且能在发送后清理副本。
let appSource = await readFile(new URL('../utils/chatAttachment.js', import.meta.url), 'utf8')
appSource = appSource.replace(/    \/\/ #ifdef (H5|MP-WEIXIN)[\s\S]*?    \/\/ #endif\r?\n/g, '')
const { chooseChatAttachment: chooseInApp } = await import(`data:text/javascript,${encodeURIComponent(appSource)}`)
let removed = false
globalThis.plus = { io: {
  PRIVATE_DOC: 1,
  chooseFile: (_options, success) => success({ files: ['/download/report.pdf'] }),
  resolveLocalFileSystemURL: (_path, success) => success({
    name: 'report.pdf',
    copyTo: (_root, _name, copied) => copied({
      toLocalURL: () => '_doc/chat-report.pdf',
      remove: (done) => { removed = true; done() },
    }),
  }),
  requestFileSystem: (_type, success) => success({ root: {} }),
} }
const appFile = await chooseInApp('文件')
assert.equal(appFile.path, '_doc/chat-report.pdf')
assert.equal(appFile.name, 'report.pdf')
appFile.cleanup()
assert.equal(removed, true)
