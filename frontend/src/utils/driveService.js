import axios from 'axios'
import auth from './auth'
import { API_BASE_URL } from "@/config";

// Axios interceptor to handle 401 errors globally
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('papairs_token');
      localStorage.removeItem('papairs_user');
      console.error('🚀 Redirecting now...');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

class DriveService {
  // Helper to get headers with user ID
  getHeaders() {
    const userId = auth.getUserId()
    const headers = {
      'Content-Type': 'application/json',
      ...auth.getAuthHeader()
    }
    
    // Add X-User-Id header if available, otherwise use a temporary ID
    if (userId) {
      headers['X-User-Id'] = userId
    } else {
      // Generate temporary user ID for unauthenticated access
      if (!this.tempUserId) {
        this.tempUserId = 'temp-' + Math.random().toString(36).substring(7)
      }
      headers['X-User-Id'] = this.tempUserId
    }
    
    return headers
  }

  // ===== Folder APIs =====
  
  async getRootFolders() {
    const response = await axios.get(`${API_BASE_URL}/api/docs/folders/roots`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getAllFolders() {
    const response = await axios.get(`${API_BASE_URL}/api/docs/folders`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getFolder(folderId) {
    const response = await axios.get(`${API_BASE_URL}/api/docs/folders/${folderId}`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getChildFolders(folderId) {
    const response = await axios.get(`${API_BASE_URL}/api/docs/folders/${folderId}/children`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getFolderPath(folderId) {
    const response = await axios.get(`${API_BASE_URL}/api/docs/folders/${folderId}/path`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getUserFolderTree() {
    const response = await axios.get(`${API_BASE_URL}/api/docs/folders/trees`,{
      headers: this.getHeaders() }
    )
    return response.data
  }

  async createFolder(name, parentFolderId = null) {
    const response = await axios.post(`${API_BASE_URL}/api/docs/folders`, {
      name,
      parentFolderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async renameFolder(folderId, newName) {
    const response = await axios.patch(`${API_BASE_URL}/api/docs/folders/${folderId}`, {
      newName
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async deleteFolder(folderId, recursive = true) {
    await axios.delete(`${API_BASE_URL}/api/docs/folders/${folderId}?recursive=${recursive}`, {
      headers: this.getHeaders()
    })
  }

  async moveFolder(folderId, parentFolderId) {
    const response = await axios.patch(`${API_BASE_URL}/api/docs/folders/${folderId}/move`, {
      parentFolderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  // ===== Document/Page APIs =====

  async getAllDocuments() {
    const response = await axios.get(`${API_BASE_URL}/api/docs/pages`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getSharedDocuments() {
    const response = await axios.get(`${API_BASE_URL}/api/docs/pages/shared`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getDocument(pageId) {
    const response = await axios.get(`${API_BASE_URL}/api/docs/pages/${pageId}`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async createDocument(title, folderId = null) {
    const response = await axios.post(`${API_BASE_URL}/api/docs/pages`, {
      title,
      folderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async renameDocument(pageId, newTitle) {
    const response = await axios.patch(`${API_BASE_URL}/api/docs/pages/${pageId}`, {
      newTitle
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async deleteDocument(pageId) {
    await axios.delete(`${API_BASE_URL}/api/docs/pages/${pageId}`, {
      headers: this.getHeaders()
    })
  }

  async moveDocument(pageId, folderId) {
    const response = await axios.patch(`${API_BASE_URL}/api/docs/pages/${pageId}/move`, {
      folderId
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async updateDocument(pageId, content) {
    const response = await axios.put(`${API_BASE_URL}/api/docs/pages/${pageId}`, {
      content
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async shareDocument(pageId, userId, role = 'VIEWER') {
    const response = await axios.post(`${API_BASE_URL}/api/docs/pages/${pageId}/members`, {
      userId,
      role
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async getDocumentMembers(pageId) {
    const response = await axios.get(`${API_BASE_URL}/api/docs/pages/${pageId}/members`, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async updateDocumentMemberRole(pageId, userId, role) {
    const response = await axios.patch(`${API_BASE_URL}/api/docs/pages/${pageId}/members/${userId}`, {
      role
    }, {
      headers: this.getHeaders()
    })
    return response.data
  }

  async removeDocumentMember(pageId, userId) {
    await axios.delete(`${API_BASE_URL}/api/docs/pages/${pageId}/members/${userId}`, {
      headers: this.getHeaders()
    })
  }
}

export const driveService = new DriveService()
