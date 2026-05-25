<script lang="ts" setup>
import { GithubOutlined } from '@ant-design/icons-vue'
import { userLogin, userRegister } from '@/api/userController'
import router from '@/router'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'

const route = useRoute()

const loginFormState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const registerFormState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const loading = ref(false)
const isRegisterMode = ref(false)

const toggleMode = () => {
  isRegisterMode.value = !isRegisterMode.value
}

const handleLogin = async (values: API.UserLoginRequest) => {
  loading.value = true
  try {
    const { data } = await userLogin(values)
    if (data.code === 20000 && data.data) {
      message.success('登录成功')
      setTimeout(() => {
        router.push('/')
      }, 900)
      return
    }
    message.error(`登录失败: ${data.message}`)
  } finally {
    loading.value = false
  }
}

const handleRegister = async (values: API.UserRegisterRequest) => {
  loading.value = true
  try {
    const { data } = await userRegister(values)
    if (data.code === 20000 && data.data) {
      message.success('注册成功，请登录')
      isRegisterMode.value = false
      return
    }
    message.error(`注册失败: ${data.message}`)
  } catch (error) {
    console.error('注册失败:', error)
    message.error('注册失败')
  } finally {
    loading.value = false
  }
}

const githubLoading = ref(false)

const handleGithubLogin = () => {
  githubLoading.value = true
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  window.location.href = `${baseUrl}/user/oauth/github/authorize`
}

onMounted(() => {
  const errorMsg = route.query.error
  if (errorMsg && typeof errorMsg === 'string') {
    message.error(`GitHub 登录失败: ${decodeURIComponent(errorMsg)}`)
    router.replace({ query: {} })
  }
})
</script>

<template>
  <div id="userLoginView">
    <div class="grain-layer"></div>
    <div class="auth-layout">
      <aside class="intro-panel">
        <p class="kicker">Creative Site Builder</p>
        <h1 class="intro-title">让灵感今天上线</h1>
        <p class="intro-description">
          通过简洁、可编辑、可持续迭代的流程，从想法到页面只走一条最短路径。
        </p>
        <ul class="intro-points">
          <li>一句话生成基础页面结构</li>
          <li>可视化修改细节并实时预览</li>
          <li>零代码发布到可访问地址</li>
        </ul>
      </aside>

      <main class="form-panel">
        <div class="auth-card">
          <div class="mode-switch">
            <button
              type="button"
              class="mode-chip"
              :class="{ active: !isRegisterMode }"
              @click="isRegisterMode = false"
            >
              登录
            </button>
            <button
              type="button"
              class="mode-chip"
              :class="{ active: isRegisterMode }"
              @click="isRegisterMode = true"
            >
              注册
            </button>
          </div>

          <header class="form-head">
            <h2 class="form-title">{{ isRegisterMode ? '创建新账户' : '欢迎回来' }}</h2>
            <p class="form-copy">
              {{
                isRegisterMode
                  ? '注册后即可保存项目、继续编辑并发布。'
                  : '登录后继续你上次的创作进度。'
              }}
            </p>
          </header>

          <a-form
            v-if="!isRegisterMode"
            :model="loginFormState"
            name="loginForm"
            class="auth-form"
            layout="vertical"
            autocomplete="off"
            @finish="handleLogin"
          >
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                v-model:value="loginFormState.userAccount"
                placeholder="账号"
                size="large"
                autocomplete="username"
              />
            </a-form-item>
            <a-form-item
              name="userPassword"
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码长度不能少于8位' },
              ]"
            >
              <a-input-password
                v-model:value="loginFormState.userPassword"
                placeholder="密码"
                size="large"
                autocomplete="current-password"
              />
            </a-form-item>
            <a-form-item class="oauth-section">
              <div class="oauth-divider">
                <span class="oauth-divider-text">或</span>
              </div>
              <a-button
                class="github-login-btn"
                size="large"
                block
                :loading="githubLoading"
                @click="handleGithubLogin"
              >
                <template #icon><GithubOutlined /></template>
              </a-button>
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loading"
                class="submit-button"
                block
              >
                {{ loading ? '登录中...' : '立即登录' }}
              </a-button>
            </a-form-item>
          </a-form>

          <a-form
            v-else
            :model="registerFormState"
            name="registerForm"
            class="auth-form"
            layout="vertical"
            autocomplete="off"
            @finish="handleRegister"
          >
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                v-model:value="registerFormState.userAccount"
                placeholder="账号"
                size="large"
                autocomplete="username"
              />
            </a-form-item>
            <a-form-item
              name="userPassword"
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码长度不能少于8位' },
              ]"
            >
              <a-input-password
                v-model:value="registerFormState.userPassword"
                placeholder="密码"
                size="large"
                autocomplete="new-password"
              />
            </a-form-item>
            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                {
                  validator: async (_rule: any, value: string) => {
                    if (value && value !== registerFormState.userPassword) {
                      throw new Error('两次输入的密码不一致')
                    }
                  },
                },
              ]"
            >
              <a-input-password
                v-model:value="registerFormState.checkPassword"
                placeholder="确认密码"
                size="large"
                autocomplete="new-password"
              />
            </a-form-item>
            <a-form-item class="oauth-section">
              <div class="oauth-divider">
                <span class="oauth-divider-text">或</span>
              </div>
              <a-button
                class="github-login-btn"
                size="large"
                block
                :loading="githubLoading"
                @click="handleGithubLogin"
              >
                <template #icon><GithubOutlined /></template>
              </a-button>
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loading"
                class="submit-button"
                block
              >
                {{ loading ? '注册中...' : '创建账户' }}
              </a-button>
            </a-form-item>
          </a-form>

          <p class="form-footnote">
            {{ isRegisterMode ? '已经有账号？' : '还没有账号？' }}
            <button type="button" class="text-link" @click="toggleMode">
              {{ isRegisterMode ? '返回登录' : '去注册' }}
            </button>
          </p>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
