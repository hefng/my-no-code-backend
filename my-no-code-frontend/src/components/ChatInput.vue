<script setup lang="ts">
import type { SelectedElementInfo } from '@/utils/visualEditor'
import { formatElementInfoForDisplay } from '@/utils/visualEditor'

interface Props {
  disabled?: boolean
  placeholder?: string
  /** 是否处于可视化编辑模式 */
  editMode?: boolean
  /** 当前选中/正在编辑的元素信息 */
  selectedElement?: SelectedElementInfo | null
  /** 是否正在直接编辑某个元素 */
  isDirectEditing?: boolean
  /** 是否有预览 URL（无预览时禁用编辑模式按钮） */
  hasPreview?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  placeholder: '描述你想做的页面，例如：帮我做一个摄影工作室官网，包含作品展示和预约表单',
  editMode: false,
  selectedElement: null,
  isDirectEditing: false,
  hasPreview: false,
})

const emit = defineEmits<{
  send: [text: string]
  'toggle-edit-mode': []
  'clear-selected-element': []
}>()

// 双向绑定输入内容
const value = defineModel<string>({ default: '' })

const handleSend = () => {
  if (value.value.trim()) {
    emit('send', value.value)
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.isComposing) return
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input-wrapper">
    <!-- 选中/编辑元素信息提示 -->
    <a-alert
      v-if="props.isDirectEditing && props.selectedElement"
      class="selected-element-alert"
      type="warning"
      :message="`正在编辑：${formatElementInfoForDisplay(props.selectedElement)} · 失焦后自动同步`"
      show-icon
    />
    <a-alert
      v-else-if="props.selectedElement"
      class="selected-element-alert"
      type="info"
      :message="`已选中元素：${formatElementInfoForDisplay(props.selectedElement)}`"
      show-icon
      closable
      @close="emit('clear-selected-element')"
    />

    <div class="chat-input-area">
      <!-- 可视化编辑模式切换按钮 -->
      <a-tooltip
        :title="
          props.hasPreview
            ? props.editMode
              ? '退出编辑模式'
              : '进入可视化编辑模式'
            : '请先生成网站'
        "
      >
        <button
          class="edit-mode-btn"
          :class="{ active: props.editMode, disabled: !props.hasPreview }"
          :disabled="!props.hasPreview || props.disabled"
          @click="emit('toggle-edit-mode')"
        >
          <!-- 鼠标点击/光标图标 -->
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M4 4l6 18 3-7 7-3z" />
            <path d="M13 13l5 5" />
          </svg>
        </button>
      </a-tooltip>

      <a-textarea
        v-model:value="value"
        :placeholder="props.placeholder"
        :auto-size="{ minRows: 2, maxRows: 6 }"
        :disabled="props.disabled"
        @keydown="handleKeydown"
      />
      <a-button
        type="primary"
        :loading="props.disabled"
        :disabled="!value.trim()"
        @click="handleSend"
      >
        发送
      </a-button>
    </div>
  </div>
</template>

<style scoped>
.chat-input-wrapper {
  flex-shrink: 0;
  background: color-mix(in oklch, var(--auth-surface) 95%, transparent);
  border-top: 1px solid var(--auth-line);
}

.selected-element-alert {
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-top: none;
  font-size: 13px;
}

.chat-input-area {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  align-items: flex-end;
}

.chat-input-area :deep(.ant-input) {
  flex: 1;
  border-color: var(--auth-line) !important;
  border-radius: 12px !important;
  color: var(--auth-text-primary) !important;
  background: color-mix(in oklch, var(--auth-surface) 90%, transparent) !important;
  box-shadow: none !important;
}

.chat-input-area :deep(.ant-input::placeholder) {
  color: color-mix(in oklch, var(--auth-text-soft) 80%, transparent) !important;
}

.chat-input-area :deep(.ant-input:hover) {
  border-color: color-mix(in oklch, var(--auth-line) 60%, var(--auth-accent)) !important;
}

.chat-input-area :deep(.ant-input:focus) {
  border-color: var(--auth-accent) !important;
  box-shadow: 0 0 0 3px oklch(0.88 0.04 38 / 0.72) !important;
}

.chat-input-area :deep(.ant-btn-primary) {
  border-color: transparent !important;
  background: var(--auth-accent) !important;
  box-shadow: var(--auth-shadow-button) !important;
  color: oklch(0.98 0.01 80) !important;
}

.chat-input-area :deep(.ant-btn-primary:hover),
.chat-input-area :deep(.ant-btn-primary:focus) {
  background: var(--auth-accent-hover) !important;
  box-shadow: var(--auth-shadow-button-hover) !important;
}

/* 可视化编辑模式按钮 */
.edit-mode-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  border: 1px solid var(--auth-line);
  background: color-mix(in oklch, var(--auth-surface) 88%, transparent);
  color: var(--auth-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.2s,
    color 0.2s,
    border-color 0.2s;
  padding: 0;
}

.edit-mode-btn:hover:not(.disabled) {
  background: var(--auth-accent-soft);
  border-color: color-mix(in oklch, var(--auth-accent) 40%, var(--auth-line));
  color: var(--auth-accent-hover);
}

.edit-mode-btn.active {
  background: var(--auth-accent);
  border-color: var(--auth-accent);
  color: oklch(0.98 0.01 80);
}

.edit-mode-btn.active:hover {
  background: var(--auth-accent-hover);
}

.edit-mode-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .chat-input-area {
    padding: var(--space-2) var(--space-3);
  }

  .chat-input-area :deep(.ant-input) {
    font-size: 16px;
    min-height: 44px;
  }

  .chat-input-area :deep(.ant-btn) {
    min-height: 44px;
    min-width: 64px;
  }
}
</style>
