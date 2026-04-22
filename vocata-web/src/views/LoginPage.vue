<template>
  <div class="login-page" :class="isMobileDevice ? 'is-mobile' : 'is-desktop'">
    <!-- 左侧品牌区（桌面端） -->
    <div v-if="!isMobileDevice" class="login-page__brand">
      <div class="login-page__brand-inner">
        <div class="login-page__logo">
          <img src="@/assets/logo.svg" alt="VocaTa" class="login-page__logo-icon" />
          <span class="login-page__logo-text">VocaTa</span>
        </div>
        <h1 class="login-page__headline">把每一次对话<br />留在更舒服的空间里</h1>
        <p class="login-page__sub">登录后继续你最近的聊天，或者创建一个新的陪伴角色。</p>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-page__form-area">
      <div class="login-page__card">
        <!-- 移动端 Logo -->
        <div v-if="isMobileDevice" class="login-page__logo login-page__logo--mobile">
          <img src="@/assets/logo.svg" alt="VocaTa" class="login-page__logo-icon" />
          <span class="login-page__logo-text">VocaTa</span>
        </div>

        <!-- Tab 切换 -->
        <nav class="login-page__tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="login-page__tab"
            :class="{ 'is-active': activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </nav>

        <!-- 登录表单 -->
        <el-form v-if="isLoginTab" class="login-page__form">
          <el-form-item>
            <el-input v-model="loginForm.loginName" placeholder="用户名" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" size="large" />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.rememberMe" label="记住我" />
          </el-form-item>
          <button
            type="button"
            class="login-page__submit"
            v-loading.fullscreen.lock="fullscreenLoading"
            element-loading-text="请稍后..."
            @click="handleLogin"
          >
            登录
          </button>
        </el-form>

        <!-- 注册表单 -->
        <el-form v-if="isRegisterTab" class="login-page__form">
          <el-form-item>
            <el-input v-model="registerForm.nickname" placeholder="用户名" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="registerForm.email" placeholder="邮箱" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="registerForm.password" type="password" placeholder="密码" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="registerForm.verificationCode" placeholder="验证码" size="large">
              <template #append>
                <el-button @click="handleSendCode" :disabled="isCodeButtonDisabled">{{ codeButtonText }}</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="registerForm.hasRead" label="我已阅读并同意用户协议和隐私政策" />
          </el-form-item>
          <button type="button" class="login-page__submit" @click="handleRegister">注册</button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'
import type { LoginParams, RegisterParams } from '@/types/api'
import { isMobile } from '@/utils/isMobile'
import { setToken } from '@/utils/token'

// 常量定义
const CODE_TIMEOUT = 60
const tabs = [
  { value: 'login', label: '登录' },
  { value: 'register', label: '注册' },
] as const

// 响应式数据
const isMobileDevice = computed(() => isMobile())
const layoutClass = computed(() => (isMobileDevice.value ? 'mobile' : 'pc'))
const activeTab = ref<'login' | 'register'>('login')

const loginForm = ref<LoginParams>({
  loginName: '',
  password: '',
  rememberMe: false,
})

const registerForm = ref<RegisterParams>({
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  verificationCode: '',
  gender: 0,
  hasRead: false,
})

const codeButtonText = ref('发送')
const isCodeButtonDisabled = ref(false)
const fullscreenLoading = ref(false)
const countdownTimer = ref<ReturnType<typeof setInterval>>()

// 计算属性
const isLoginTab = computed(() => activeTab.value === 'login')
const isRegisterTab = computed(() => activeTab.value === 'register')
const isPasswordMatch = computed(
  () => registerForm.value.password === registerForm.value.confirmPassword,
)

// 路由
const router = useRouter()

// 清理定时器
onUnmounted(() => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
})

// 登录相关方法

// 登录表单确认
const validateLoginForm = (): boolean => {
  if (!loginForm.value.loginName.trim()) {
    ElMessage.error('请输入用户名')
    return false
  }
  if (!loginForm.value.password.trim()) {
    ElMessage.error('请输入密码')
    return false
  }
  return true
}

