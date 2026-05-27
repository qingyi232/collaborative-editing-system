import { defineStore } from 'pinia'
import {
  getDocumentList,
  getDocument,
  createDocument as createDocApi,
  updateDocument as updateDocApi,
  deleteDocument as deleteDocApi
} from '@/api/document'

export const useDocumentStore = defineStore('document', {
  state: () => ({
    documents: [],
    currentDoc: null,
    total: 0,
    pages: 0,
    onlineUsers: []
  }),

  actions: {
    async fetchDocuments(params = { page: 1, size: 10 }) {
      const res = await getDocumentList(params)
      this.documents = res.data.records
      this.total = res.data.total
      this.pages = res.data.pages
      return res
    },

    async fetchDocument(id) {
      const res = await getDocument(id)
      this.currentDoc = res.data
      return res
    },

    async createDocument(data) {
      return await createDocApi(data)
    },

    async updateDocument(id, data) {
      return await updateDocApi(id, data)
    },

    async deleteDocument(id) {
      return await deleteDocApi(id)
    },

    setOnlineUsers(users) {
      this.onlineUsers = users
    }
  }
})
