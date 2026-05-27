<template>
  <div class="global-header">
    <!-- 左侧 Logo 和标题 -->
    <div class="logo-section">
      <div class="logo-wrapper">
        <img class="logo" src="@/assets/logo.png" alt="易搭 Logo" />
      </div>
      <span class="title">易搭</span>
    </div>

    <!-- 中间菜单 -->
    <a-menu
      v-model:selectedKeys="selectedKeys"
      mode="horizontal"
      class="menu"
      :items="menuItems"
      @click="handleMenuClick"
    />

    <!-- 右侧用户区域 -->
    <div class="user-section">
      <!-- 已登录：显示用户名和退出按钮 -->
      <template v-if="loginUserStore.loginUser.id">
        <a-dropdown>
          <a-space class="user-info">
            <a-avatar
              v-if="loginUserStore.loginUser.userAvatar"
              :src="loginUserStore.loginUser.userAvatar"
              size="small"
            />
            <a-avatar v-else size="small" class="avatar-fallback">
              {{ loginUserStore.loginUser.username?.charAt(0) }}
            </a-avatar>
            <span class="username">{{ loginUserStore.loginUser.username }}</span>
          </a-space>
          <template #overlay>
            <a-menu @click="handleUserMenuClick">
              <a-menu-item key="quota" disabled class="quota-item">
                <div class="quota-label">应用创建次数</div>
                <template v-if="loginUserStore.loginUser.appMaxCount === -1">
                  <div class="quota-text">已创建 {{ loginUserStore.loginUser.appUsedCount ?? 0 }} 个（不限制）</div>
                </template>
                <template v-else>
                  <a-progress
                    :percent="quotaPercent"
                    :stroke-color="quotaStrokeColor"
                    :trail-color="'#f0f0f0'"
                    :size="6"
                    :show-info="false"
                    style="margin: 4px 0 0;"
                  />
                  <div class="quota-text">
                    {{ loginUserStore.loginUser.appUsedCount ?? 0 }} / {{ loginUserStore.loginUser.appMaxCount ?? 0 }}
                  </div>
                </template>
              </a-menu-item>
              <a-menu-item key="profile">个人中心</a-menu-item>
              <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
      <!-- 未登录：显示登录按钮 -->
      <template v-else>
        <a-button type="primary" class="login-btn" @click="router.push('/user/login')">
          <a-space class="btn-text">登录</a-space>
        </a-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/LoginUserStore'
import { userLogout } from '@/api/userController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 菜单配置
const menuItems = computed<MenuProps['items']>(() => {
  const items: MenuProps['items'] = [
    {
      key: '/',
      label: '首页',
    },
  ]

  if (loginUserStore.loginUser.userRole === 'admin') {
    items.push({
      key: '/admin/apps',
      label: '应用管理',
    })
    items.push({
      key: '/admin/users',
      label: '用户管理',
    })
  }

  return items
})

// 配额进度条
const quotaPercent = computed(() => {
  const used = loginUserStore.loginUser.appUsedCount ?? 0
  const max = loginUserStore.loginUser.appMaxCount ?? 1
  return max > 0 ? Math.round((used / max) * 100) : 0
})

const quotaStrokeColor = computed(() => {
  const pct = quotaPercent.value
  if (pct >= 100) return '#ff4d4f'
  if (pct >= 80) return '#faad14'
  return '#52c41a'
})

// 当前选中的菜单项
const selectedKeys = ref<string[]>([route.path])

// 监听路由变化，更新选中的菜单项
watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
  },
)

// 处理菜单点击事件
const handleMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}

const handleUserMenuClick = ({ key }: { key: string }) => {
  if (key === 'profile') {
    router.push('/user/profile')
  }
}

// 处理退出登录
const handleLogout = async () => {
  try {
    await userLogout()
    loginUserStore.clearLoginUser()
    message.success('已退出登录')
    router.push('/user/login')
  } catch (error) {
    console.error('[退出登录失败]', error)
    message.error('退出登录失败，请稍后重试')
  }
}
</script>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  height: var(--header-height);
  padding: 0 var(--spacing-3xl);
  background:
    linear-gradient(
      130deg,
      color-mix(in oklch, var(--auth-surface) 94%, transparent) 0%,
      color-mix(in oklch, var(--auth-bg-secondary) 22%, var(--auth-surface)) 100%
    ),
    color-mix(in oklch, var(--auth-surface) 82%, transparent);
  backdrop-filter: blur(16px) saturate(1.2);
  border-bottom: 1px solid color-mix(in oklch, var(--auth-line) 82%, transparent);
  box-shadow: 0 8px 22px oklch(0.44 0.03 46 / 0.08);
  transition: var(--transition-base);
}

