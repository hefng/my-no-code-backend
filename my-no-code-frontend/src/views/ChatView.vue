<script setup lang="ts">
import { ref, onMounted, onUnmounted, h, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppVoById, deployedApp, updateMyApp } from '@/api/appController'
import { listLatestChatHistory, listMoreChatHistory } from '@/api/chatHistoryController'
import ChatMessage from '@/components/ChatMessage.vue'
import PreviewFrame from '@/components/PreviewFrame.vue'
import ChatInput from '@/components/ChatInput.vue'
import { createSSEConnection, type WorkflowStepPayload, type ToolEventPayload } from '@/utils/sse'
import { editProjectFile } from '@/api/projectController'
import { Modal } from 'ant-design-vue'
import { useAppStore } from '@/stores/AppStore'
import {
  type SelectedElementInfo,
  type ContentChangeInfo,
  exitVisualEditorInIframe,
  formatElementInfoForPrompt,
} from '@/utils/visualEditor'

/**
 * 根据生成类型构建预览 URL
 * vue-project 类型需访问构建产物 dist/index.html
 * useProxy=true 时走 /preview-proxy 同源代理，用于可视化编辑模式下的脚本注入
 */
const buildPreviewUrl = (
  codegenType: string,
  appId: string,
  timestamp?: number,
  useProxy = false,
): string => {
  let base: string
  if (useProxy) {
    // 将 VITE_PREVIEW_BASE_URL 的 origin 替换为 /preview-proxy，使 iframe 同源
    const previewUrl = new URL(import.meta.env.VITE_PREVIEW_BASE_URL)
    base = `/preview-proxy${previewUrl.pathname}/${codegenType}_${appId}`
  } else {
    base = `${import.meta.env.VITE_PREVIEW_BASE_URL}/${codegenType}_${appId}`
  }
  const suffix = codegenType === 'vue-project' ? '/dist/index.html' : '/'
  return timestamp ? `${base}${suffix}?t=${timestamp}` : `${base}${suffix}`
}

// 应用状态管理
const appStore = useAppStore()

// 从路由参数获取 appId
const route = useRoute()
const appId = computed(() => route.params.appId as string)
const isAgentMode = ref(route.query.isAgent !== 'false')
const mobilePanel = ref<'chat' | 'preview'>('chat')

// 可视化编辑相关状态
const isEditMode = ref(false)
const selectedElement = ref<SelectedElementInfo | null>(null)
const previewFrameRef = ref<InstanceType<typeof PreviewFrame> | null>(null)
const isDirectEditing = ref(false) // 是否正在直接编辑某个元素

// 编辑模式下使用代理 URL（同源），普通模式使用直连 URL
const activePreviewUrl = computed(() => {
  if (!previewUrl.value || !isEditMode.value) return previewUrl.value
  // 将已有 previewUrl 的 origin 替换为代理路径
  try {
    const u = new URL(previewUrl.value)
    return `/preview-proxy${u.pathname}${u.search}`
  } catch {
    return previewUrl.value
  }
})

// 切换可视化编辑模式
const toggleEditMode = () => {
  isEditMode.value = !isEditMode.value
  if (!isEditMode.value) {
    // 退出编辑模式：通知 iframe 清除高亮，清除选中元素
    const iframe = previewFrameRef.value?.iframeRef
    if (iframe) exitVisualEditorInIframe(iframe)
    selectedElement.value = null
  }
}

const switchMobilePanel = (panel: 'chat' | 'preview') => {
  mobilePanel.value = panel
}

// 清除选中元素
const clearSelectedElement = () => {
  selectedElement.value = null
}

/**
 * 直接编辑：调用后端接口将 iframe 内的修改同步到源文件
 * html 类型文件固定为 index.html；vue-project / multi-file 由 xpath 推断路径（暂不支持，降级提示）
 */
