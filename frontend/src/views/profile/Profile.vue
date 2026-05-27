<template>
  <div class="profile-page">
    <h2 class="page-title">个人设置</h2>

    <div class="profile-sections">
      <div class="profile-card">
        <div class="card-header">
          <h3>基本信息</h3>
        </div>
        <div class="card-body">
          <div class="avatar-section">
            <el-avatar :size="72" class="profile-avatar">
              {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0) }}
            </el-avatar>
            <div class="avatar-info">
              <p class="avatar-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</p>
              <p class="avatar-role">
                <el-tag size="small" :type="roleTagType" effect="plain">{{ roleLabel }}</el-tag>
              </p>
            </div>
          </div>
          <el-form :model="profileForm" label-width="80px" class="profile-form">
            <el-form-item label="用户名">
              <el-input :model-value="userStore.userInfo?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="profile-card">
        <div class="card-header">
          <h3>修改密码</h3>
        </div>
        <div class="card-body">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" class="profile-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6个字符" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changingPwd" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { updateProfile, changePassword } from '@/api/auth'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const saving = ref(false)
const changingPwd = ref(false)
const pwdFormRef = ref(null)

const profileForm = reactive({
  nickname: '',
  email: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const roleLabel = computed(() => {
  const map = { USER: '普通用户', DOC_ADMIN: '文档管理员', SYS_ADMIN: '系统管理员' }
  return map[userStore.userInfo?.role] || '普通用户'
})

const roleTagType = computed(() => {
  const map = { USER: 'info', DOC_ADMIN: 'warning', SYS_ADMIN: 'danger' }
  return map[userStore.userInfo?.role] || 'info'
})

const handleSaveProfile = async () => {
  saving.value = true
  try {
    const res = await updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email
    })
    userStore.userInfo = { ...userStore.userInfo, nickname: profileForm.nickname, email: profileForm.email }
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    ElMessage.success('保存成功')
  } catch (e) {} finally {
    saving.value = false
  }
}

const handleChangePwd = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPwd.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    setTimeout(() => userStore.logout(), 1500)
  } catch (e) {} finally {
    changingPwd.value = false
  }
}

onMounted(async () => {
  try {
    await userStore.getUserInfo()
  } catch (e) { /* fallback to cached */ }
  profileForm.nickname = userStore.userInfo?.nickname || ''
  profileForm.email = userStore.userInfo?.email || ''
})
</script>

<style lang="scss" scoped>
.profile-page {
  max-width: 720px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.profile-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.card-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }
}

.card-body {
  padding: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.profile-avatar {
  background: var(--primary-color);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
}

.avatar-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.avatar-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.avatar-role {
  margin: 0;
}

.profile-form {
  max-width: 420px;
}
</style>
