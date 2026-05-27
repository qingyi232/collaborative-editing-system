<template>
  <div class="audit-logs">
    <h2 class="page-title">操作日志</h2>

    <div class="filter-bar">
      <el-input
        v-model="filters.username"
        placeholder="搜索用户名"
        clearable
        style="width: 160px"
        :prefix-icon="User"
      />
      <el-select v-model="filters.action" placeholder="操作类型" clearable style="width: 160px">
        <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.targetType" placeholder="目标类型" clearable style="width: 160px">
        <el-option label="文档" value="DOCUMENT" />
        <el-option label="用户" value="USER" />
        <el-option label="系统" value="SYSTEM" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 280px"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button @click="handleReset">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </div>

    <el-table :data="logs" v-loading="loading" stripe class="log-table">
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="action" label="操作类型" width="130">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.action)" size="small" effect="plain">
            {{ actionLabel(row.action) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetType" label="目标类型" width="100">
        <template #default="{ row }">
          {{ targetTypeLabel(row.targetType) }}
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="250" show-overflow-tooltip />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLogs, exportLogs } from '@/api/admin'
import { Search, Refresh, Download, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const dateRange = ref(null)

const filters = reactive({
  action: '',
  targetType: '',
  username: ''
})

const actionOptions = [
  { label: '登录', value: 'LOGIN' },
  { label: '注册', value: 'REGISTER' },
  { label: '创建', value: 'CREATE' },
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '导出', value: 'EXPORT' },
  { label: '回滚', value: 'ROLLBACK' },
  { label: '转让所有权', value: 'TRANSFER' },
  { label: '添加成员', value: 'ADD_MEMBER' },
  { label: '移除成员', value: 'REMOVE_MEMBER' },
  { label: '修改权限', value: 'UPDATE_PERMISSION' },
  { label: '修改资料', value: 'UPDATE_PROFILE' },
  { label: '修改密码', value: 'CHANGE_PASSWORD' },
  { label: '修改配置', value: 'UPDATE_CONFIG' },
  { label: '修改状态', value: 'UPDATE_STATUS' }
]

const actionLabelMap = {}
actionOptions.forEach(item => { actionLabelMap[item.value] = item.label })

const actionLabel = (action) => actionLabelMap[action] || action

const targetTypeLabelMap = {
  DOCUMENT: '文档',
  USER: '用户',
  SYSTEM: '系统'
}
const targetTypeLabel = (type) => targetTypeLabelMap[type] || type

const buildParams = () => {
  const params = {
    action: filters.action,
    targetType: filters.targetType,
    username: filters.username,
    page: currentPage.value,
    size: pageSize.value
  }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await getLogs(buildParams())
    logs.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchLogs()
}

const handleReset = () => {
  filters.action = ''
  filters.targetType = ''
  filters.username = ''
  dateRange.value = null
  currentPage.value = 1
  fetchLogs()
}

const handleExport = async () => {
  try {
    const params = buildParams()
    delete params.page
    delete params.size
    const res = await exportLogs(params)
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const actionTagType = (action) => {
  const map = {
    CREATE: 'success',
    DELETE: 'danger',
    UPDATE: 'warning',
    LOGIN: 'info',
    REGISTER: 'info',
    ROLLBACK: 'danger',
    TRANSFER: 'warning',
    ADD_MEMBER: 'success',
    REMOVE_MEMBER: 'danger',
    UPDATE_PERMISSION: 'warning',
    UPDATE_PROFILE: '',
    CHANGE_PASSWORD: '',
    UPDATE_CONFIG: 'warning',
    UPDATE_STATUS: 'warning',
    EXPORT: ''
  }
  return map[action] || ''
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(fetchLogs)
</script>

<style lang="scss" scoped>
.audit-logs {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.log-table {
  border-radius: 8px;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