const handleDirectEdit = async (change: ContentChangeInfo) => {
  if (!app.value || !appId.value) return

  const codegenType = app.value.codegenType || 'html'

  // 根据 codegenType 确定 relativePath
  // html 类型只有一个 index.html；其他类型暂不支持直接编辑，降级到 AI
  let relativePath: string
  if (codegenType === 'html') {
    relativePath = 'index.html'
  } else if (codegenType === 'multi-file') {
    // multi-file 也只有 index.html 作为入口
    relativePath = 'index.html'
  } else {
    // vue-project 文件结构复杂，暂不支持直接定位，降级到 AI
    const prompt = `请将页面中 ${formatElementInfoForPrompt(change)} 的内容修改为：\n${change.newContent}\n\n只修改这一处内容，保持其他代码不变。`
    sendMessage(prompt)
    return
  }

  try {
    const res = await editProjectFile({
      appId: appId.value,
      relativePath,
      oldContent: change.oldContent,
      newContent: change.newContent,
    })
    if (res.data?.code === 20000) {
      message.success('修改已保存')
      // 刷新预览
      const timestamp = Date.now()
      previewUrl.value = buildPreviewUrl(codegenType, appId.value, timestamp)
    } else {
      message.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    console.error('[直接编辑失败]', e)
    message.error('保存失败，请稍后重试')
  }
}

// 监听 iframe postMessage 消息
const handleIframeMessage = (event: MessageEvent) => {
  if (event.data?.type === 'VISUAL_EDITOR_SELECT') {
    selectedElement.value = event.data.payload as SelectedElementInfo
  }
  if (event.data?.type === 'VISUAL_EDITOR_EDITING') {
    isDirectEditing.value = true
    selectedElement.value = event.data.payload as SelectedElementInfo
  }
  if (event.data?.type === 'VISUAL_EDITOR_CONTENT_CHANGE') {
    isDirectEditing.value = false
    const change = event.data.payload as ContentChangeInfo
    handleDirectEdit(change)
  }
}

// 状态定义
const app = ref<API.AppVO | null>(null)
const messages = ref<
  Array<{
    role: 'user' | 'assistant'
    content: string
    thought?: string
    workflowSteps?: WorkflowStepPayload[]
    buildLogs?: string[]
    toolEvents?: ToolEventPayload[]
    isThinking?: boolean
  }>
>([])
const userInput = ref('')
const isGenerating = ref(false)
const previewUrl = ref('')
const previewLoading = ref(false)
const appLoading = ref(false)
const isDeploying = ref(false)
const isEditModalVisible = ref(false)
const editAppName = ref('')
const isEditing = ref(false)

// 历史消息相关状态
const historyLoading = ref(false)
const loadMoreLoading = ref(false)
const hasMoreHistory = ref(false) // 是否还有更多历史消息
const oldestMessageTime = ref<string | undefined>(undefined) // 游标：最早一条消息的 createTime
const historyRecordCount = ref(0) // 后端返回的原始历史记录条数

// 解析 ChatHistoryVO 列表，chatMessageType 为角色，messages 为消息内容
const parseChatHistoryMessages = (
  historyList: API.ChatHistoryVO[],
): Array<{ role: 'user' | 'assistant'; content: string }> => {
  const result: Array<{ role: 'user' | 'assistant'; content: string }> = []
  for (const item of historyList) {
    if (!item.messages || !item.chatMessageType) continue
    // 修正：后端 chatMessageType 为 "ai"，映射到前端 "assistant"
    const role: 'user' | 'assistant' = item.chatMessageType === 'user' ? 'user' : 'assistant'
    result.push({ role, content: item.messages })
  }
  return result
}

// 加载最新 10 条历史消息（初始加载）
const loadLatestHistory = async () => {
  if (!appId.value) return
  historyLoading.value = true
  try {
    const res = await listLatestChatHistory({ appId: appId.value as unknown as number })
    if (res.data?.code === 20000 && res.data.data) {
      const historyList = res.data.data
      messages.value = parseChatHistoryMessages(historyList)
      historyRecordCount.value = historyList.length
      if (historyList.length > 0) {
        oldestMessageTime.value = historyList[0]?.createTime
      }
      // 返回 10 条说明可能还有更多
      hasMoreHistory.value = historyList.length >= 10
    }
  } catch (error) {
    console.error('[加载历史消息失败]', error)
  } finally {
    historyLoading.value = false
  }
}

// 加载更多历史消息（游标翻页）
const loadMoreHistory = async () => {
  if (!appId.value || loadMoreLoading.value || !hasMoreHistory.value) return
  loadMoreLoading.value = true
  try {
    const res = await listMoreChatHistory({
      appId: appId.value as unknown as number,
      beforeTime: oldestMessageTime.value,
    })
    if (res.data?.code === 20000 && res.data.data) {
      const historyList = res.data.data
      const parsed = parseChatHistoryMessages(historyList)
      // 将更早的消息插入到列表头部
      messages.value = [...parsed, ...messages.value]
      historyRecordCount.value += historyList.length
      if (historyList.length > 0) {
        oldestMessageTime.value = historyList[0]?.createTime
      }
      // 返回不足 10 条说明没有更多了
      hasMoreHistory.value = historyList.length >= 10
    }
  } catch (error) {
    console.error('[加载更多历史消息失败]', error)
    message.error('加载更多失败，请稍后重试')
  } finally {
    loadMoreLoading.value = false
  }
}

const upsertWorkflowStep = (messageIndex: number, step: WorkflowStepPayload) => {
  const currentMessage = messages.value[messageIndex]
  if (!currentMessage) return

  const workflowSteps = [...(currentMessage.workflowSteps ?? [])]
  const existingIndex = workflowSteps.findIndex((item) => item.step === step.step)

  if (existingIndex >= 0) {
    workflowSteps[existingIndex] = step
  } else {
    workflowSteps.push(step)
    workflowSteps.sort((a, b) => a.step - b.step)
  }

  currentMessage.workflowSteps = workflowSteps
  currentMessage.isThinking = true
}

// 发送消息并处理 SSE 流式响应
const sendMessage = async (messageText: string) => {
  if (!messageText || !messageText.trim()) {
    message.warning('请输入消息内容')
    return
  }
  if (!appId.value) {
    message.error('应用 ID 无效')
    return
  }

  // 若有选中元素，将元素信息追加到提示词
  let finalMessage = messageText.trim()
  if (selectedElement.value) {
    finalMessage = `${finalMessage}\n\n${formatElementInfoForPrompt(selectedElement.value)}`
  }

  // 发送后清除选中元素并退出编辑模式
  const iframe = previewFrameRef.value?.iframeRef
  if (iframe) exitVisualEditorInIframe(iframe)
  selectedElement.value = null
  isEditMode.value = false

  messages.value.push({ role: 'user', content: messageText.trim() })
  userInput.value = ''
  isGenerating.value = true

  const aiMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    thought: '',
    workflowSteps: [],
    buildLogs: [],
    toolEvents: [],
    isThinking: false,
  })

  try {
    const sseUrl = `${import.meta.env.VITE_API_BASE_URL}/app/chat/gen/code?appId=${appId.value}&userMessage=${encodeURIComponent(finalMessage)}&isAgent=${isAgentMode.value}`
    createSSEConnection(sseUrl, {
      // HTML/MULTI-FILE：内容追加到 content
      onMessage: (data: string) => {
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content += data
        }
      },
      // Vue工程化：思考过程追加到 thought，标记 isThinking
      onThought: (data: string) => {
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].thought =
            (messages.value[aiMessageIndex].thought ?? '') + data
          messages.value[aiMessageIndex].isThinking = true
        }
      },
      // Vue工程化：最终答案追加到 content，思考阶段结束
      onAnswer: (data: string) => {
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content += data
          messages.value[aiMessageIndex].isThinking = false
        }
      },
      onWorkflowStep: (data: WorkflowStepPayload) => {
        upsertWorkflowStep(aiMessageIndex, data)
      },
      onBuildLog: (data: string) => {
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].buildLogs = [
            ...(messages.value[aiMessageIndex].buildLogs ?? []),
            data,
          ]
        }
      },
      onTool: (data: ToolEventPayload) => {
        const msg = messages.value[aiMessageIndex]
        if (!msg) return
        const events = [...(msg.toolEvents ?? [])]
        const idx = events.findIndex((e) => e.toolCallId && e.toolCallId === data.toolCallId)
        if (idx >= 0) {
          events[idx] = data
        } else {
          events.push(data)
        }
        msg.toolEvents = events
      },
      onWorkflowError: (data: string) => {
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content += `\n${data}`
          messages.value[aiMessageIndex].isThinking = false
        }
      },
      onError: (error: Event) => {
        console.error('[SSE 连接错误]', error)
        const currentMessage = messages.value[aiMessageIndex]
        if (
          currentMessage &&
          currentMessage.content === '' &&
          !currentMessage.thought &&
          !(currentMessage.workflowSteps?.length)
        ) {
          messages.value.splice(aiMessageIndex, 1)
        }
        message.error('AI 对话请求失败，请稍后重试')
        isGenerating.value = false
      },
      onComplete: () => {
        isGenerating.value = false
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].isThinking = false
        }
        const codegenType = app.value?.codegenType || 'html'
        previewLoading.value = true
        setTimeout(() => {
          previewUrl.value = buildPreviewUrl(codegenType, appId.value, Date.now())
          previewLoading.value = false
        }, 1000)
      },
    })
  } catch (error) {
    console.error('[发送消息失败]', error)
    message.error('发送消息失败，请稍后重试')
    isGenerating.value = false
  }
}

