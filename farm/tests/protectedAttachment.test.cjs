const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const request = fs.readFileSync(path.join(__dirname, '../utils/request.js'), 'utf8')
const messageBody = fs.readFileSync(path.join(__dirname, '../componment/ChatMessageBody.vue'), 'utf8')

// 私有聊天图片和文件必须经过携带 Bearer Token 的受控下载，不能直接公开静态 URL。
assert.match(request, /export function downloadProtectedFile/)
assert.match(request, /uni\.downloadFile\([\s\S]*?Authorization:\s*`Bearer \$\{token\}`/)
assert.match(messageBody, /downloadProtectedFile\(url\)/)
assert.match(messageBody, /downloadProtectedFile\(this\.url\)/)
assert.doesNotMatch(messageBody, /window\.open\(this\.url/)
