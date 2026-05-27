<template>
  <div class="admin-users">
    <!-- 搜索区 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchForm" @finish="handleSearch">
        <a-form-item label="ID" name="id">
          <a-input-number v-model:value="searchForm.id" placeholder="用户ID" :min="0" style="width: 140px" />
        </a-form-item>
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="searchForm.username" placeholder="用户名" allow-clear />
        </a-form-item>
        <a-form-item label="用户简介" name="userProfile">
          <a-input v-model:value="searchForm.userProfile" placeholder="用户简介" allow-clear />
        </a-form-item>
        <a-form-item label="用户角色" name="userRole">
          <a-select v-model:value="searchForm.userRole" placeholder="用户角色" allow-clear style="width: 130px">
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 数据表格 -->
    <a-card class="table-card" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="paginationConfig"
        :scroll="{ x: 1200 }"
        row-key="id"
        @change="handleTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <!-- 头像缩略图 -->
          <template v-if="column.key === 'userAvatar'">
            <a-image
              v-if="record.userAvatar"
              :src="record.userAvatar"
              :width="48"
              :height="48"
              style="border-radius: 4px; object-fit: cover;"
              :preview="{ mask: '查看' }"
              fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            />
            <span v-else class="no-avatar">无头像</span>
          </template>

          <!-- 用户角色 -->
          <template v-if="column.key === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'red' : 'blue'">
              {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>

          <!-- 创建时间 -->
          <template v-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>

          <!-- 应用配额 -->
          <template v-if="column.key === 'appQuota'">
            <span v-if="record.appMaxCount === -1">不限制（已用 {{ record.appUsedCount ?? 0 }}）</span>
            <span v-else>{{ record.appUsedCount ?? 0 }} / {{ record.appMaxCount ?? 0 }}</span>
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openViewModal(record)">查看</a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button type="link" size="small" @click="openAddQuotaModal(record)">加次数</a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 查看详情弹窗 -->
    <a-modal
      v-model:open="viewModalVisible"
      title="用户详情"
      footer=""
      @cancel="handleViewCancel"
      cancel-text="关闭"
    >
      <a-descriptions :column="1" bordered :label-style="{ width: '120px' }">
        <a-descriptions-item label="ID">{{ viewData.id ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户账户">{{ viewData.userAccount ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户名">{{ viewData.username ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="头像">
          <a-image
            v-if="viewData.userAvatar"
            :src="viewData.userAvatar"
            :width="80"
            style="border-radius: 4px;"
            fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
          />
          <span v-else>无</span>
        </a-descriptions-item>
        <a-descriptions-item label="用户简介">{{ viewData.userProfile ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户角色">
          <a-tag :color="viewData.userRole === 'admin' ? 'red' : 'blue'">
            {{ viewData.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatTime(viewData.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ formatTime(viewData.updateTime) }}</a-descriptions-item>
        <a-descriptions-item label="应用配额">
          <template v-if="viewData.appMaxCount === -1">不限制（已创建 {{ viewData.appUsedCount ?? 0 }} 个）</template>
          <template v-else>已创建 {{ viewData.appUsedCount ?? 0 }} / 上限 {{ viewData.appMaxCount ?? 0 }}</template>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑用户"
      :confirm-loading="editLoading"
      @ok="handleEditSubmit"
      @cancel="handleEditCancel"
      ok-text="保存"
      cancel-text="取消"
    >
      <a-form layout="vertical" :model="editForm" :rules="editRules">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="editForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像URL" name="userAvatar">
          <a-input v-model:value="editForm.userAvatar" placeholder="请输入头像 URL" />
          <div v-if="editForm.userAvatar" class="avatar-preview-wrapper">
            <a-image
              :src="editForm.userAvatar"
              :width="80"
              :height="80"
              style="border-radius: 4px; object-fit: cover; margin-top: 8px;"
              fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            />
          </div>
        </a-form-item>
        <a-form-item label="用户简介" name="userProfile">
          <a-textarea v-model:value="editForm.userProfile" placeholder="请输入用户简介" :rows="3" />
        </a-form-item>
        <a-form-item label="用户角色" name="userRole">
          <a-select v-model:value="editForm.userRole" placeholder="请选择用户角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="应用创建上限" name="appMaxCount">
          <a-input-number
            v-model:value="editForm.appMaxCount"
            :min="-1"
            placeholder="最大可创建应用数，-1 为不限制"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">
            已使用 {{ editForm.appUsedCount ?? 0 }} 次，-1 表示不限制，其他值不可低于已使用次数
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 增加次数弹窗 -->
    <a-modal
      v-model:open="addQuotaModalVisible"
      title="增加应用创建次数"
      @ok="handleAddQuotaSubmit"
      ok-text="确定"
      cancel-text="取消"
    >
      <a-form layout="vertical">
        <a-form-item label="增加次数">
          <a-input-number v-model:value="addQuotaCount" :min="1" :max="999" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import { listUserByPage, getUserById, updateUser, deleteUser, addAppQuota } from '@/api/userController'
import { useLoginUserStore } from '@/stores/LoginUserStore'
import { HOME_PATH } from '@/utils/authAccess'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 权限检查
onMounted(async () => {
  await loginUserStore.ensureLoginUserLoaded()
  if (loginUserStore.loginUser.userRole !== 'admin') {
    message.warning('无权限访问，仅管理员可操作')
    void router.replace(HOME_PATH)
    return
  }
  loadData()
})

// ========== 搜索 ==========
const searchForm = reactive({
  id: undefined as number | undefined,
  username: undefined as string | undefined,
  userProfile: undefined as string | undefined,
  userRole: undefined as string | undefined,
})

const handleSearch = () => {
  loadData(1)
}

const handleReset = () => {
  searchForm.id = undefined
  searchForm.username = undefined
  searchForm.userProfile = undefined
  searchForm.userRole = undefined
  loadData(1)
}

// ========== 表格数据 ==========
const dataSource = ref<API.User[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const columns: TableColumnType[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户账户', dataIndex: 'userAccount', key: 'userAccount', width: 140 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 140 },
  { title: '头像', key: 'userAvatar', width: 80 },
  { title: '用户简介', dataIndex: 'userProfile', key: 'userProfile', width: 200, ellipsis: true },
  { title: '用户角色', key: 'userRole', dataIndex: 'userRole', width: 100 },
  { title: '应用配额', key: 'appQuota', width: 120 },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

const paginationConfig = computed(() => ({
  current: currentPage.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`,
}))

const loadData = async (page?: number) => {
  loading.value = true
  try {
    const response = await listUserByPage({
      current: page ?? currentPage.value,
      pageSize: pageSize.value,
      id: searchForm.id,
      username: searchForm.username || undefined,
      userProfile: searchForm.userProfile || undefined,
      userRole: searchForm.userRole || undefined,
    })
    if (response.data.code === 20000 && response.data.data) {
      dataSource.value = response.data.data.records ?? []
      total.value = response.data.data.totalRow ?? 0
      currentPage.value = response.data.data.pageNumber ?? 1
    } else {
      message.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('[加载用户列表失败]', error)
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: { current?: number; pageSize?: number }) => {
  currentPage.value = pag.current ?? 1
  pageSize.value = pag.pageSize ?? 10
  loadData()
}

// ========== 查看弹窗 ==========
const viewModalVisible = ref(false)
const viewData = reactive<API.User>({})

const openViewModal = async (record: API.User) => {
  if (record.id == null) return
  try {
    const response = await getUserById({ id: Number(record.id) })
    if (response.data.code === 20000 && response.data.data) {
      const user = response.data.data
      Object.assign(viewData, user)
    } else {
      Object.assign(viewData, record)
    }
  } catch {
    Object.assign(viewData, record)
  }
  viewModalVisible.value = true
}

const handleViewCancel = () => {
  viewModalVisible.value = false
}

// ========== 编辑弹窗 ==========
const editModalVisible = ref(false)
const editLoading = ref(false)
const editingUserId = ref<number | null>(null)
const editForm = reactive({
  username: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user' as string,
  appMaxCount: 3 as number,
  appUsedCount: 0 as number,
})
const editRules = {
  userRole: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
}

const openEditModal = async (record: API.User) => {
  if (record.id == null) return
  editingUserId.value = Number(record.id)
  // 加载最新数据
  try {
    const response = await getUserById({ id: Number(record.id) })
    if (response.data.code === 20000 && response.data.data) {
      const user = response.data.data
      editForm.username = user.username ?? ''
      editForm.userAvatar = user.userAvatar ?? ''
      editForm.userProfile = user.userProfile ?? ''
      editForm.userRole = user.userRole ?? 'user'
      editForm.appMaxCount = user.appMaxCount ?? 3
      editForm.appUsedCount = user.appUsedCount ?? 0
    } else {
      editForm.username = record.username ?? ''
      editForm.userAvatar = record.userAvatar ?? ''
      editForm.userProfile = record.userProfile ?? ''
      editForm.userRole = record.userRole ?? 'user'
      editForm.appMaxCount = record.appMaxCount ?? 3
      editForm.appUsedCount = record.appUsedCount ?? 0
    }
  } catch {
    editForm.username = record.username ?? ''
    editForm.userAvatar = record.userAvatar ?? ''
    editForm.userProfile = record.userProfile ?? ''
    editForm.userRole = record.userRole ?? 'user'
    editForm.appMaxCount = record.appMaxCount ?? 3
    editForm.appUsedCount = record.appUsedCount ?? 0
  }
  editModalVisible.value = true
}

const handleEditSubmit = async () => {
  if (editingUserId.value == null) return
  editLoading.value = true
  try {
    const response = await updateUser({
      id: editingUserId.value,
      username: editForm.username || undefined,
      userAvatar: editForm.userAvatar || undefined,
      userProfile: editForm.userProfile || undefined,
      userRole: editForm.userRole || undefined,
      appMaxCount: editForm.appMaxCount,
    })
    if (response.data.code === 20000) {
      message.success('更新成功')
      editModalVisible.value = false
      loadData()
    } else {
      message.error(response.data.message || '更新失败')
    }
  } catch (error) {
    console.error('[更新用户失败]', error)
    message.error('更新失败，请稍后重试')
  } finally {
    editLoading.value = false
  }
}

const handleEditCancel = () => {
  editModalVisible.value = false
}

// ========== 删除 ==========
const handleDelete = (record: API.User) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户"${record.username || record.userAccount}"（ID: ${record.id}）吗？此操作不可恢复。`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const response = await deleteUser({ id: record.id })
        if (response.data.code === 20000 && response.data.data) {
          message.success('删除成功')
          loadData()
        } else {
          message.error(response.data.message || '删除失败')
        }
      } catch (error) {
        console.error('[删除用户失败]', error)
        message.error('删除失败，请稍后重试')
      }
    },
  })
}

// ========== 增加次数 ==========
const addQuotaModalVisible = ref(false)
const addQuotaUserId = ref<number | null>(null)
const addQuotaCount = ref(1)

const openAddQuotaModal = (record: API.User) => {
  addQuotaUserId.value = Number(record.id)
  addQuotaCount.value = 1
  addQuotaModalVisible.value = true
}

const handleAddQuotaSubmit = async () => {
  if (addQuotaUserId.value == null || addQuotaCount.value <= 0) return
  try {
    const response = await addAppQuota({
      userId: addQuotaUserId.value,
      addCount: addQuotaCount.value,
    })
    if (response.data.code === 20000) {
      message.success('增加成功')
      addQuotaModalVisible.value = false
      loadData()
    } else {
      message.error(response.data.message || '操作失败')
    }
  } catch {
    message.error('操作失败，请稍后重试')
  }
}

// ========== 工具函数 ==========
const formatTime = (time?: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.admin-users {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  min-height: 400px;
}

.no-avatar {
  color: #999;
  font-size: 12px;
}

.avatar-preview-wrapper {
  display: inline-block;
}
</style>
