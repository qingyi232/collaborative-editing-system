import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue')
  },
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue')
      },
      {
        path: 'editor/:id',
        name: 'Editor',
        component: () => import('@/views/editor/Editor.vue')
      },
      {
        path: 'shared',
        name: 'SharedDocs',
        component: () => import('@/views/dashboard/Dashboard.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue')
      },
      {
        path: 'docadmin/manage',
        name: 'DocManage',
        component: () => import('@/views/docadmin/DocManage.vue'),
        meta: { role: 'DOC_ADMIN' }
      },
      {
        path: 'admin/logs',
        name: 'AuditLogs',
        component: () => import('@/views/admin/AuditLogs.vue'),
        meta: { role: 'SYS_ADMIN' }
      },
      {
        path: 'admin/config',
        name: 'SystemConfig',
        component: () => import('@/views/admin/SystemConfig.vue'),
        meta: { role: 'SYS_ADMIN' }
      },
      {
        path: 'admin/monitor',
        name: 'Monitor',
        component: () => import('@/views/admin/Monitor.vue'),
        meta: { role: 'SYS_ADMIN' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')

  if (to.matched.some(r => r.meta.requiresAuth)) {
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  if (to.matched.some(r => r.meta.role)) {
    const requiredRole = to.matched.find(r => r.meta.role)?.meta.role
    if (requiredRole) {
      const userRole = userInfo?.role
      const allowed =
        userRole === requiredRole ||
        (requiredRole === 'DOC_ADMIN' && (userRole === 'DOC_ADMIN' || userRole === 'SYS_ADMIN')) ||
        userRole === 'SYS_ADMIN'
      if (!allowed) {
        next('/dashboard')
        return
      }
    }
  }

  if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
    return
  }

  next()
})

export default router