// 加载应用信息
const loadApp = async () => {
  if (!appId.value) {
    message.error('应用 ID 无效')
    return
  }
  appLoading.value = true
  try {
    const res = await getAppVoById({ id: appId.value as unknown as number })
    if (res.data?.data) {
      app.value = res.data.data
      appStore.setCurrentApp(res.data.data)
    }
  } catch (error) {
    console.error('[加载应用信息失败]', error)
    message.error('加载应用信息失败,请稍后重试')
  } finally {
    appLoading.value = false
  }
}

// 检查并自动发送初始提示词
// 判断逻辑：route.query.initPrompt 存在（从创建页跳转）且没有历史记录，才自动触发
const checkAndSendInitPrompt = () => {
  const initPrompt = route.query.initPrompt as string
  if (!initPrompt || !initPrompt.trim()) return
  if (historyRecordCount.value > 0) return
  sendMessage(initPrompt)
}

// 组件挂载时加载应用信息和历史消息，并注册 iframe 消息监听
onMounted(async () => {
  window.addEventListener('message', handleIframeMessage)

  await loadApp()
  await loadLatestHistory()

  // 仅当历史记录 >= 2 条时，才展示预览网站
  if (historyRecordCount.value >= 2) {
    const codegenType = app.value?.codegenType || 'html'
    previewUrl.value = buildPreviewUrl(codegenType, appId.value)
    console.log(previewUrl.value)
  }

  // 加载完成后检查是否自动发送初始提示词
  checkAndSendInitPrompt()
})

