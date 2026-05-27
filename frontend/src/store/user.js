import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),

  getters: {
    isLoggedIn: state => !!state.token,
    role: state => state.userInfo?.role || '',
    isSysAdmin: state => state.userInfo?.role === 'SYS_ADMIN',
    isDocAdmin: state => state.userInfo?.role === 'DOC_ADMIN' || state.userInfo?.role === 'SYS_ADMIN'
  },

  actions: {
    async login(credentials) {
      const res = await loginApi(credentials)
      const data = res.data
      this.token = data.token
      this.userInfo = {
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        role: data.role,
        avatar: data.avatar
      }
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return res
    },

    async register(data) {
      return await registerApi(data)
    },

    async getUserInfo() {
      const res = await getUserInfoApi()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return res
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
  }
})
