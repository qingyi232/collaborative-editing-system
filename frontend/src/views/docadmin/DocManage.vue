<template>
  <div class="doc-manage">
    <h2 class="page-title">文档管理</h2>

    <div class="stats-row">
      <div class="stat-card" v-for="s in statCards" :key="s.key">
        <div class="stat-value">{{ stats[s.key] || 0 }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索文档标题..."
        clearable
        style="width: 260px"
        @clear="fetchDocuments"
        @keyup.enter="fetchDocuments"
      />
      <el-button type="primary" @click="fetchDocuments">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <el-table :data="documents" v-loading="loading" stripe class="doc-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="文档标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ownerName" label="所有者" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="plain">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="公开" width="70">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small" effect="plain">
            {{ row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="memberCount" label="成员数" width="80" />
      <el-table-column label="版本" width="70">
        <template #default="{ row }">
          V{{ row.currentVersion || 1 }}
        </template>
      </el-table-column>
      <el-table-column label="大小" width="90">
        <template #default="{ row }">
          {{ formatSize(row.docSize || 0) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" type="primary" @click="openMemberDialog(row)">
            成员管理
          </el-button>
          <el-button text size="small" @click="openPermissionDialog(row)">
            权限设置
          </el-button>
          <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, row)">
            <el-button text size="small">
              更多
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">修改标题</el-dropdown-item>
                <el-dropdown-item command="edit">进入编辑</el-dropdown-item>
                <el-dropdown-item command="transfer">转移所有权</el-dropdown-item>
                <el-dropdown-item v-if="row.status === 1" command="archive">归档</el-dropdown-item>
                <el-dropdown-item v-if="row.status === 2" command="restore">恢复</el-dropdown-item>
                <el-dropdown-item command="delete" style="color: #e5524a;">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="fetchDocuments"
        @current-change="fetchDocuments"
      />
    </div>

    <el-dialog v-model="showMemberDialog" :title="'成员管理 - ' + (currentDocTitle || '')" width="560px">
      <div class="member-dialog-actions">
        <el-button type="primary" size="small" @click="showAddMemberForm = true">
          <el-icon><Plus /></el-icon>
          添加成员
        </el-button>
      </div>

      <div v-if="showAddMemberForm" class="add-member-section">
        <el-input v-model="memberSearchKeyword" placeholder="搜索用户名或昵称" size="small" @input="handleMemberSearch" />
        <div v-if="memberSearchResults.length > 0" class="member-search-results">
          <div v-for="u in memberSearchResults" :key="u.id" class="member-search-item" @click="selectMemberUser(u)">
            <el-avatar :size="24" style="background: var(--primary-color); color: #fff; font-size: 10px;">
              {{ (u.nickname || u.username || 'U').charAt(0) }}
            </el-avatar>
            <span>{{ u.nickname || u.username }} (@{{ u.username }})</span>
          </div>
        </div>
        <div v-if="selectedMemberUser" class="selected-member-row">
          <el-tag type="success" closable @close="selectedMemberUser = null">
            {{ selectedMemberUser.nickname || selectedMemberUser.username }}
          </el-tag>
          <el-select v-model="newMemberPermission" size="small" style="width: 120px;">
            <el-option label="只读" value="VIEW" />
            <el-option label="可编辑" value="EDIT" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
          <el-button type="primary" size="small" @click="handleAddDocMember">添加</el-button>
        </div>
      </div>

      <el-table :data="currentDocMembers" size="small" class="member-table">
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-avatar :size="24" style="background: var(--secondary-color); color: #fff; font-size: 10px;">
                {{ (row.nickname || row.username || 'U').charAt(0) }}
              </el-avatar>
              <span>{{ row.nickname || row.username || '用户' + row.userId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="权限" width="130">
          <template #default="{ row }">
            <el-select
              v-if="row.permission !== 'OWNER'"
              :model-value="row.permission"
              size="small"
              @change="(val) => handleUpdateMemberPerm(row, val)"
            >
              <el-option label="只读" value="VIEW" />
              <el-option label="可编辑" value="EDIT" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
            <el-tag v-else type="danger" size="small" effect="plain">所有者</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button v-if="row.permission !== 'OWNER'" text type="danger" size="small"
              @click="handleRemoveDocMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="showPermissionDialog" :title="'权限设置 - ' + (currentDocTitle || '')" width="400px">
      <el-form label-width="90px">
        <el-form-item label="公开访问">
          <el-switch :model-value="currentDocPublic" @change="handleToggleDocPublic" />
        </el-form-item>
        <el-form-item label="文档状态">
          <el-select :model-value="currentDocStatus" @change="handleChangeDocStatus">
            <el-option label="正常" :value="1" />
            <el-option label="归档" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="showTransferDialog" title="转移所有权" width="400px">
      <el-form label-width="100px">
        <el-form-item label="新所有者ID">
          <el-input v-model="transferTargetId" placeholder="输入目标用户ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTransferDialog = false">取消</el-button>
        <el-button type="warning" @click="handleTransferOwnership" :disabled="!transferTargetId">确认转移</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRenameDialog" title="修改文档标题" width="400px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="新标题">
          <el-input v-model="renameTitle" placeholder="请输入新的文档标题" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRename" :disabled="!renameTitle.trim()">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getDocAdminDocuments, getDocAdminStats, getDocAdminMembers,
  addDocAdminMember, removeDocAdminMember, updateDocAdminMember,
  transferDocAdmin, updateDocStatus, updateDocument
} from '@/api/document'
import { searchUsers } from '@/api/auth'
import { Search, Plus, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const documents = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const stats = ref({})
const statCards = [
  { key: 'totalDocs', label: '文档总数' },
  { key: 'publicDocs', label: '公开文档' },
  { key: 'archivedDocs', label: '归档文档' },
  { key: 'totalMembers', label: '协作成员总数' }
]

const showMemberDialog = ref(false)
const showPermissionDialog = ref(false)
const showTransferDialog = ref(false)
const showAddMemberForm = ref(false)

const currentDocId = ref(null)
const currentDocTitle = ref('')
const currentDocMembers = ref([])
const currentDocPublic = ref(false)
const currentDocStatus = ref(1)
const transferTargetId = ref('')
const showRenameDialog = ref(false)
const renameTitle = ref('')

const memberSearchKeyword = ref('')
const memberSearchResults = ref([])
const selectedMemberUser = ref(null)
const newMemberPermission = ref('VIEW')
let memberSearchTimer = null

const fetchDocuments = async () => {
  loading.value = true
  try {
    const res = await getDocAdminDocuments({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined
    })
    documents.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getDocAdminStats()
    stats.value = res.data || {}
  } catch (e) { /* ignore */ }
}

const statusLabel = (s) => ({ 1: '正常', 2: '归档', 0: '已删除' }[s] || '未知')
const statusType = (s) => ({ 1: 'success', 2: 'warning', 0: 'danger' }[s] || 'info')

const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const openMemberDialog = async (doc) => {
  currentDocId.value = doc.id
  currentDocTitle.value = doc.title
  showAddMemberForm.value = false
  selectedMemberUser.value = null
  memberSearchKeyword.value = ''
  memberSearchResults.value = []
  try {
    const res = await getDocAdminMembers(doc.id)
    currentDocMembers.value = res.data || []
  } catch (e) {
    currentDocMembers.value = []
  }
  showMemberDialog.value = true
}

const openPermissionDialog = (doc) => {
  currentDocId.value = doc.id
  currentDocTitle.value = doc.title
  currentDocPublic.value = doc.isPublic === 1
  currentDocStatus.value = doc.status
  showPermissionDialog.value = true
}

const handleMemberSearch = () => {
  clearTimeout(memberSearchTimer)
  if (!memberSearchKeyword.value.trim()) {
    memberSearchResults.value = []
    return
  }
  memberSearchTimer = setTimeout(async () => {
    try {
      const res = await searchUsers(memberSearchKeyword.value.trim())
      memberSearchResults.value = res.data || []
    } catch (e) {
      memberSearchResults.value = []
    }
  }, 300)
}

const selectMemberUser = (u) => {
  selectedMemberUser.value = u
  memberSearchResults.value = []
  memberSearchKeyword.value = ''
}

const handleAddDocMember = async () => {
  if (!selectedMemberUser.value) return
  try {
    await addDocAdminMember(currentDocId.value, {
      userId: selectedMemberUser.value.id,
      permission: newMemberPermission.value
    })
    ElMessage.success('成员已添加')
    selectedMemberUser.value = null
    showAddMemberForm.value = false
    const res = await getDocAdminMembers(currentDocId.value)
    currentDocMembers.value = res.data || []
    fetchDocuments()
  } catch (e) { /* handled */ }
}

const handleUpdateMemberPerm = async (member, newPerm) => {
  try {
    await updateDocAdminMember(currentDocId.value, {
      userId: member.userId,
      permission: newPerm
    })
    ElMessage.success('权限已更新')
    const res = await getDocAdminMembers(currentDocId.value)
    currentDocMembers.value = res.data || []
  } catch (e) { /* handled */ }
}

const handleRemoveDocMember = async (member) => {
  await ElMessageBox.confirm('确定移除该成员？', '提示', { type: 'warning' })
  try {
    await removeDocAdminMember(currentDocId.value, member.userId)
    ElMessage.success('已移除')
    const res = await getDocAdminMembers(currentDocId.value)
    currentDocMembers.value = res.data || []
    fetchDocuments()
  } catch (e) { /* handled */ }
}

const handleToggleDocPublic = async (val) => {
  try {
    await updateDocument(currentDocId.value, { isPublic: !!val })
    ElMessage.success(val ? '已设为公开' : '已设为私有')
    currentDocPublic.value = val
    fetchDocuments()
  } catch (e) {
    currentDocPublic.value = !val
  }
}

const handleChangeDocStatus = async (val) => {
  try {
    await updateDocStatus(currentDocId.value, val)
    ElMessage.success('状态已更新')
    currentDocStatus.value = val
    fetchDocuments()
    fetchStats()
  } catch (e) { /* handled */ }
}

const handleRename = async () => {
  if (!renameTitle.value.trim()) return
  try {
    await updateDocument(currentDocId.value, { title: renameTitle.value.trim() })
    ElMessage.success('标题已修改')
    showRenameDialog.value = false
    fetchDocuments()
  } catch (e) { /* handled */ }
}

const handleAction = async (cmd, doc) => {
  if (cmd === 'rename') {
    currentDocId.value = doc.id
    renameTitle.value = doc.title || ''
    showRenameDialog.value = true
  } else if (cmd === 'edit') {
    router.push(`/editor/${doc.id}`)
  } else if (cmd === 'transfer') {
    currentDocId.value = doc.id
    currentDocTitle.value = doc.title
    transferTargetId.value = ''
    showTransferDialog.value = true
  } else if (cmd === 'archive') {
    await ElMessageBox.confirm('确定归档此文档？', '归档确认', { type: 'warning' })
    await updateDocStatus(doc.id, 2)
    ElMessage.success('已归档')
    fetchDocuments()
    fetchStats()
  } else if (cmd === 'restore') {
    await updateDocStatus(doc.id, 1)
    ElMessage.success('已恢复')
    fetchDocuments()
    fetchStats()
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm('确定删除此文档？此操作为软删除。', '删除确认', { type: 'warning' })
    await updateDocStatus(doc.id, 0)
    ElMessage.success('已删除')
    fetchDocuments()
    fetchStats()
  }
}

const handleTransferOwnership = async () => {
  if (!transferTargetId.value) return
  await ElMessageBox.confirm('确定转移此文档所有权？', '转移确认', { type: 'warning' })
  try {
    await transferDocAdmin(currentDocId.value, Number(transferTargetId.value))
    ElMessage.success('所有权已转移')
    showTransferDialog.value = false
    fetchDocuments()
  } catch (e) { /* handled */ }
}

onMounted(() => {
  fetchDocuments()
  fetchStats()
})
</script>

<style lang="scss" scoped>
.doc-manage {
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid var(--border-color);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.doc-table {
  border-radius: 8px;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.member-dialog-actions {
  margin-bottom: 16px;
}

.add-member-section {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-color);
}

.member-search-results {
  margin-top: 8px;
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: #fff;
}

.member-search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;

  &:hover {
    background: rgba(45, 184, 127, 0.06);
  }

  & + & {
    border-top: 1px solid var(--border-color);
  }
}

.selected-member-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.member-table {
  border-radius: 8px;
}
</style>
