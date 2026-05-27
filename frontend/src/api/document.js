import request from './request'

export function createDocument(data) {
  return request.post('/doc', data)
}

export function getDocument(id) {
  return request.get(`/doc/${id}`)
}

export function updateDocument(id, data) {
  return request.put(`/doc/${id}`, data)
}

export function deleteDocument(id) {
  return request.delete(`/doc/${id}`)
}

export function getDocumentList(params) {
  return request.get('/doc/list', { params })
}

export function transferDocument(id, newOwnerId) {
  return request.post(`/doc/${id}/transfer`, { newOwnerId })
}

export function addMember(docId, data) {
  return request.post(`/doc/${docId}/member`, data)
}

export function removeMember(docId, userId) {
  return request.delete(`/doc/${docId}/member/${userId}`)
}

export function updateMember(docId, data) {
  return request.put(`/doc/${docId}/member`, data)
}

export function getMemberList(docId) {
  return request.get(`/doc/${docId}/member/list`)
}

export function createVersion(docId, data) {
  return request.post(`/doc/${docId}/version`, data)
}

export function getVersionList(docId, params) {
  return request.get(`/doc/${docId}/version/list`, { params })
}

export function getVersion(docId, versionId) {
  return request.get(`/doc/${docId}/version/${versionId}`)
}

export function compareVersions(docId, v1, v2) {
  return request.get(`/doc/${docId}/version/compare`, { params: { v1, v2 } })
}

export function rollbackVersion(docId, versionId) {
  return request.post(`/doc/${docId}/version/${versionId}/rollback`)
}

export function addComment(docId, data) {
  return request.post(`/doc/${docId}/comment`, data)
}

export function getCommentList(docId) {
  return request.get(`/doc/${docId}/comment/list`)
}

export function resolveComment(docId, commentId) {
  return request.put(`/doc/${docId}/comment/${commentId}/resolve`)
}

export function deleteComment(docId, commentId) {
  return request.delete(`/doc/${docId}/comment/${commentId}`)
}

export function getSharedDocuments(params) {
  return request.get('/doc/shared', { params })
}

export function getOwnDocuments(params) {
  return request.get('/doc/own', { params })
}

export function getDocumentStats() {
  return request.get('/doc/stats')
}

export function getDocAdminDocuments(params) {
  return request.get('/docadmin/documents', { params })
}

export function getDocAdminStats() {
  return request.get('/docadmin/stats')
}

export function getDocAdminMembers(docId) {
  return request.get(`/docadmin/documents/${docId}/members`)
}

export function addDocAdminMember(docId, data) {
  return request.post(`/docadmin/documents/${docId}/members`, data)
}

export function removeDocAdminMember(docId, userId) {
  return request.delete(`/docadmin/documents/${docId}/members/${userId}`)
}

export function updateDocAdminMember(docId, data) {
  return request.put(`/docadmin/documents/${docId}/members`, data)
}

export function transferDocAdmin(docId, newOwnerId) {
  return request.post(`/docadmin/documents/${docId}/transfer`, { newOwnerId })
}

export function updateDocStatus(docId, status) {
  return request.put(`/docadmin/documents/${docId}/status`, { status })
}