// 登录
const handleLogin = async (): Promise<void> => {
  if (!validateLoginForm()) return

  try {
    fullscreenLoading.value = true
    const res = await userApi.login(loginForm.value)

    if (res.code === 200 && res.data) {
      setToken(res.data.token, res.data.expiresIn)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (error) {
    console.error('登录错误:', error)
    ElMessage.error('登录失败，请重试')
  } finally {
    fullscreenLoading.value = false
  }
}

// 注册相关方法

// 注册表单确认（发送验证码前）
const validateRegisterForm = (): boolean => {
  const { nickname, email, password, confirmPassword } = registerForm.value

  if (!nickname.trim()) {
    ElMessage.error('请输入用户名')
    return false
  }
  if (!email.trim()) {
    ElMessage.error('请输入邮箱')
    return false
  }
  if (!password.trim()) {
    ElMessage.error('请输入密码')
    return false
  }
  if (!confirmPassword.trim()) {
    ElMessage.error('请确认密码')
    return false
  }
  if (!isPasswordMatch.value) {
    ElMessage.error('两次密码不一致')
    return false
  }
  return true
}

// 发送验证码前验证
const validateBeforeSendCode = (): boolean => {
  if (!validateRegisterForm()) return false
  return true
}

// 发送验证码
const handleSendCode = async (): Promise<void> => {
  if (!validateBeforeSendCode()) return

  try {
    const res = await userApi.sendCode(registerForm.value.email)

    if (res.code === 200) {
      ElMessage.success('验证码发送成功')
      startCountdown()
    } else {
      ElMessage.error(res.message || '验证码发送失败')
    }
  } catch (error) {
    console.error('发送验证码错误:', error)
    ElMessage.error('验证码发送失败，请重试')
    resetCodeButton()
  }
}

// 发送验证码读秒
const startCountdown = (): void => {
  let timeout = CODE_TIMEOUT
  isCodeButtonDisabled.value = true

  countdownTimer.value = setInterval(() => {
    codeButtonText.value = `${timeout}s后重新发送`
    timeout -= 1

    if (timeout < 0) {
      resetCodeButton()
    }
  }, 1000)
}

// 重置验证码按钮
const resetCodeButton = (): void => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
  codeButtonText.value = '发送'
  isCodeButtonDisabled.value = false
}

// 注册表单确认（发送验证码后）
const handleRegister = async (): Promise<void> => {
  if (!registerForm.value.verificationCode.trim()) {
    ElMessage.error('请输入验证码')
    return
  }
  if (!registerForm.value.hasRead) {
    ElMessage.error('请阅读并同意用户协议和隐私政策')
    return
  }
  try {
    fullscreenLoading.value = true
    const res = await userApi.register(registerForm.value)

    if (res.code === 200 && res.data) {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error) {
    console.error('注册错误:', error)
    ElMessage.error('注册失败，请重试')
  } finally {
    fullscreenLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--vt-bg);
}

/* Brand panel (desktop left) */
.login-page__brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(135deg, var(--vt-brand-soft) 0%, var(--vt-bg) 100%);
  border-right: 1px solid var(--vt-line);
}

.login-page__brand-inner {
  max-width: 360px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.login-page__logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.login-page__logo--mobile {
  justify-content: center;
  margin-bottom: 8px;
}

.login-page__logo-icon {
  width: 32px;
  height: 32px;
}

.login-page__logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  background: linear-gradient(135deg, var(--vt-brand) 0%, oklch(65% 0.18 200) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-page__headline {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--vt-text);
  letter-spacing: -0.5px;
}

.login-page__sub {
  margin: 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--vt-text-soft);
}

/* Form area */
.login-page__form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
}

.login-page__card {
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Tabs */
.login-page__tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--vt-line);
}

.login-page__tab {
  flex: 1;
  padding: 10px 0;
  border: 0;
  background: transparent;
  color: var(--vt-text-muted);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.15s, border-color 0.15s;

  &:hover { color: var(--vt-text); }

  &.is-active {
    color: var(--vt-text);
    border-bottom-color: var(--vt-brand);
  }
}

/* Form */
.login-page__form {
  display: flex;
  flex-direction: column;
  gap: 4px;

  :deep(.el-form-item) { margin-bottom: 12px; }
}

.login-page__submit {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  border: 0;
  border-radius: var(--vt-radius-md);
  background: var(--vt-brand);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;

  &:hover { background: var(--vt-brand-strong); }
  &:active { transform: scale(0.99); }
}

/* Mobile */
.login-page.is-mobile {
  flex-direction: column;

  .login-page__form-area {
    padding: 32px 20px;
    align-items: flex-start;
    padding-top: 48px;
  }

  .login-page__card {
    max-width: 100%;
  }
}
</style>
