<template>
  <div class="register-page">
    <div class="register-bg"></div>
    <div class="register-bg-overlay"></div>
    <div class="register-container">
      <div class="register-left">
        <div class="brand-area">
          <div class="decorations">
            <div class="deco-circle deco-circle-1"></div>
            <div class="deco-circle deco-circle-2"></div>
            <div class="deco-dot deco-dot-1"></div>
            <div class="deco-dot deco-dot-2"></div>
          </div>
          <div class="brand-content">
            <div class="logo-icon">
              <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="6" y="8" width="26" height="32" rx="3" stroke="#fff" stroke-width="2.5" fill="none"/>
                <rect x="16" y="14" width="26" height="32" rx="3" stroke="#fff" stroke-width="2.5" fill="rgba(255,255,255,0.15)"/>
                <line x1="12" y1="18" x2="26" y2="18" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                <line x1="12" y1="24" x2="22" y2="24" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                <line x1="12" y1="30" x2="24" y2="30" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <h1 class="brand-title">协作编辑系统</h1>
            <p class="brand-slogan">创建账户，开始协作</p>
          </div>
        </div>
      </div>
      <div class="register-right">
        <div class="form-wrapper">
          <h2 class="form-title">创建账户</h2>
          <p class="form-subtitle">填写信息完成注册</p>
          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="UserFilled" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>已有账户？</span>
            <router-link to="/login" class="link">返回登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { User, UserFilled, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { confirmPassword, ...data } = form
    await userStore.register(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 20px;
  overflow: hidden;
}

.register-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    url('https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=1920&q=80&auto=format&fit=crop') center/cover no-repeat;
  filter: blur(3px);
  transform: scale(1.05);
  animation: bgFadeIn 1s ease-out;
}

@keyframes bgFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.register-bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(54, 181, 160, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(45, 184, 127, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 60% 80%, rgba(54, 181, 160, 0.05) 0%, transparent 50%),
    linear-gradient(135deg, rgba(232, 245, 239, 0.85) 0%, rgba(240, 248, 244, 0.75) 50%, rgba(245, 247, 250, 0.80) 100%);
}

.register-container {
  position: relative;
  z-index: 2;
  display: flex;
  width: 900px;
  min-height: 580px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(45, 184, 127, 0.08);
  backdrop-filter: blur(20px);
}

.register-left {
  flex: 0 0 340px;
  background: var(--secondary-color);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-area {
  position: relative;
  z-index: 1;
  padding: 48px 36px;
  color: #fff;
}

.decorations {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.12);
}

.deco-circle-1 {
  width: 180px;
  height: 180px;
  top: -50px;
  right: -50px;
}

.deco-circle-2 {
  width: 100px;
  height: 100px;
  bottom: -20px;
  left: -20px;
}

.deco-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
}

.deco-dot-1 { top: 40px; left: 40px; }
.deco-dot-2 { bottom: 60px; right: 40px; }

.brand-content {
  position: relative;
  z-index: 1;
}

.logo-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 20px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.brand-slogan {
  font-size: 15px;
  opacity: 0.85;
}

.register-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
}

.form-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 28px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-secondary);

  .link {
    color: var(--primary-color);
    text-decoration: none;
    margin-left: 4px;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
  }

  .register-left {
    flex: none;
    padding: 24px;
  }
}
</style>
