const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const dialog = readFileSync('src/views/warehouse/AddOrUpdate.vue', 'utf8')
const list = readFileSync('src/views/warehouse/ListView.vue', 'utf8')
const api = readFileSync('src/api/managedImage.ts', 'utf8')

// 仓库弹框必须包含真实上传、图片预览、移除入口和上传期间的提交保护。
assert.match(dialog, /uploadManagedImage\(file, 'warehouse'\)/)
assert.match(dialog, /class="upload-image"/)
assert.match(dialog, /@click="removeImage"/)
assert.match(dialog, /:disabled="uploadLoading"/)

// 公共管理图片 API 的类型必须显式允许 warehouse，防止调用端绕过 TypeScript 检查。
assert.match(api, /\| 'warehouse'/)
assert.match(api, /category === 'warehouse' \? '\/warehouse\/item\/image\/upload'/)

// 表格中的后端上传地址必须拼接 API 基础地址，前端静态默认图则保持原路径。
assert.match(list, /url\.startsWith\('\/warehouse-images\/'\) \? url : getFileUrl\(url\)/)
assert.match(list, /imageUrl: resolveImageUrl\(item\.imageUrl, item\.category\)/)
