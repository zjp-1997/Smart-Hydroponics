import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    vueJsx(),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css', directives: true })],
      // Element Plus 2.14 的泛型表格声明会让 vue-tsc 将插槽行误判为 DefaultRow。
      // 运行时仍按需导入组件，关闭声明生成以避免不稳定的第三方模板类型污染。
      dts: false,
    }),
    ...(command === 'serve' ? [vueDevTools()] : []),
  ],
  server: {
    host: '0.0.0.0',
    // 管理端开发服务固定使用 5175，便于前后端联调时保持访问地址稳定。
    port: 5175,
    strictPort: true,
  },
  preview: {
    host: '0.0.0.0',
    port: 4174,
    strictPort: true,
  },
  build: {
    // Vite 8/Rolldown 的显式依赖分组：框架、图表和网络库独立缓存。
    // Element Plus 保持按路由自然拆分，避免把异步页面的 UI 模块聚合进首屏预加载。
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'vendor-vue',
              test: /node_modules[\\/](vue|vue-router|pinia|@vue)[\\/]/,
              priority: 30,
            },
            {
              name: 'vendor-charts',
              test: /node_modules[\\/](chart\.js|@kurkle)[\\/]/,
              priority: 20,
            },
            {
              name: 'vendor-http',
              test: /node_modules[\\/]axios[\\/]/,
              priority: 20,
            },
          ],
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
}))
