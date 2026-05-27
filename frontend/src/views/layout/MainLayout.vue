<template>
  <el-container class="main-layout">
    <el-header class="layout-header">
      <div class="header-left">
        <div class="logo" @click="$router.push('/dashboard')">
          <svg class="logo-svg" viewBox="0 0 32 32" fill="none">
            <rect x="4" y="5" width="17" height="22" rx="2" stroke="currentColor" stroke-width="2" fill="none"/>
            <rect x="11" y="9" width="17" height="22" rx="2" stroke="currentColor" stroke-width="2" fill="var(--primary-color)" opacity="0.15"/>
            <line x1="8" y1="12" x2="17" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="8" y1="16" x2="14" y2="16" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="8" y1="20" x2="16" y2="20" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="logo-text">协作编辑</span>
        </div>
      </div>
      <div class="header-right">
        <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
        <el-dropdown @command="handleCommand">
          <div class="avatar-wrapper">
            <el-avatar :size="32" class="user-avatar">
              {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0) }}
            </el-avatar>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><UserFilled /></el-icon>
                个人设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container class="layout-body">
      <el-aside class="layout-aside" :width="isCollapsed ? '64px' : '200px'">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          router
          class="side-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Document /></el-icon>
            <template #title>我的文档</template>
          </el-menu-item>
          <el-menu-item index="/shared">
            <el-icon><Share /></el-icon>
            <template #title>与我共享</template>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><UserFilled /></el-icon>
            <template #title>个人设置</template>
          </el-menu-item>
          <template v-if="isDocAdmin || userStore.isSysAdmin">
            <el-menu-item-group>
              <template #title><span v-show="!isCollapsed">文档管理</span></template>
              <el-menu-item index="/docadmin/manage">
                <el-icon><Folder /></el-icon>
                <template #title>文档管理</template>
              </el-menu-item>
            </el-menu-item-group>
          </template>
          <template v-if="userStore.isSysAdmin">
            <el-menu-item-group>
              <template #title><span v-show="!isCollapsed">系统管理</span></template>
              <el-menu-item index="/admin/logs">
                <el-icon><Notebook /></el-icon>
                <template #title>操作日志</template>
              </el-menu-item>
              <el-menu-item index="/admin/config">
                <el-icon><Setting /></el-icon>
                <template #title>系统配置</template>
              </el-menu-item>
              <el-menu-item index="/admin/monitor">
                <el-icon><Monitor /></el-icon>
                <template #title>资源监控</template>
              </el-menu-item>
            </el-menu-item-group>
          </template>
        </el-menu>
        <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon>
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  Document, Notebook, Setting, Monitor,
  ArrowDown, SwitchButton, Fold, Expand,
  Share, UserFilled, Folder
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)
const isDocAdmin = computed(() => userStore.userInfo?.role === 'DOC_ADMIN' || userStore.userInfo?.role === 'SYS_ADMIN')

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch (e) {
      userStore.logout()
    }
  }
})

const handleCommand = (cmd) => {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    userStore.logout()
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  background: var(--bg-color);
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  padding: 0 24px;
  height: 56px;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--primary-color);
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.85;
  }
}

.logo-svg {
  width: 28px;
  height: 28px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: var(--text-secondary);
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.user-avatar {
  background: var(--primary-color);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.arrow-icon {
  font-size: 12px;
  color: var(--text-secondary);
}

.layout-body {
  height: calc(100vh - 56px);
}

.layout-aside {
  background: #fff;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
}

.side-menu {
  flex: 1;
  border-right: none;
  padding-top: 8px;

  :deep(.el-menu-item) {
    border-radius: 6px;
    margin: 2px 8px;
    height: 44px;

    &.is-active {
      background: rgba(45, 184, 127, 0.08);
      color: var(--primary-color);
    }
  }

  :deep(.el-menu-item-group__title) {
    padding: 12px 0 4px 20px;
    font-size: 12px;
    color: var(--text-secondary);
  }
}

.collapse-btn {
  padding: 12px;
  text-align: center;
  cursor: pointer;
  border-top: 1px solid var(--border-color);
  color: var(--text-secondary);
  transition: color 0.2s;

  &:hover {
    color: var(--primary-color);
  }
}

.layout-main {
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-color);
}
</style>
