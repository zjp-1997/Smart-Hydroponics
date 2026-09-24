const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const page = fs.readFileSync(path.join(__dirname, '../pages/register/index.vue'), 'utf8')

// 注册页必须明确表达“申请”，不能暗示注册后已经获得农场权限。
assert.match(page, /申请加入的农场主/)
assert.match(page, /提交后需农场主或管理员审核，通过后才可登录/)
assert.match(page, /入场申请已提交/)
assert.match(page, /审核通过后才可登录并访问农场/)
