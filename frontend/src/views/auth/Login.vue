<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-bg-overlay"></div>
    <div class="login-container">
      <div class="login-left">
        <div class="brand-area">
          <div class="decorations">
            <div class="deco-circle deco-circle-1"></div>
            <div class="deco-circle deco-circle-2"></div>
            <div class="deco-line deco-line-1"></div>
            <div class="deco-line deco-line-2"></div>
            <div class="deco-dot deco-dot-1"></div>
            <div class="deco-dot deco-dot-2"></div>
            <div class="deco-dot deco-dot-3"></div>
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
            <p class="brand-slogan">在线协作，高效创作</p>
            <div class="feature-list">
              <div class="feature-item">
                <span class="feature-dot"></span>
                <span>实时多人协作编辑</span>
              </div>
              <div class="feature-item">
                <span class="feature-dot"></span>
                <span>完整的版本管理</span>
              </div>
              <div class="feature-item">
                <span class="feature-dot"></span>
                <span>灵活的权限控制</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="form-wrapper">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录你的账户以继续</p>
          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>还没有账户？</span>
            <router-link to="/register" class="link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 20px;
  overflow: hidden;
}

.login-bg {
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

.login-bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(45, 184, 127, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(54, 181, 160, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 60% 80%, rgba(45, 184, 127, 0.05) 0%, transparent 50%),
    linear-gradient(135deg, rgba(232, 245, 239, 0.85) 0%, rgba(240, 248, 244, 0.75) 50%, rgba(245, 247, 250, 0.80) 100%);
}

.login-container {
  position: relative;
  z-index: 2;
  display: flex;
  width: 900px;
  min-height: 520px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(45, 184, 127, 0.08);
  backdrop-filter: blur(20px);
}

.login-left {
  flex: 1;
  background: var(--primary-color);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-area {
  position: relative;
  z-index: 1;
  padding: 48px 40px;
  color: #fff;
}

.decorations {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.12);
}

.deco-circle-1 {
  width: 200px;
  height: 200px;
  top: -60px;
  right: -40px;
}

.deco-circle-2 {
  width: 120px;
  height: 120px;
  bottom: -30px;
  left: -20px;
}

.deco-line {
  position: absolute;
  background: rgba(255, 255, 255, 0.08);
}

.deco-line-1 {
  width: 1px;
  height: 80px;
  top: 60px;
  left: 30px;
}

.deco-line-2 {
  width: 60px;
  height: 1px;
  bottom: 80px;
  right: 20px;
}

.deco-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
}

.deco-dot-1 { top: 40px; right: 60px; }
.deco-dot-2 { bottom: 60px; left: 50px; }
.deco-dot-3 { top: 50%; right: 30px; }

.brand-content {
  position: relative;
  z-index: 1;
}

.logo-icon {
  width: 56px;
  height: 56px;
  margin-bottom: 24px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.brand-slogan {
  font-size: 16px;
  opacity: 0.85;
  margin-bottom: 40px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  opacity: 0.9;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  flex-shrink: 0;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 340px;
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
  margin-bottom: 32px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.form-footer {
  text-align: center;
  margin-top: 24px;
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
  .login-container {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
  }

  .login-left {
    padding: 32px;
    min-height: auto;
  }

  .brand-area {
    padding: 24px;
  }

  .feature-list {
    display: none;
  }
}
</style>
