const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const router = readFileSync('src/router/index.ts', 'utf8')
const menu = readFileSync('src/components/LeftMenu.vue', 'utf8')
const page = readFileSync('src/views/phonePicture/ListView.vue', 'utf8')
const cropImageApi = readFileSync('src/api/cropImage.ts', 'utf8')
const constants = readFileSync('src/utils/constants.ts', 'utf8')
const utils = readFileSync('src/utils/utils.ts', 'utf8')

assert.match(router, /path: '\/disease-image\/list'[\s\S]*?name: 'diseaseImageList'/)
assert.match(router, /diseaseImageList: 'disease_image:manage'/)
assert.match(menu, /病害图片管理[\s\S]*?disease_image:manage/)
assert.match(page, /diseaseOnly: true/)
assert.match(page, /label="病害名称"/)
assert.match(page, /图片标签/)
assert.match(page, /手机图片/)
assert.match(cropImageApi, /diseasePestId\?: number/)
assert.match(cropImageApi, /diseaseOnly\?: boolean/)

// loopback API 地址必须跟随当前页面主机，确保跨端访问时图片 Cookie 能发送。
assert.match(constants, /apiUrl\.hostname = window\.location\.hostname/)
assert.match(utils, /absoluteUrl\.hostname = window\.location\.hostname/)
