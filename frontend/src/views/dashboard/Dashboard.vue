<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h2 class="page-title">文档工作台</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新建文档
      </el-button>
    </div>

    <div class="stats-row">
      <div class="stat-card" @click="activeTab = 'all'">
        <div class="stat-icon all-icon">
          <el-icon :size="24"><Files /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.totalCount || 0 }}</span>
          <span class="stat-label">全部文档</span>
        </div>
      </div>
      <div class="stat-card" @click="activeTab = 'own'">
        <div class="stat-icon own-icon">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.ownCount || 0 }}</span>
          <span class="stat-label">我创建的</span>
        </div>
      </div>
      <div class="stat-card" @click="activeTab = 'shared'">
        <div class="stat-icon shared-icon">
          <el-icon :size="24"><Share /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.sharedCount || 0 }}</span>
          <span class="stat-label">与我共享</span>
        </div>
      </div>
    </div>

    <div class="toolbar-row">
      <el-tabs v-model="activeTab" class="doc-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部文档" name="all" />
        <el-tab-pane label="我创建的" name="own" />
        <el-tab-pane label="与我共享" name="shared" />
      </el-tabs>
      <div class="search-box">
        <el-input
          v-model="searchKey"
          placeholder="搜索文档标题..."
          clearable
          :prefix-icon="Search"
          @input="handleSearch"
        />
      </div>
    </div>

    <div v-loading="loading" class="doc-grid">
      <div
        v-for="doc in filteredDocs"
        :key="doc.id"
        class="doc-card"
        @click="openDoc(doc.id)"
      >
        <div class="doc-card-header">
          <div class="doc-icon">
            <el-icon :size="22"><Document /></el-icon>
          </div>
          <el-dropdown trigger="click" @command="cmd => handleDocAction(cmd, doc)">
            <el-icon class="doc-more" @click.stop><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="share">
                  <el-icon><Share /></el-icon>分享
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <el-icon><Delete /></el-icon>删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <h3 class="doc-title">{{ doc.title || '无标题文档' }}</h3>
        <div class="doc-meta">
          <span class="doc-time">{{ formatTime(doc.updatedAt || doc.createdAt) }}</span>
          <el-tag v-if="doc.isPublic === 1 || doc.isPublic === true" size="small" type="success" effect="plain">公开</el-tag>
          <el-tag v-else size="small" type="info" effect="plain">私有</el-tag>
        </div>
      </div>

      <div v-if="!loading && filteredDocs.length === 0" class="empty-state">
        <el-icon :size="48" class="empty-icon"><FolderOpened /></el-icon>
        <p v-if="searchKey">未找到匹配的文档</p>
        <p v-else-if="activeTab === 'shared'">暂无共享文档</p>
        <p v-else>暂无文档，点击右上角创建</p>
      </div>
    </div>

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="fetchDocs"
        @current-change="fetchDocs"
      />
    </div>

    <el-dialog v-model="showCreateDialog" title="新建文档" width="480px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="70px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="createForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showShareDialog" title="分享文档" width="460px" @open="shareSearch = ''">
      <div class="share-doc-name">
        <el-icon><Document /></el-icon>
        <span>{{ shareDoc?.title || '无标题文档' }}</span>
      </div>
      <el-form label-width="80px">
        <el-form-item label="搜索用户">
          <el-input v-model="shareSearch" placeholder="输入用户名或昵称搜索" clearable @input="handleShareSearch" />
          <div v-if="shareResults.length > 0" class="share-search-list">
            <div v-for="u in shareResults" :key="u.id" class="share-search-item" :class="{ active: shareUserId === u.id }" @click="shareUserId = u.id; shareSelectedUser = u; shareResults = []; shareSearch = ''">
              <el-avatar :size="28" class="share-avatar">{{ (u.nickname || u.username || 'U').charAt(0) }}</el-avatar>
              <div class="share-user-info">
                <span class="share-nickname">{{ u.nickname || u.username }}</span>
                <span class="share-uname">@{{ u.username }}</span>
              </div>
            </div>
          </div>
          <div v-else-if="shareSearch && !shareLoading" class="share-empty">未找到匹配用户</div>
        </el-form-item>
        <el-form-item v-if="shareSelectedUser" label="已选用户">
          <el-tag type="success" closable @close="shareUserId = null; shareSelectedUser = null">
            {{ shareSelectedUser.nickname || shareSelectedUser.username }} (@{{ shareSelectedUser.username }})
          </el-tag>
        </el-form-item>
        <el-form-item label="权限">
          <el-select v-model="sharePermission" style="width: 100%">
            <el-option label="只读 (VIEW)" value="VIEW" />
            <el-option label="可编辑 (EDIT)" value="EDIT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShareDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!shareUserId" @click="handleShare">分享</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getDocumentList, getSharedDocuments, getOwnDocuments, getDocumentStats, createDocument as createDocApi, deleteDocument as deleteDocApi, addMember } from '@/api/document'
