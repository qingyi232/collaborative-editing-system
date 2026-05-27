<template>
  <div class="system-config">
    <h2 class="page-title">系统配置</h2>

    <el-table :data="configs" v-loading="loading" stripe class="config-table">
      <el-table-column prop="configKey" label="配置项" width="240" />
      <el-table-column prop="configValue" label="当前值" min-width="200" />
      <el-table-column prop="description" label="说明" min-width="200" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openEdit(row)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEdit" title="编辑配置" width="450px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="配置项">
          <el-input :model-value="editForm.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置值">
          <el-input v-model="editForm.configValue" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getConfigs, updateConfig } from '@/api/admin'
import { Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const configs = ref([])
const showEdit = ref(false)

const editForm = reactive({
  configKey: '',
  configValue: ''
})

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await getConfigs()
    configs.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  editForm.configKey = row.configKey
  editForm.configValue = row.configValue
  showEdit.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateConfig({ configKey: editForm.configKey, configValue: editForm.configValue })
    ElMessage.success('保存成功')
    showEdit.value = false
    fetchConfigs()
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfigs)
</script>

<style lang="scss" scoped>
.system-config {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.config-table {
  border-radius: 8px;
  overflow: hidden;
}
</style>
