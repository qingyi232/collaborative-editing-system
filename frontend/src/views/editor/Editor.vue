<template>
  <div class="editor-page">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button text @click="$router.push('/dashboard')">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="title-wrapper">
          <input
            v-model="docTitle"
            class="doc-title-input"
            placeholder="无标题文档"
            :disabled="!canEdit"
            @blur="saveTitle"
            @keyup.enter="($event) => $event.target.blur()"
          />
        </div>
      </div>
      <div class="toolbar-center">
        <el-tag v-if="!canEdit" type="info" size="small" effect="plain" style="margin-right: 8px;">
          只读模式
        </el-tag>
        <span v-if="saving" class="save-status saving">保存中...</span>
        <span v-else class="save-status saved">已保存</span>
      </div>
      <div class="toolbar-right">
        <div class="online-users">
          <el-tooltip
            v-for="(user, idx) in onlineUsers"
            :key="idx"
            :content="user.nickname || user.username"
            placement="bottom"
          >
            <el-avatar
              :size="28"
              :style="{ background: getUserColor(idx), marginLeft: idx > 0 ? '-6px' : '0' }"
              class="online-avatar"
            >
              {{ (user.nickname || user.username || 'U').charAt(0) }}
            </el-avatar>
          </el-tooltip>
        </div>
        <el-button v-if="canEdit" type="primary" size="small" @click="handleSaveVersion">
          <el-icon><Clock /></el-icon>
          保存版本
        </el-button>
        <el-button
          size="small"
          :type="showPanel ? 'primary' : 'default'"
          :plain="showPanel"
          @click="showPanel = !showPanel"
        >
          <el-icon><More /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="editor-body">
      <div class="editor-main">
        <div ref="editorContainer" class="quill-wrapper"></div>
      </div>

      <transition name="slide-panel">
        <div v-show="showPanel" class="editor-panel">
          <el-tabs v-model="activeTab" class="panel-tabs">
            <el-tab-pane label="成员" name="members">
              <div class="panel-section">
                <div v-if="canManage" class="panel-action">
                  <el-button size="small" type="primary" plain @click="showAddMember = true">
                    <el-icon><Plus /></el-icon>
                    添加成员
                  </el-button>
                </div>
                <div class="member-list">
                  <div v-for="m in members" :key="m.userId" class="member-item">
                    <el-avatar :size="32" class="member-avatar">
                      {{ (m.nickname || m.username || 'U').charAt(0) }}
                    </el-avatar>
                    <div class="member-info">
                      <span class="member-name">{{ m.nickname || m.username || '用户' + m.userId }}</span>
                      <el-tag size="small" :type="permissionTagType(m.permission)" effect="plain">
                        {{ permissionLabel(m.permission) }}
                      </el-tag>
                    </div>
                    <el-dropdown v-if="canManage && m.permission !== 'OWNER'" trigger="click" @command="(cmd) => handleMemberAction(cmd, m)">
                      <el-button text size="small">
                        <el-icon><More /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="setView">设为只读</el-dropdown-item>
                          <el-dropdown-item command="setEdit">设为可编辑</el-dropdown-item>
                          <el-dropdown-item command="setAdmin">设为管理员</el-dropdown-item>
                          <el-dropdown-item divided command="remove" style="color: #e5524a;">移除成员</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                  <el-empty v-if="members.length === 0" description="暂无成员" :image-size="60" />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="评论" name="comments">
              <div class="panel-section">
                <div class="comment-input">
                  <el-input
                    v-model="newComment"
                    type="textarea"
                    :rows="2"
                    placeholder="添加评论..."
                    resize="none"
                  />
                  <el-button
                    type="primary"
                    size="small"
                    :disabled="!newComment.trim()"
                    @click="handleAddComment"
                    style="margin-top: 8px; align-self: flex-end;"
                  >
                    发送
                  </el-button>
                </div>
                <div class="comment-list">
                  <div v-for="c in comments" :key="c.id" class="comment-item" :class="{ resolved: c.resolved }">
                    <div class="comment-header">
                      <el-avatar :size="24" class="comment-avatar">
                        {{ (c.nickname || 'U').charAt(0) }}
                      </el-avatar>
                      <span class="comment-author">{{ c.nickname || '匿名用户' }}</span>
                      <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                    </div>
                    <p class="comment-content">{{ c.content }}</p>
                    <div class="comment-actions">
                      <el-button v-if="!c.resolved" text size="small" @click="handleResolveComment(c.id)">
                        标记解决
                      </el-button>
                      <el-tag v-else size="small" type="success" effect="plain">已解决</el-tag>
                      <el-button text size="small" type="danger" @click="handleDeleteComment(c.id)">
                        删除
                      </el-button>
                    </div>
                  </div>
                  <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="60" />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="版本" name="versions">
              <div class="panel-section">
                <div class="version-list">
                  <div v-for="v in versions" :key="v.id" class="version-item">
                    <div class="version-header">
                      <span class="version-num">V{{ v.versionNumber || v.id }}</span>
                      <span class="version-time">{{ formatTime(v.createdAt) }}</span>
                    </div>
                    <p v-if="v.changeSummary" class="version-summary">{{ v.changeSummary }}</p>
                    <div class="version-actions">
                      <el-button text size="small" @click="handleViewVersion(v)">查看</el-button>
                      <el-button v-if="canEdit" text size="small" type="warning" @click="handleRollback(v)">回滚</el-button>
                    </div>
                  </div>
                  <el-empty v-if="versions.length === 0" description="暂无版本" :image-size="60" />
                </div>

                <div v-if="versions.length >= 2" class="version-compare">
                  <p class="compare-title">版本对比</p>
                  <div class="compare-selectors">
                    <el-select v-model="compareV1" placeholder="版本1" size="small">
                      <el-option v-for="v in versions" :key="v.id" :label="'V' + (v.versionNumber || v.id)" :value="v.id" />
                    </el-select>
                    <span class="compare-vs">vs</span>
                    <el-select v-model="compareV2" placeholder="版本2" size="small">
                      <el-option v-for="v in versions" :key="v.id" :label="'V' + (v.versionNumber || v.id)" :value="v.id" />
                    </el-select>
                    <el-button size="small" type="primary" plain @click="handleCompare" :disabled="!compareV1 || !compareV2">
                      对比
                    </el-button>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="设置" name="settings">
              <div class="panel-section">
                <div class="settings-group">
                  <h4 class="settings-title">文档设置</h4>
                  <div class="settings-item">
                    <span class="settings-label">公开访问</span>
                    <el-switch v-model="docPublic" :disabled="!canManage" @change="handleTogglePublic" />
                  </div>
                  <div class="settings-item">
                    <span class="settings-label">文档大小</span>
                    <span class="settings-value">{{ formatSize(currentDoc?.docSize || 0) }}</span>
                  </div>
                  <div class="settings-item">
                    <span class="settings-label">当前版本</span>
                    <span class="settings-value">V{{ currentDoc?.currentVersion || 1 }}</span>
                  </div>
                  <div class="settings-item">
                    <span class="settings-label">创建时间</span>
                    <span class="settings-value">{{ formatTime(currentDoc?.createdAt) }}</span>
                  </div>
                  <div class="settings-item">
                    <span class="settings-label">我的权限</span>
                    <el-tag size="small" :type="permissionTagType(myPermission)" effect="plain">
                      {{ permissionLabel(myPermission) }}
                    </el-tag>
                  </div>
                </div>
                <template v-if="canManage">
                  <el-divider />
                  <div class="settings-group">
                    <h4 class="settings-title">所有权管理</h4>
                    <div class="transfer-form">
                      <el-input v-model="transferUserId" placeholder="输入新所有者的用户ID" size="small" />
                      <el-button type="warning" size="small" @click="handleTransfer" :disabled="!transferUserId">
                        转移所有权
                      </el-button>
                    </div>
                  </div>
                </template>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </transition>
    </div>

    <el-dialog v-model="showAddMember" title="邀请成员" width="460px" @open="searchKeyword = ''">
      <el-form :model="addMemberForm" label-width="80px">
        <el-form-item label="搜索用户">
          <el-input
            v-model="searchKeyword"
            placeholder="输入用户名或昵称搜索"
            clearable
            @input="handleSearchUser"
          />
          <div v-if="searchResults.length > 0" class="search-result-list">
            <div
              v-for="u in searchResults"
              :key="u.id"
              class="search-result-item"
              :class="{ active: addMemberForm.userId === u.id }"
              @click="selectUser(u)"
            >
              <el-avatar :size="28" class="search-avatar">{{ (u.nickname || u.username || 'U').charAt(0) }}</el-avatar>
              <div class="search-user-info">
                <span class="search-nickname">{{ u.nickname || u.username }}</span>
                <span class="search-username">@{{ u.username }}</span>
              </div>
              <el-icon v-if="addMemberForm.userId === u.id" class="search-check" color="#2DB87F"><Check /></el-icon>
            </div>
          </div>
          <div v-else-if="searchKeyword && !searchLoading" class="search-empty">未找到匹配用户</div>
        </el-form-item>
        <el-form-item v-if="selectedUser" label="已选用户">
          <el-tag type="success" closable @close="clearSelectedUser">
            {{ selectedUser.nickname || selectedUser.username }} (@{{ selectedUser.username }})
          </el-tag>
        </el-form-item>
        <el-form-item label="权限">
          <el-select v-model="addMemberForm.permission" style="width: 100%">
            <el-option label="只读 (VIEW)" value="VIEW" />
            <el-option label="可编辑 (EDIT)" value="EDIT" />
            <el-option label="管理员 (ADMIN)" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMember = false">取消</el-button>
        <el-button type="primary" :disabled="!addMemberForm.userId" @click="handleAddMember">邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSaveVersion" title="保存版本" width="400px">
      <el-input v-model="versionSummary" type="textarea" :rows="3" placeholder="请输入版本说明（可选）" />
      <template #footer>
        <el-button @click="showSaveVersion = false">取消</el-button>
        <el-button type="primary" @click="confirmSaveVersion">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showVersionDetail" title="版本详情" width="600px">
      <div v-if="versionDetail" class="version-detail-content" v-html="versionDetail.content"></div>
    </el-dialog>

    <el-dialog v-model="showCompareResult" title="版本对比" width="700px">
      <div v-if="compareData" class="compare-panels">
        <div class="compare-panel">
          <h4>V{{ compareData.version1?.versionNumber }} <span class="compare-time">{{ formatTime(compareData.version1?.createdAt) }}</span></h4>
          <div class="compare-content" v-html="compareData.version1?.content || '(空)'"></div>
        </div>
        <div class="compare-panel">
          <h4>V{{ compareData.version2?.versionNumber }} <span class="compare-time">{{ formatTime(compareData.version2?.createdAt) }}</span></h4>
          <div class="compare-content" v-html="compareData.version2?.content || '(空)'"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDocumentStore } from '@/store/document'
