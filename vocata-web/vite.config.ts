import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 根据当前模式加载对应的环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined
            }
            if (id.includes('@element-plus/icons-vue')) {
              return 'element-plus-icons'
            }
            const elementPlusComponent = id.match(/node_modules\/element-plus\/(?:es|lib)\/components\/([^/]+)/)
            if (elementPlusComponent) {
              return `element-plus-${elementPlusComponent[1]}`
            }
            if (id.includes('node_modules/element-plus')) {
              return 'element-plus-core'
            }
            if (id.includes('vue-router')) {
              return 'vue-router'
            }
            if (id.includes('pinia')) {
              return 'pinia'
            }
            return 'vendor'
          },
        },
      },
    },
    server: {
      port: 3000,
      host: '0.0.0.0', // 允许外部访问
      strictPort: true, // 端口被占用时不自动尝试下一个端口
      proxy: {
        // 代理所有 /api 开头的请求到后端服务器
        '/api': {
          target: env.VITE_APP_URL,
          changeOrigin: true,
          secure: false,
        }
      }
    },
  }
})