// 组件卸载时移除消息监听，防止内存泄漏
onUnmounted(() => {
  window.removeEventListener('message', handleIframeMessage)
})

// 打开编辑弹框
const openEditModal = () => {
  editAppName.value = app.value?.appName || ''
  isEditModalVisible.value = true
}

// 提交编辑
const submitEdit = async () => {
  if (!editAppName.value.trim()) {
    message.warning('应用名称不能为空')
    return
  }
  isEditing.value = true
  try {
    const res = await updateMyApp({
      id: appId.value as unknown as number,
      appName: editAppName.value.trim(),
    })
    if (res.data && res.data.code === 20000) {
      message.success('更新成功')
      isEditModalVisible.value = false
      await loadApp()
    } else {
      message.error(res.data?.message || '更新失败')
    }
  } catch (error) {
    console.error('[更新应用失败]', error)
    message.error('更新应用失败，请稍后重试')
  } finally {
    isEditing.value = false
  }
}

const downloadProject = () => {
  if (!appId.value || !app.value?.codegenType) return
  const url = `${import.meta.env.VITE_API_BASE_URL}/project/download?appId=${appId.value}&codegenType=${app.value.codegenType}`
  window.open(url, '_blank')
}

const deployApp = async () => {
  if (!appId.value) {
    message.error('应用 ID 无效')
    return
  }
  isDeploying.value = true
  try {
    const res = await deployedApp({ appId: appId.value as unknown as number })
    if (res.data && res.data.code === 20000 && res.data.data) {
      const deployUrl = res.data.data
      Modal.success({
        title: '部署成功',
        content: () =>
          h('div', [
            h(
              'a',
              {
                href: deployUrl,
                style: 'word-break: break-all; cursor: pointer; color: oklch(0.62 0.16 38)',
                onClick: (e: MouseEvent) => {
                  e.preventDefault()
                  navigator.clipboard.writeText(deployUrl).then(() => {
                    message.success('链接已复制')
                  })
                },
                title: '点击复制链接',
              },
              deployUrl,
            ),
          ]),
        okText: '访问网页',
        onOk: () => {
          window.open(deployUrl, '_blank')
        },
      })
      await loadApp()
    } else {
      message.error(res.data?.message || '部署失败')
    }
  } catch (error) {
    console.error('[部署应用失败]', error)
    message.error('部署应用失败，请稍后重试')
  } finally {
    isDeploying.value = false
  }
}
</script>