import { useUserStore } from '@/store/user'
import { CollabSocket } from '@/utils/websocket'
import {
  getMemberList, addMember, removeMember, updateMember,
  getCommentList, addComment, resolveComment, deleteComment,
  getVersionList, createVersion, getVersion, compareVersions, rollbackVersion,
  updateDocument, transferDocument
} from '@/api/document'
import { searchUsers } from '@/api/auth'
import {
  ArrowLeft, Clock, More, Plus, Check
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

const route = useRoute()
const router = useRouter()
const docStore = useDocumentStore()
const userStore = useUserStore()
const docId = route.params.id

const editorContainer = ref(null)
let quill = null
let socket = null
let serverVersion = 0

const docTitle = ref('')
const saving = ref(false)
const showPanel = ref(true)
const activeTab = ref('members')
const currentDoc = ref(null)
const docPublic = ref(false)
const transferUserId = ref('')
const myPermission = ref('VIEW')

const canEdit = computed(() => {
  return ['OWNER', 'ADMIN', 'EDIT'].includes(myPermission.value)
})

const canManage = computed(() => {
  return ['OWNER', 'ADMIN'].includes(myPermission.value)
})

const onlineUsers = ref([])
const members = ref([])
const comments = ref([])
const versions = ref([])
const newComment = ref('')

const showAddMember = ref(false)
const addMemberForm = reactive({ userId: null, permission: 'VIEW' })
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const selectedUser = ref(null)
let searchTimer = null

const showSaveVersion = ref(false)
const versionSummary = ref('')

const showVersionDetail = ref(false)
const versionDetail = ref(null)

const showCompareResult = ref(false)
const compareData = ref(null)
const compareV1 = ref(null)
const compareV2 = ref(null)

const userColors = [
  '#2DB87F', '#36B5A0', '#F0B429', '#E5524A',
  '#9B59B6', '#3498DB', '#E67E22', '#1ABC9C'
]

const getUserColor = (idx) => userColors[idx % userColors.length]

const permissionLabel = (p) => {
  const map = { OWNER: '所有者', ADMIN: '管理员', EDIT: '可编辑', VIEW: '只读' }
  return map[p] || p
}

const permissionTagType = (p) => {
  const map = { OWNER: 'danger', ADMIN: 'warning', EDIT: 'success', VIEW: 'info' }
  return map[p] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const resolveMyPermission = (doc) => {
  const userId = userStore.userInfo?.id
  if (!userId || !doc) return 'VIEW'
  if (doc.ownerId === userId) return 'OWNER'

  const member = members.value.find(m => m.userId === userId)
  if (member) return member.permission
  return 'VIEW'
}

const initEditor = () => {
  const toolbarOptions = canEdit.value ? [
    [{ header: [1, 2, 3, false] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ color: [] }, { background: [] }],
    [{ list: 'ordered' }, { list: 'bullet' }],
    [{ align: [] }],
    ['blockquote', 'code-block'],
    ['link', 'image'],
    ['clean']
  ] : false

  quill = new Quill(editorContainer.value, {
    theme: 'snow',
    placeholder: canEdit.value ? '开始输入内容...' : '只读模式',
    readOnly: !canEdit.value,
    modules: {
      toolbar: toolbarOptions
    }
  })

  quill.on('text-change', (delta, oldDelta, source) => {
    if (source === 'user' && socket && canEdit.value) {
      socket.sendOperation(delta.ops, serverVersion)
      debouncedSave()
    }
  })

  quill.on('selection-change', (range) => {
    if (range && socket) {
      socket.sendCursor(range)
    }
  })
}

let saveTimer = null
const debouncedSave = () => {
  if (!canEdit.value) return
  clearTimeout(saveTimer)
  saveTimer = setTimeout(async () => {
    saving.value = true
    try {
      const content = JSON.stringify(quill.getContents())
      await updateDocument(docId, { title: docTitle.value, content })
    } catch (e) {
      if (e?.message?.includes('无权')) {
        ElMessage.error('您没有编辑权限，无法保存')
      }
    } finally {
      saving.value = false
    }
  }, 2000)
}

const saveTitle = () => {
  if (canEdit.value) {
    debouncedSave()
  }
}

const connectWebSocket = () => {
  const token = localStorage.getItem('token')
  socket = new CollabSocket(docId, token)

  socket.onMessage('operation', (data) => {
    if (data?.ops) {
      quill.updateContents(data.ops, 'api')
    }
    if (data?.version) {
      serverVersion = data.version
    }
  })

  socket.onMessage('ack', (data) => {
    if (data?.version) {
      serverVersion = data.version
    }
  })

  socket.onMessage('cursor', () => {})

  socket.onMessage('userJoin', (data) => {
    if (data?.onlineUsers) {
      onlineUsers.value = Array.from(data.onlineUsers).map(name => ({ username: name, nickname: name }))
    }
  })

  socket.onMessage('userLeave', (data) => {
    if (data?.onlineUsers) {
      onlineUsers.value = Array.from(data.onlineUsers).map(name => ({ username: name, nickname: name }))
    }
  })

  socket.onMessage('sync', (data) => {
    if (data?.onlineUsers) {
      onlineUsers.value = Array.from(data.onlineUsers).map(name => ({ username: name, nickname: name }))
    }
    if (data?.currentVersion !== undefined) {
      serverVersion = data.currentVersion
    }
    if (data?.permission) {
      myPermission.value = data.permission
    }
  })

  socket.onMessage('error', (data) => {
    if (data?.message) {
      ElMessage.error(data.message)
    }
  })

  socket.connect()
}

const fetchMembers = async () => {
  try {
    const res = await getMemberList(docId)
    members.value = res.data || []
  } catch (e) { /* ignore */ }
}

const fetchComments = async () => {
  try {
    const res = await getCommentList(docId)
    comments.value = res.data || []
  } catch (e) { /* ignore */ }
}

const fetchVersions = async () => {
  try {
    const res = await getVersionList(docId, { page: 1, size: 50 })
    versions.value = res.data?.records || res.data || []
  } catch (e) { /* ignore */ }
}

const handleSearchUser = () => {
  clearTimeout(searchTimer)
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    searchLoading.value = true
    try {
      const res = await searchUsers(searchKeyword.value.trim())
      searchResults.value = res.data || []
    } catch (e) {
      searchResults.value = []
    } finally {
      searchLoading.value = false
    }
  }, 300)
}