#userLoginView {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: clamp(var(--space-6), 3vw, var(--space-12));
  font-family: var(--auth-font-body);
  background:
    radial-gradient(
      circle at 15% 15%,
      color-mix(in oklch, var(--auth-bg-secondary) 74%, transparent) 0%,
      transparent 42%
    ),
    radial-gradient(
      circle at 80% 8%,
      color-mix(in oklch, var(--auth-accent-soft) 88%, transparent) 0%,
      transparent 32%
    ),
    linear-gradient(155deg, var(--auth-bg-main) 0%, oklch(0.95 0.02 78) 100%);
  overflow: hidden;
}

.grain-layer {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 1px 1px, oklch(0.5 0.03 60 / 0.09) 1px, transparent 0);
  background-size: 22px 22px;
  opacity: 0.3;
  pointer-events: none;
}

.auth-layout {
  position: relative;
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(340px, 1fr) minmax(380px, 480px);
  min-height: 100%;
  border: 1px solid var(--auth-line);
  border-radius: 32px;
  overflow: hidden;
  box-shadow: var(--auth-shadow-panel);
  background: color-mix(in oklch, var(--auth-surface) 94%, transparent);
}

.intro-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-6);
  padding: clamp(var(--space-8), 5vw, 72px);
  background:
    linear-gradient(165deg, oklch(0.95 0.04 84) 0%, oklch(0.92 0.05 55) 100%),
    var(--auth-surface-strong);
}

.kicker {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--auth-text-soft);
}

.intro-title {
  margin: 0;
  max-width: 10ch;
  font-family: var(--auth-font-display);
  font-size: clamp(2.1rem, 4.2vw, 4rem);
  line-height: 1.03;
  letter-spacing: -0.02em;
  color: var(--auth-text-primary);
}

.intro-description {
  margin: 0;
  max-width: 34ch;
  color: var(--auth-text-secondary);
  font-size: 1rem;
  line-height: 1.72;
}

.intro-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--space-3);
}

.intro-points li {
  position: relative;
  padding-inline-start: 26px;
  font-size: 0.96rem;
  line-height: 1.65;
  color: var(--auth-text-secondary);
}

.intro-points li::before {
  content: '';
  position: absolute;
  top: 0.58em;
  left: 0;
  width: 14px;
  height: 8px;
  border-radius: 10px;
  background: var(--auth-accent);
  transform: rotate(-13deg);
}

.form-panel {
  display: grid;
  place-items: center;
  padding: clamp(var(--space-6), 4vw, var(--space-12));
}

.auth-card {
  width: min(100%, 420px);
  display: grid;
  gap: var(--space-6);
}

.mode-switch {
  width: fit-content;
  display: inline-grid;
  grid-template-columns: repeat(2, minmax(84px, 1fr));
  gap: var(--space-1);
  padding: var(--space-1);
  border: 1px solid var(--auth-line);
  border-radius: 999px;
  background: oklch(0.98 0.01 70);
}

