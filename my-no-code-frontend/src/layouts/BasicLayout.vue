<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <a-layout-header class="header">
      <GlobalHeader />
    </a-layout-header>

    <!-- 内容区域 -->
    <a-layout-content class="content">
      <div class="content-inner">
        <RouterView v-slot="{ Component }">
          <component
            :is="Component"
            class="route-page"
            :class="{ 'route-page--scrollable': isPageScrollable }"
          />
        </RouterView>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'

const route = useRoute()

const isPageScrollable = computed(() => {
  // 聊天页依赖自身的分栏滚动区域，其余页面默认由布局层承接纵向滚动。
  return route.meta.layoutScrollable !== false
})
</script>

<style scoped>
.basic-layout {
  height: 100dvh;
  background: var(--gradient-bg);
  position: relative;
  display: flex;
  flex-direction: column;
}

.basic-layout::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(
      circle at 18% 24%,
      color-mix(in oklch, var(--auth-bg-secondary) 18%, transparent) 0%,
      transparent 46%
    ),
    radial-gradient(
      circle at 82% 12%,
      color-mix(in oklch, var(--auth-accent-soft) 20%, transparent) 0%,
      transparent 34%
    ),
    radial-gradient(
      circle at 72% 72%,
      color-mix(in oklch, var(--auth-accent) 7%, transparent) 0%,
      transparent 42%
    );
  pointer-events: none;
  z-index: 0;
}

.header {
  height: var(--header-height);
  line-height: var(--header-height);
  background: transparent;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: var(--z-index-sticky);
  animation: slideDown 0.6s var(--ease-in-out);
}

.content {
  display: flex;
  flex: 1;
  padding: 0;
  min-height: 0;
  position: relative;
  z-index: 1;
  overflow: hidden;
  animation: fadeInUp 0.8s var(--ease-in-out) 0.2s backwards;
}

.content-inner {
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.route-page {
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.route-page--scrollable {
  height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .header {
    height: var(--header-height-mobile);
    line-height: var(--header-height-mobile);
  }

  .content {
    padding: 0;
    min-height: 0;
  }
}

@media (max-width: 576px) {
  .content {
    padding: 0;
  }
}
</style>