const selectUser = (u) => {
  addMemberForm.userId = u.id
  selectedUser.value = u
  searchResults.value = []
  searchKeyword.value = ''
}

const clearSelectedUser = () => {
  addMemberForm.userId = null
  selectedUser.value = null
}

const handleAddMember = async () => {
  if (!addMemberForm.userId) {
    ElMessage.warning('请先搜索并选择用户')
    return
  }
  try {
    await addMember(docId, { userId: Number(addMemberForm.userId), permission: addMemberForm.permission })
    ElMessage.success('邀请成功')
    showAddMember.value = false
    addMemberForm.userId = null
    addMemberForm.permission = 'VIEW'
    selectedUser.value = null
    searchKeyword.value = ''
    fetchMembers()
  } catch (e) { /* handled */ }
}

const handleMemberAction = async (cmd, member) => {
  if (cmd === 'remove') {
    try {
      await ElMessageBox.confirm('确定移除该成员？', '提示', { type: 'warning' })
      await removeMember(docId, member.userId)
      ElMessage.success('已移除')
      fetchMembers()
    } catch (e) {
      if (e !== 'cancel') console.error(e)
    }
  } else {
    const permMap = { setView: 'VIEW', setEdit: 'EDIT', setAdmin: 'ADMIN' }
    const newPerm = permMap[cmd]
    if (newPerm && newPerm !== member.permission) {
      try {
        await updateMember(docId, { userId: member.userId, permission: newPerm })
        ElMessage.success('权限已更新')
        fetchMembers()
      } catch (e) { /* handled */ }
    }
  }
}

