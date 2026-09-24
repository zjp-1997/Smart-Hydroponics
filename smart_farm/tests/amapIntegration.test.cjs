const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

// 读取环境变量但不输出凭证，验证开发与生产环境使用同一套高德配置项。
const readEnv = (file) => Object.fromEntries(
  readFileSync(file, 'utf8')
    .split(/\r?\n/)
    .filter((line) => line && !line.trimStart().startsWith('#'))
    .map((line) => {
      const separator = line.indexOf('=')
      return [line.slice(0, separator), line.slice(separator + 1).replace(/^['"]|['"]$/g, '')]
    }),
)

const development = readEnv('.env.development')
const production = readEnv('.env.production')
assert.ok(development.VITE_AMAP_KEY)
assert.ok(development.VITE_AMAP_SECURITY_JS_CODE)
assert.equal(development.VITE_AMAP_KEY, production.VITE_AMAP_KEY)
assert.equal(development.VITE_AMAP_SECURITY_JS_CODE, production.VITE_AMAP_SECURITY_JS_CODE)

// 验证三个原有入口均已使用公共高德能力，避免回退到浏览器定位或无 Key 瓦片。
const amap = readFileSync('src/utils/amap.ts', 'utf8')
const mapOverview = readFileSync('src/views/map/MapOverview.vue', 'utf8')
const farmForm = readFileSync('src/views/farm/AddOrUpdate.vue', 'utf8')
const plotForm = readFileSync('src/views/plot/AddOrUpdate.vue', 'utf8')
assert.match(amap, /AMap\.Geolocation[\s\S]*AMap\.Geocoder/)
assert.match(mapOverview, /loadAmap[\s\S]*new AMapApi\.Map/)
assert.doesNotMatch(mapOverview, /leaflet|navigator\.geolocation/i)
assert.match(farmForm, /locateWithAmap/)
assert.match(plotForm, /locateWithAmap/)
