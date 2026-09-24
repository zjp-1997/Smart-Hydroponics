/// <reference types="vite/client" />
/// <reference types="amap-js-api" />
/// <reference types="amap-js-api-geocoder" />
/// <reference types="amap-js-api-geolocation" />

interface ImportMetaEnv {
  /** 高德 Web JS API 的浏览器端 Key。 */
  readonly VITE_AMAP_KEY: string
  /** 高德 Web JS API 2.0 配套的安全密钥。 */
  readonly VITE_AMAP_SECURITY_JS_CODE: string
}

interface ImportMeta {
  /** Vite 构建时注入的只读环境变量。 */
  readonly env: ImportMetaEnv
}