<template>
  <div class="chat-view">
    <!-- 页面头部 -->
    <div class="chat-header">
      <div class="header-left">
        <a-spin v-if="appLoading" size="small" />
        <h2 v-else-if="app">{{ app.appName }}</h2>
        <h2 v-else>应用</h2>
      </div>
      <div class="header-right">
        <button
          type="button"
          class="edit-btn"
          :disabled="isGenerating || !app"
          :aria-label="'编辑应用'"
          :title="'编辑应用'"
          @click="openEditModal"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <button
          type="button"
          class="download-btn"
          :disabled="isGenerating || !app"
          :aria-label="'下载代码'"
          :title="'下载代码'"
          @click="downloadProject"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 3v13M5 14l7 7 7-7" />
            <line x1="3" y1="21" x2="21" y2="21" />
          </svg>
        </button>
        <a-button
          type="primary"
          :loading="isDeploying"
          :disabled="isDeploying || isGenerating"
          @click="deployApp"
        >
          {{ isDeploying ? '部署中...' : '部署' }}
        </a-button>
      </div>
    </div>

    <!-- 编辑应用弹框 -->
    <a-modal
      v-model:open="isEditModalVisible"
      title="编辑应用"
      :confirm-loading="isEditing"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitEdit"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="应用名称">
          <a-input v-model:value="editAppName" placeholder="请输入应用名称" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>

    <div class="mobile-panel-switch">
      <button
        type="button"
        class="mobile-panel-btn"
        :class="{ active: mobilePanel === 'chat' }"
        @click="switchMobilePanel('chat')"
      >
        对话
      </button>
      <button
        type="button"
        class="mobile-panel-btn"
        :class="{ active: mobilePanel === 'preview' }"
        @click="switchMobilePanel('preview')"
      >
        预览
      </button>
    </div>

    <!-- 左右分栏布局 -->
    <div class="chat-layout">
      <!-- 对话区 (40%) -->
      <div class="chat-section" :class="{ 'mobile-hidden': mobilePanel !== 'chat' }">
        <div class="chat-messages">
          <!-- 加载更多历史消息 -->
          <div v-if="hasMoreHistory" class="load-more-wrap">
            <a-button type="link" size="small" :loading="loadMoreLoading" @click="loadMoreHistory">
              {{ loadMoreLoading ? '加载中...' : '加载更多历史消息' }}
            </a-button>
          </div>

          <!-- 初始加载历史消息中 -->
          <div v-if="historyLoading" class="history-loading">
            <a-spin size="small" />
            <span>加载历史消息...</span>
          </div>

          <!-- 消息列表 -->
          <ChatMessage
            v-for="(msg, index) in messages"
            :key="index"
            :role="msg.role"
            :content="msg.content"
            :thought="msg.thought"
            :workflow-steps="msg.workflowSteps"
            :build-logs="msg.buildLogs"
            :tool-events="msg.toolEvents"
            :is-thinking="msg.isThinking"
            :is-streaming="
              isGenerating && index === messages.length - 1 && msg.role === 'assistant'
            "
          />
        </div>

        <!-- 输入区 -->
        <ChatInput
          v-model="userInput"
          :disabled="isGenerating"
          :edit-mode="isEditMode"
          :selected-element="selectedElement"
          :is-direct-editing="isDirectEditing"
          :has-preview="!!previewUrl"
          @send="sendMessage"
          @toggle-edit-mode="toggleEditMode"
          @clear-selected-element="clearSelectedElement"
        />
      </div>

      <!-- 预览区 (60%) -->
      <div class="preview-section" :class="{ 'mobile-hidden': mobilePanel !== 'preview' }">
        <PreviewFrame
          ref="previewFrameRef"
          :url="activePreviewUrl"
          :loading="previewLoading"
          :edit-mode="isEditMode"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-view {
  width: 100%;
  height: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background:
    radial-gradient(
      circle at 88% 8%,
      color-mix(in oklch, var(--auth-accent-soft) 78%, transparent) 0%,
      transparent 30%
    ),
    linear-gradient(150deg, var(--auth-bg-main) 0%, oklch(0.96 0.018 74) 100%);
}

