import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChatView from '../views/ChatView.vue'
import EditView from '../views/EditView.vue'
import UserLoginReisterView from '@/views/user/UserLoginReisterView.vue'
import UserProfileView from '@/views/user/UserProfileView.vue'
import { useLoginUserStore } from '@/stores/LoginUserStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/app/:appId/chat',
      name: 'chat',
      component: ChatView,
      meta: { requiresAuth: true, layoutScrollable: false },
    },
    {
      path: '/app/:appId/edit',
      name: 'edit',
      component: EditView,
      meta: { requiresAuth: true },
    },
    {
      path: '/user/login',
      name: 'userLogin',
      component: UserLoginReisterView,
      meta: { requiresAuth: false },
    },
    {
      path: '/user/profile',
      name: 'userProfile',
      component: UserProfileView,
      meta: { requiresAuth: true },
    },
  ],
})

// 导航守卫：检查登录状态
router.beforeEach(async (to, _from, next) => {
  // 不需要认证的页面直接放行
  if (!to.meta.requiresAuth) {
    next()
    return
  }

  const loginUserStore = useLoginUserStore()

  // 如果尚未获取登录用户信息，先尝试获取
  if (!loginUserStore.loginUser.id) {
    await loginUserStore.fetchLoginUser()
  }

  // 仍未登录则重定向到登录页
  if (!loginUserStore.loginUser.id) {
    next('/user/login')
    return
  }

  next()
})

export default router