.logo-section {
  display: flex;
  align-items: center;
  margin-right: var(--spacing-5xl);
  white-space: nowrap;
  animation: slideInLeft 0.6s var(--ease-in-out);
}

.logo-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 34px;
  height: 34px;
  margin-right: 12px;
  background: transparent;
  border: none;
  box-shadow: none;
}

.logo {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center;
  transform: scale(2.35);
  transform-origin: center;
}

.title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--auth-text-primary);
  letter-spacing: 0.5px;
  font-family: var(--auth-font-display);
}

.menu {
  flex: 1;
  border-bottom: none;
  line-height: var(--header-height);
  background: transparent;
  animation: fadeIn 0.8s var(--ease-in-out) 0.2s backwards;
}

.menu :deep(.ant-menu-item) {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--auth-text-secondary);
  border-radius: var(--border-radius-sm);
  margin: 0 var(--spacing-xs);
  transition:
    background-color 180ms ease,
    color 180ms ease;
  font-family: var(--auth-font-body);
}

.menu :deep(.ant-menu-item:hover) {
  color: var(--auth-text-primary);
  background: color-mix(in oklch, var(--auth-accent-soft) 56%, transparent);
}

.menu :deep(.ant-menu-item-selected) {
  color: var(--auth-text-primary);
  background: color-mix(in oklch, var(--auth-accent-soft) 64%, transparent);
  font-weight: var(--font-weight-semibold);
}

.menu :deep(.ant-menu-item-selected::after) {
  border-bottom-color: var(--auth-accent);
  border-bottom-width: 2px;
}

.user-section {
  margin-left: var(--spacing-2xl);
  animation: slideInRight 0.6s var(--ease-in-out);
}

.user-info {
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--border-radius-full);
  background: transparent;
  border: 1px solid transparent;
}

.username {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--auth-text-primary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-btn {
  height: 40px;
  padding: 0 28px;
  background: color-mix(in oklch, var(--auth-accent-soft) 72%, white);
  border: 1px solid color-mix(in oklch, var(--auth-accent) 24%, var(--auth-line));
  border-radius: var(--border-radius-full);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--auth-text-primary);
  box-shadow: none;
  transition:
    background-color 180ms ease,
    border-color 180ms ease,
    transform 180ms ease;
  font-family: var(--auth-font-body);
}

.login-btn:hover {
  background: color-mix(in oklch, var(--auth-accent-soft) 88%, white);
  border-color: color-mix(in oklch, var(--auth-accent) 34%, var(--auth-line));
  transform: translateY(-1px);
}

.btn-text {
  letter-spacing: 0.5px;
}

.avatar-fallback {
  background: color-mix(in oklch, var(--auth-accent-soft) 70%, white) !important;
  color: var(--auth-text-primary) !important;
}

.quota-item {
  cursor: default !important;
  padding: 8px 16px !important;
}

.quota-item:hover {
  background: transparent !important;
}

.quota-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}

.quota-text {
  font-size: 12px;
  color: #666;
  text-align: right;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .global-header {
    padding: 0 var(--spacing-xl);
    height: var(--header-height-mobile);
  }

  .logo-section {
    margin-right: var(--spacing-xl);
  }

  .logo-wrapper {
    width: 30px;
    height: 30px;
    margin-right: 10px;
  }

  .logo {
    transform: scale(2.35);
  }

  .title {
    font-size: var(--font-size-xl);
  }

  .menu {
    flex: 1;
    min-width: 0;
    line-height: var(--header-height-mobile);
  }

  .menu :deep(.ant-menu-item) {
    font-size: var(--font-size-base);
    padding: 0 var(--spacing-md);
  }

  .user-section {
    margin-left: var(--spacing-md);
  }

  .username {
    max-width: 88px;
  }

  .login-btn {
    height: 36px;
    padding: 0 var(--spacing-xl);
    font-size: var(--font-size-base);
  }
}

@media (max-width: 576px) {
  .global-header {
    padding: 0 var(--spacing-lg);
  }

  .title {
    display: none;
  }

  .logo-wrapper {
    margin-right: 0;
  }

  .username {
    display: none;
  }

  .menu :deep(.ant-menu-item) {
    font-size: var(--font-size-sm);
    padding: 0 var(--spacing-sm);
  }
}
</style>
