import { BaseApiController } from './BaseApiController.js';

/**
 * Page Management Controller
 * Handles all page-related API calls
 */
export class PageController extends BaseApiController {
  constructor() {
    super('http://localhost:8080');
    this.servicePath = '/api/docs';
  }

  /**
   * Get all pages user has access to
   */
  async getAllPages() {
    return this.get(`${this.servicePath}/pages`);
  }

  /**
   * Create a new page
   * @param {Object} pageData - { title, folderId? }
   */
  async createPage(pageData) {
    return this.post(`${this.servicePath}/pages`, pageData);
  }

  /**
   * Get page by ID
   * @param {string} pageId - The page ID
   */
  async getPage(pageId) {
    return this.get(`${this.servicePath}/pages/${pageId}`);
  }

  /**
   * Update page content
   * @param {string} pageId - The page ID
   * @param {Object} updateData - { content }
   */
  async updatePage(pageId, updateData) {
    return this.put(`${this.servicePath}/pages/${pageId}`, updateData);
  }

  /**
   * Rename a page
   * @param {string} pageId - The page ID
   * @param {Object} renameData - { newTitle }
   */
  async renamePage(pageId, renameData) {
    return this.patch(`${this.servicePath}/pages/${pageId}`, renameData);
  }

  /**
   * Move page to different folder
   * @param {string} pageId - The page ID
   * @param {Object} moveData - { folderId }
   */
  async movePage(pageId, moveData) {
    return this.patch(`${this.servicePath}/pages/${pageId}/move`, moveData);
  }

  /**
   * Delete a page
   * @param {string} pageId - The page ID
   */
  async deletePage(pageId) {
    return this.delete(`${this.servicePath}/pages/${pageId}`);
  }
}