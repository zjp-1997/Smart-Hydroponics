const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

// 检查 H5 后端地址跟随页面主机，防止局域网 IP 变化后登录再次请求不可达的旧地址。
const requestSource = fs.readFileSync(path.resolve(__dirname, '../utils/request.js'), 'utf8')

assert.match(requestSource, /window\.location\.hostname/)
assert.match(requestSource, /return `http:\/\/\$\{window\.location\.hostname\}:8080`/)
assert.match(requestSource, /uni\.getStorageSync\('farm_api_base_url'\)/)