const handleRemoveMember = async (userId) => {
  try {
    await ElMessageBox.confirm('确定移除该成员？', '提示', { type: 'warning' })
    await removeMember(docId, userId)
    ElMessage.success('已移除')
    fetchMembers()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleAddComment = async () => {
  if (!newComment.value.trim()) return
  try {
    await addComment(docId, { content: newComment.value })
    newComment.value = ''
    ElMessage.success('评论已添加')
    fetchComments()
  } catch (e) { /* handled by interceptor */ }
}

const handleResolveComment = async (commentId) => {
  try {
    await resolveComment(docId, commentId)
    fetchComments()
  } catch (e) { /* handled by interceptor */ }
}

const handleDeleteComment = async (commentId) => {
  try {
    await deleteComment(docId, commentId)
    fetchComments()
  } catch (e) { /* handled by interceptor */ }
}

const handleSaveVersion = () => {
  versionSummary.value = ''
  showSaveVersion.value = true
}

const confirmSaveVersion = async () => {
  try {
    await createVersion(docId, { changeSummary: versionSummary.value || '手动保存' })
    ElMessage.success('版本已保存')
    showSaveVersion.value = false
    fetchVersions()
  } catch (e) { /* handled */ }
}

const handleViewVersion = async (v) => {
  try {
    const res = await getVersion(docId, v.id)
    versionDetail.value = res.data
    showVersionDetail.value = true
  } catch (e) { /* handled */ }
}

const handleRollback = async (v) => {
  await ElMessageBox.confirm('确定要回滚到该版本吗？当前内容将被覆盖。', '回滚确认', { type: 'warning' })
  await rollbackVersion(docId, v.id)
  ElMessage.success('回滚成功')
  fetchVersions()
  const res = await docStore.fetchDocument(docId)
  currentDoc.value = res.data
  if (res.data?.content) {
    try {
      const delta = JSON.parse(res.data.content)
      quill.setContents(delta, 'api')
    } catch {
      quill.setText(res.data.content, 'api')
    }
  }
}

const handleCompare = async () => {
  if (!compareV1.value || !compareV2.value) return
  try {
    const res = await compareVersions(docId, compareV1.value, compareV2.value)
    compareData.value = res.data
    showCompareResult.value = true
  } catch (e) { /* handled */ }
}

const handleTogglePublic = async (val) => {
  try {
    await updateDocument(docId, { isPublic: !!val })
    ElMessage.success(val ? '已设为公开' : '已设为私有')
  } catch (e) {
    docPublic.value = !val
  }
}

const handleTransfer = async () => {
  if (!transferUserId.value) return
  await ElMessageBox.confirm('转移后您将失去文档所有权，确定继续？', '转移所有权', { type: 'warning' })
  try {
    await transferDocument(docId, Number(transferUserId.value))
    ElMessage.success('所有权已转移')
    transferUserId.value = ''
    fetchMembers()
  } catch (e) { /* handled */ }
}

onMounted(async () => {
  try {
    const res = await docStore.fetchDocument(docId)
    currentDoc.value = res.data
    docTitle.value = res.data?.title || ''
    docPublic.value = res.data?.isPublic === 1 || res.data?.isPublic === true

    const userId = userStore.userInfo?.id
    if (res.data?.ownerId === userId) {
      myPermission.value = 'OWNER'
    }

    await fetchMembers()
    const perm = resolveMyPermission(res.data)
    myPermission.value = perm

    await nextTick()
    initEditor()

    if (res.data?.content) {
      try {
        const delta = JSON.parse(res.data.content)
        quill.setContents(delta, 'api')
      } catch {
        quill.setText(res.data.content, 'api')
      }
    }

    connectWebSocket()
    fetchComments()
    fetchVersions()
  } catch (e) {
    ElMessage.error('加载文档失败')
    router.push('/dashboard')
  }
})

onBeforeUnmount(() => {
  clearTimeout(saveTimer)
  if (socket) socket.close()
})
</script>

<style lang="scss" scoped>
.editor-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  margin: -24px;
  background: #fff;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-color);
  background: #fff;
  z-index: 5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.title-wrapper {
  flex: 1;
  max-width: 400px;
}

.doc-title-input {
  border: none;
  outline: none;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  width: 100%;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;

  &:hover, &:focus {
    background: var(--bg-color);
  }

  &::placeholder {
    color: #ccc;
  }

  &:disabled {
    cursor: default;
    opacity: 0.8;
  }
}

.toolbar-center {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.save-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;

  &.saving {
    color: var(--warning-color);
    background: rgba(240, 180, 41, 0.1);
  }

  &.saved {
    color: var(--success-color);
    background: rgba(45, 184, 127, 0.1);
  }
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.online-users {
  display: flex;
  align-items: center;
  padding-left: 6px;
}

.online-avatar {
  border: 2px solid #fff;
  font-size: 12px;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-main {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.quill-wrapper {
  flex: 1;

  :deep(.ql-toolbar) {
    border: none;
    border-bottom: 1px solid var(--border-color);
    background: #fafbfc;
  }

  :deep(.ql-container) {
    border: none;
    font-size: 15px;
    line-height: 1.8;
  }

  :deep(.ql-editor) {
    padding: 32px 48px;
    min-height: 100%;

    &.ql-blank::before {
      color: #ccc;
      font-style: normal;
    }
  }
}

.editor-panel {
  width: 340px;
  border-left: 1px solid var(--border-color);
  background: #fff;
  overflow-y: auto;
  flex-shrink: 0;
}

.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: all 0.3s ease;
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  width: 0;
  opacity: 0;
}

.panel-tabs {
  height: 100%;

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 12px;
    border-bottom: 1px solid var(--border-color);
  }

  :deep(.el-tabs__content) {
    padding: 0;
    height: calc(100% - 40px);
    overflow-y: auto;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.panel-section { padding: 16px; }
.panel-action { margin-bottom: 12px; }

.member-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover { background: var(--bg-color); }
}

.member-avatar {
  background: var(--secondary-color);
  color: #fff;
  font-size: 13px;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-name {
  font-size: 14px;
  color: var(--text-primary);
}

.comment-input {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-color);

  &.resolved { opacity: 0.6; }
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-avatar {
  background: var(--primary-color);
  color: #fff;
  font-size: 11px;
  flex-shrink: 0;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.comment-time {
  font-size: 11px;
  color: var(--text-secondary);
  margin-left: auto;
}

.comment-content {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
  margin: 0;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.version-item {
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: border-color 0.2s;

  &:hover { border-color: var(--primary-color); }
}

.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.version-num {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
}

.version-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.version-summary {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 4px 0;
}

.version-actions {
  display: flex;
  gap: 4px;
}

.version-compare {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.compare-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.compare-selectors {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .el-select { width: 90px; }
}

.compare-vs {
  font-size: 12px;
  color: var(--text-secondary);
}

.version-detail-content {
  padding: 16px;
  background: var(--bg-color);
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.8;
}

.compare-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  h4 {
    font-size: 14px;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  .compare-time {
    font-size: 12px;
    font-weight: 400;
    color: var(--text-secondary);
  }
}

.compare-content {
  padding: 12px;
  background: var(--bg-color);
  border-radius: 8px;
  max-height: 350px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
}

.settings-group {
  margin-bottom: 8px;
}

.settings-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 14px;
}

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);

  &:last-child { border-bottom: none; }
}

.settings-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.settings-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.transfer-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-result-list {
  margin-top: 8px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(45, 184, 127, 0.06);
  }

  &.active {
    background: rgba(45, 184, 127, 0.1);
  }

  & + & {
    border-top: 1px solid var(--border-color);
  }
}

.search-avatar {
  background: var(--primary-color);
  color: #fff;
  font-size: 12px;
  flex-shrink: 0;
}

.search-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.search-nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.search-username {
  font-size: 12px;
  color: var(--text-secondary);
}

.search-check {
  flex-shrink: 0;
}

.search-empty {
  margin-top: 8px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 12px;
}
</style>