.mode-chip {
  border: 0;
  border-radius: 999px;
  height: 36px;
  padding-inline: var(--space-4);
  background: transparent;
  color: var(--auth-text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
  font-family: var(--auth-font-body);
  cursor: pointer;
  transition: background-color 240ms ease, color 240ms ease, transform 240ms ease;
}

.mode-chip.active {
  background: var(--auth-accent-soft);
  color: var(--auth-text-primary);
}

.mode-chip:focus-visible {
  outline: 2px solid var(--auth-focus);
  outline-offset: 2px;
}

.form-head {
  display: grid;
  gap: var(--space-2);
}

.form-title {
  margin: 0;
  font-family: var(--auth-font-display);
  font-size: clamp(1.9rem, 2.8vw, 2.6rem);
  line-height: 1.1;
  color: var(--auth-text-primary);
  letter-spacing: -0.01em;
}

.form-copy {
  margin: 0;
  max-width: 30ch;
  color: var(--auth-text-secondary);
  font-size: 0.95rem;
  line-height: 1.66;
}

.auth-form {
  display: grid;
  gap: var(--space-2);
}

.auth-form :deep(.ant-form-item) {
  margin-bottom: var(--space-3);
}

.auth-form :deep(.ant-input-affix-wrapper),
.auth-form :deep(.ant-input) {
  border-color: var(--auth-line);
  border-radius: 12px;
  height: 48px;
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--auth-text-primary);
  background: oklch(0.99 0.008 65);
  box-shadow: none;
  font-family: var(--auth-font-body);
}

.auth-form :deep(.ant-input) {
  padding-block: 0;
}

.auth-form :deep(.ant-input-affix-wrapper) {
  padding-block: 0;
  padding-inline: 12px;
}

.auth-form :deep(.ant-input-affix-wrapper .ant-input) {
  height: 100%;
  padding-inline: 0;
  background: transparent;
}

.auth-form :deep(.ant-input-affix-wrapper:hover),
.auth-form :deep(.ant-input:hover) {
  border-color: color-mix(in oklch, var(--auth-line) 58%, var(--auth-accent));
}

.auth-form :deep(.ant-input-affix-wrapper-focused),
.auth-form :deep(.ant-input:focus) {
  border-color: var(--auth-accent);
  box-shadow: 0 0 0 3px oklch(0.88 0.04 38 / 0.9);
}

.submit-button {
  height: 48px;
  border: 0;
  border-radius: 14px;
  font-size: 0.96rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  background: var(--auth-accent);
  box-shadow: var(--auth-shadow-button);
  transition: background-color 220ms ease, transform 220ms ease, box-shadow 220ms ease;
}

.submit-button:hover,
.submit-button:focus-visible {
  background: var(--auth-accent-hover);
  transform: translateY(-1px);
  box-shadow: var(--auth-shadow-button-hover);
}

.submit-button:active {
  transform: translateY(0);
}

.oauth-section {
  display: grid;
  gap: var(--space-3);
}

.oauth-section :deep(.ant-form-item-control-input-content) {
  display: grid;
  gap: var(--space-3);
}

.oauth-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.oauth-divider::before,
.oauth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--auth-line);
}

.oauth-divider-text {
  margin: 0 var(--space-3);
  color: var(--auth-text-soft);
  font-size: 0.84rem;
  white-space: nowrap;
}

.github-login-btn {
  height: 48px;
  border-radius: 12px;
  border: 1px solid color-mix(in oklch, var(--auth-line) 78%, transparent);
  background: oklch(0.14 0.02 260);
  color: oklch(0.96 0.01 260);
  font-size: 1.3rem;
  transition: background-color 200ms ease, transform 200ms ease;
}

.github-login-btn:hover,
.github-login-btn:focus-visible {
  background: oklch(0.18 0.02 260) !important;
  color: oklch(0.99 0.01 260) !important;
  border-color: oklch(0.18 0.02 260) !important;
  transform: translateY(-1px);
}

.github-login-btn:active {
  transform: translateY(0);
}

.form-footnote {
  margin: 0;
  color: var(--auth-text-soft);
  font-size: 0.86rem;
}

.text-link {
  border: 0;
  padding: 0;
  margin-inline-start: var(--space-2);
  color: var(--auth-text-primary);
  font-size: 0.88rem;
  font-weight: 700;
  background: transparent;
  cursor: pointer;
}

.text-link:focus-visible {
  outline: 2px solid var(--auth-focus);
  outline-offset: 2px;
  border-radius: 4px;
}

@media (max-width: 940px) {
  #userLoginView {
    padding: var(--space-3);
  }

  .auth-layout {
    grid-template-columns: 1fr;
    min-height: 100%;
  }

  .intro-panel {
    gap: var(--space-3);
    padding: 20px;
  }

  .intro-title {
    max-width: 14ch;
    font-size: clamp(1.7rem, 7vw, 2.5rem);
  }

  .form-panel {
    padding: var(--space-4);
  }

  .form-copy {
    max-width: 36ch;
  }
}

@media (max-width: 576px) {
  .auth-card {
    gap: var(--space-4);
  }

  .intro-description,
  .intro-points {
    display: none;
  }

  .intro-panel {
    padding: var(--space-4);
  }

  .form-title {
    font-size: 1.85rem;
  }

  .auth-form :deep(.ant-form-item) {
    margin-bottom: var(--space-2);
  }

  .form-panel {
    padding: var(--space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
