import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import '@/utils/rem.ts'
import '@/assets/styles/fonts.css'
import '@/assets/styles/theme.css'
import '@/assets/styles/element-overrides.scss'
import '@/assets/styles/pagination-theme.css'
import { useTheme } from '@/composables/useTheme'
import { zhCn } from 'element-plus/es/locales.mjs'

// 初始化主题（在 app 挂载前，避免闪烁）
useTheme().init()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
