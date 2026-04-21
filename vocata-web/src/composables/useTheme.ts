import { ref } from 'vue'

const isDark = ref(false)

function init() {
  const saved = localStorage.getItem('vt-theme')
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  isDark.value = saved ? saved === 'dark' : prefersDark
  apply()
}

function apply() {
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
}

export function useTheme() {
  const toggle = () => {
    isDark.value = !isDark.value
    apply()
    localStorage.setItem('vt-theme', isDark.value ? 'dark' : 'light')
  }

  return { isDark, toggle, init }
}