import { searchUsers } from '@/api/auth'
import { Document, Plus, MoreFilled, Delete, FolderOpened, Search, Files, Share, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const activeTab = ref(route.path === '/shared' ? 'shared' : 'all')
const searchKey = ref('')
const showCreateDialog = ref(false)
const creating = ref(false)
const createFormRef = ref(null)

const documents = ref([])
const total = ref(0)
const stats = reactive({ totalCount: 0, ownCount: 0, sharedCount: 0 })

const showShareDialog = ref(false)
const shareDoc = ref(null)
const shareSearch = ref('')
const shareResults = ref([])
const shareLoading = ref(false)
const shareUserId = ref(null)
const shareSelectedUser = ref(null)
const sharePermission = ref('EDIT')
let shareTimer = null

const createForm = reactive({
  title: '',
  content: '',
  isPublic: false
})

const createRules = {
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }]
}

const filteredDocs = computed(() => {
  if (!searchKey.value) return documents.value
  const key = searchKey.value.toLowerCase()
  return documents.value.filter(d => (d.title || '').toLowerCase().includes(key))
})

const fetchStats = async () => {
  try {
    const res = await getDocumentStats()
    Object.assign(stats, res.data)
  } catch (e) {}
}

const fetchDocs = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    let res
    if (activeTab.value === 'shared') {
      res = await getSharedDocuments(params)
    } else if (activeTab.value === 'own') {
      res = await getOwnDocuments(params)
    } else {
      res = await getDocumentList(params)
    }
    documents.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchDocs()
}

const handleSearch = () => {}

const openDoc = (id) => {
  router.push(`/editor/${id}`)
}

const handleCreate = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const submitData = { ...createForm, isPublic: createForm.isPublic ? 1 : 0 }
    const res = await createDocApi(submitData)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.title = ''
    createForm.content = ''
    createForm.isPublic = false
    fetchStats()
    if (res.data?.id) {
      router.push(`/editor/${res.data.id}`)
    } else {
      fetchDocs()
    }
  } finally {
    creating.value = false
  }
}

const handleDocAction = async (cmd, doc) => {
  if (cmd === 'share') {
    shareDoc.value = doc
    shareUserId.value = null
    shareSelectedUser.value = null
    sharePermission.value = 'EDIT'
    showShareDialog.value = true
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm('确定要删除此文档吗？删除后不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDocApi(doc.id)
    ElMessage.success('删除成功')
    fetchStats()
    fetchDocs()
  }
}

const handleShareSearch = () => {
  clearTimeout(shareTimer)
  if (!shareSearch.value.trim()) {
    shareResults.value = []
    return
  }
  shareTimer = setTimeout(async () => {
    shareLoading.value = true
    try {
      const res = await searchUsers(shareSearch.value.trim())
      shareResults.value = res.data || []
    } catch (e) {
      shareResults.value = []
    } finally {
      shareLoading.value = false
    }
  }, 300)
}

const handleShare = async () => {
  if (!shareUserId.value || !shareDoc.value) return
  try {
    await addMember(shareDoc.value.id, { userId: shareUserId.value, permission: sharePermission.value })
    ElMessage.success('分享成功')
    showShareDialog.value = false
    fetchStats()
  } catch (e) { /* handled */ }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return d.toLocaleDateString('zh-CN')
}

watch(() => route.path, (newPath) => {
  if (newPath === '/shared') {
    activeTab.value = 'shared'
  } else if (newPath === '/dashboard') {
    activeTab.value = 'all'
  }
  currentPage.value = 1
  fetchDocs()
})

onMounted(() => {
  fetchStats()
  fetchDocs()
})
</script>

<style lang="scss" scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.all-icon {
  background: rgba(45, 184, 127, 0.1);
  color: #2DB87F;
}

.own-icon {
  background: rgba(54, 181, 160, 0.1);
  color: #36B5A0;
}

.shared-icon {
  background: rgba(245, 166, 35, 0.1);
  color: #F5A623;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
}

.doc-tabs {
  flex: 1;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
}

.search-box {
  width: 240px;
  flex-shrink: 0;
}

.doc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.doc-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    border-color: var(--primary-color);
  }
}

.doc-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.doc-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(45, 184, 127, 0.1);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.doc-more {
  font-size: 18px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-color);
    color: var(--text-primary);
  }
}

.doc-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.doc-time {
  color: var(--text-secondary);
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-secondary);

  .empty-icon {
    color: var(--border-color);
    margin-bottom: 12px;
  }

  p {
    font-size: 14px;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.share-doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(45, 184, 127, 0.06);
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.share-search-list {
  margin-top: 8px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  max-height: 180px;
  overflow-y: auto;
}

.share-search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover { background: rgba(45, 184, 127, 0.06); }
  &.active { background: rgba(45, 184, 127, 0.1); }
  & + & { border-top: 1px solid var(--border-color); }
}

.share-avatar {
  background: var(--primary-color);
  color: #fff;
  font-size: 12px;
  flex-shrink: 0;
}

.share-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.share-nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.share-uname {
  font-size: 12px;
  color: var(--text-secondary);
}

.share-empty {
  margin-top: 8px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 12px;
}
</style>
