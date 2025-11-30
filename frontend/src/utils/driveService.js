import axios from 'axios'
import auth from './auth'

const API_BASE_URL = 'http://localhost:8080/api/docs'

class DriveService {
  // Helper to get headers with user ID
  getHeaders() {
    return {
      'Content-Type': 'application/json',
      ...auth.getAuthHeader()
    }
  }

  // ===== Folder APIs =====
  
  async getRootFolders() {
    const response = await axios.get(`${API_BASE_URL}/folders/roots`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getAllFolders() {
    const response = await axios.get(`${API_BASE_URL}/folders`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getFolder(folderId) {
    const response = await axios.get(`${API_BASE_URL}/folders/${folderId}`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getChildFolders(folderId) {
    const response = await axios.get(`${API_BASE_URL}/folders/${folderId}/children`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getFolderPath(folderId) {
    const response = await axios.get(`${API_BASE_URL}/folders/${folderId}/path`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getUserFolderTree() {
    const response = await axios.get(`${API_BASE_URL}/folders/trees`,{ 
      headers: this.getHeaders() }
    )
    return response.data
  }

  async createFolder(name, parentFolderId = null) {
    const response = await axios.post(`${API_BASE_URL}/folders`, {
      name,
      parentFolderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async renameFolder(folderId, newName) {
    const response = await axios.patch(`${API_BASE_URL}/folders/${folderId}`, {
      newName
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async deleteFolder(folderId, recursive = true) {
    await axios.delete(`${API_BASE_URL}/folders/${folderId}?recursive=${recursive}`, {
      headers: this.getHeaders()
    })
  }

  async moveFolder(folderId, parentFolderId) {
    const response = await axios.patch(`${API_BASE_URL}/folders/${folderId}/move`, {
      parentFolderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  // ===== Document/Page APIs =====

  async getAllDocuments() {
    const response = await axios.get(`${API_BASE_URL}/pages`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getSharedDocuments() {
    const response = await axios.get(`${API_BASE_URL}/pages/shared`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getDocument(pageId) {
    const response = await axios.get(`${API_BASE_URL}/pages/${pageId}`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async createDocument(title, folderId = null) {
    const response = await axios.post(`${API_BASE_URL}/pages`, {
      title,
      folderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async renameDocument(pageId, newTitle) {
    const response = await axios.patch(`${API_BASE_URL}/pages/${pageId}`, {
      newTitle
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async deleteDocument(pageId) {
    await axios.delete(`${API_BASE_URL}/pages/${pageId}`, {
      headers: this.getHeaders()
    })
  }

  async moveDocument(pageId, folderId) {
    const response = await axios.patch(`${API_BASE_URL}/pages/${pageId}/move`, {
      folderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async updateDocument(pageId, content) {
    const response = await axios.put(`${API_BASE_URL}/pages/${pageId}`, {
      content
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async shareDocument(pageId, userId, role = 'VIEWER') {
    const response = await axios.post(`${API_BASE_URL}/pages/${pageId}/members`, {
      userId,
      role
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getDocumentMembers(pageId) {
    const response = await axios.get(`${API_BASE_URL}/pages/${pageId}/members`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async updateDocumentMemberRole(pageId, userId, role) {
    const response = await axios.patch(`${API_BASE_URL}/pages/${pageId}/members/${userId}`, {
      role
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async removeDocumentMember(pageId, userId) {
    await axios.delete(`${API_BASE_URL}/pages/${pageId}/members/${userId}`, {
      headers: this.getHeaders()
    })
  }
}

export const driveService = new DriveService()