.chat-header {
  padding: var(--space-3) var(--space-6);
  background: color-mix(in oklch, var(--auth-surface) 96%, transparent);
  border-bottom: 1px solid var(--auth-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  box-shadow: 0 8px 24px oklch(0.46 0.04 42 / 0.08);
  backdrop-filter: blur(4px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  border: 1px solid var(--auth-line);
  background: color-mix(in oklch, var(--auth-surface) 86%, transparent);
  color: var(--auth-text-primary);
  cursor: pointer;
  transition:
    background-color 0.2s,
    color 0.2s,
    border-color 0.2s,
    transform 0.2s;
  position: relative;
  padding: 0;
}

.edit-btn:hover:not(:disabled)::after {
  content: '编辑应用';
  position: absolute;
  bottom: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: color-mix(in oklch, var(--auth-text-primary) 86%, transparent);
  color: oklch(0.98 0.01 80);
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}

.edit-btn:hover:not(:disabled) {
  background: var(--auth-accent-soft);
  border-color: color-mix(in oklch, var(--auth-accent) 36%, var(--auth-line));
  transform: translateY(-1px);
}

.edit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.edit-btn:focus-visible {
  outline: 2px solid var(--auth-focus);
  outline-offset: 2px;
}

.download-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  border: 1px solid var(--auth-line);
  background: color-mix(in oklch, var(--auth-surface) 86%, transparent);
  color: var(--auth-text-primary);
  cursor: pointer;
  transition:
    background-color 0.2s,
    color 0.2s,
    opacity 0.2s,
    border-color 0.2s,
    transform 0.2s;
  position: relative;
  padding: 0;
}

.download-btn:hover:not(.disabled) {
  background: var(--auth-accent-soft);
  border-color: color-mix(in oklch, var(--auth-accent) 36%, var(--auth-line));
  transform: translateY(-1px);
}

.download-btn:hover:not(.disabled)::after {
  content: '下载代码';
  position: absolute;
  bottom: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: color-mix(in oklch, var(--auth-text-primary) 86%, transparent);
  color: oklch(0.98 0.01 80);
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}

.download-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.download-btn:focus-visible {
  outline: 2px solid var(--auth-focus);
  outline-offset: 2px;
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--auth-text-primary);
  font-family: var(--auth-font-display);
  letter-spacing: -0.01em;
}

.chat-layout {
  display: flex;
  flex: 1;
  height: 100%;
  min-height: 0;
  gap: 0;
  padding: 0;
  overflow: hidden;
}

.mobile-panel-switch {
  display: none;
}

.chat-section {
  flex: 0 0 40%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 0;
  min-width: 0;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border-right: 1px solid var(--auth-line);
  background: color-mix(in oklch, var(--auth-surface) 92%, transparent);
}

.chat-messages {
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-4);
  background: color-mix(in oklch, var(--auth-surface) 84%, transparent);
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 4px 0 8px;
}

.history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  color: var(--auth-text-secondary);
  font-size: 13px;
}

.preview-section {
  flex: 0 0 60%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  background: color-mix(in oklch, var(--auth-surface) 76%, transparent);
}

.chat-header :deep(.ant-btn-primary) {
  border-color: transparent !important;
  background: var(--auth-accent) !important;
  box-shadow: var(--auth-shadow-button) !important;
  color: oklch(0.98 0.01 80) !important;
}

.chat-header :deep(.ant-btn-primary:hover),
.chat-header :deep(.ant-btn-primary:focus) {
  background: var(--auth-accent-hover) !important;
  box-shadow: var(--auth-shadow-button-hover) !important;
}

.chat-messages :deep(.ant-btn-link) {
  color: var(--auth-accent-hover) !important;
}

.chat-messages :deep(.ant-btn-link:hover) {
  color: var(--auth-accent) !important;
}

@media (max-width: 768px) {
  .chat-view {
    overflow: hidden;
  }

  .mobile-panel-switch {
    display: inline-grid;
    grid-template-columns: repeat(2, minmax(88px, 1fr));
    gap: var(--space-1);
    align-self: flex-start;
    margin: 0 var(--space-4) var(--space-2);
    padding: 4px;
    border: 1px solid var(--auth-line);
    border-radius: 999px;
    background: color-mix(in oklch, var(--auth-surface) 92%, transparent);
  }

  .mobile-panel-btn {
    height: 34px;
    padding: 0 14px;
    border-radius: 999px;
    color: var(--auth-text-secondary);
    font-size: 13px;
    font-weight: 600;
    background: transparent;
  }

  .mobile-panel-btn.active {
    background: var(--auth-accent-soft);
    color: var(--auth-text-primary);
  }

  .chat-layout {
    position: relative;
    overflow: hidden;
  }

  .chat-section {
    flex: 1;
    width: 100%;
    border-right: none;
    border-bottom: none;
  }

  .preview-section {
    flex: 1;
    width: 100%;
  }

  .mobile-hidden {
    display: none;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .chat-section {
    flex: 0 0 40%;
  }

  .preview-section {
    flex: 0 0 60%;
  }
}

@media (min-width: 1025px) {
  .chat-layout {
    flex-direction: row;
  }

  .chat-section {
    flex: 2;
  }

  .preview-section {
    flex: 3;
  }
}
</style>
