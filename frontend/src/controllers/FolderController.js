import { BaseApiController } from './BaseApiController.js';

/**
 * Folder Management Controller
 * Handles all folder-related API calls
 */
export class FolderController extends BaseApiController {
  constructor() {
    super('http://localhost:8080');
    this.servicePath = '/api/docs';
  }

  /**
   * Get folder by ID
   * @param {string} folderId - The folder ID
   */
  async getFolder(folderId) {
    return this.get(`${this.servicePath}/folders/${folderId}`);
  }

  /**
   * Create a new folder
   * @param {Object} folderData - { name, parentFolderId? }
   */
  async createFolder(folderData) {
    return this.post(`${this.servicePath}/folders`, folderData);
  }

  /**
   * Rename a folder
   * @param {string} folderId - The folder ID
   * @param {Object} renameData - { newName }
   */
  async renameFolder(folderId, renameData) {
    return this.patch(`${this.servicePath}/folders/${folderId}`, renameData);
  }

  /**
   * Delete a folder
   * @param {string} folderId - The folder ID
   * @param {boolean} recursive - Whether to delete recursively
   */
  async deleteFolder(folderId, recursive = false) {
    const queryParam = recursive ? '?recursive=true' : '';
    return this.delete(`${this.servicePath}/folders/${folderId}${queryParam}`);
  }

  /**
   * Get all user folders
   */
  async getAllFolders() {
    return this.get(`${this.servicePath}/folders`);
  }

  /**
   * Get root folders
   */
  async getRootFolders() {
    return this.get(`${this.servicePath}/folders/roots`);
  }

  /**
   * Get child folders
   * @param {string} folderId - The folder ID
   */
  async getChildFolders(folderId) {
    return this.get(`${this.servicePath}/folders/${folderId}/children`);
  }

  /**
   * Get folder tree from specific folder
   * @param {string} folderId - The folder ID
   */
  async getFolderTree(folderId) {
    return this.get(`${this.servicePath}/folders/${folderId}/tree`);
  }

  /**
   * Get all user folder trees
   */
  async getUserFolderTrees() {
    return this.get(`${this.servicePath}/folders/trees`);
  }

  /**
   * Get folder path from root
   * @param {string} folderId - The folder ID
   */
  async getFolderPath(folderId) {
    return this.get(`${this.servicePath}/folders/${folderId}/path`);
  }

  /**
   * Move folder to new parent
   * @param {string} folderId - The folder ID
   * @param {Object} moveData - { parentFolderId }
   */
  async moveFolder(folderId, moveData) {
    return this.patch(`${this.servicePath}/folders/${folderId}/move`, moveData);
  }
}