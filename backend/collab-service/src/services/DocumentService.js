const axios = require('axios')
const config = require('../config/config')
const ContentFormatter = require('../utils/ContentFormatter')

/**
 * Service class for managing document operations
 * Handles saving, loading, and content transformation for documents
 */
class DocumentService {
  constructor() {
    this.saveTimers = new Map()
    this.documentUsers = new Map()
    this.saveRetries = new Map()
  }

  /**
   * Schedule a delayed save for a document
   * @param {string} documentName - Document identifier
   * @param {Y.Doc} ydoc - Y.js document instance
   * @param {string} userId - User identifier
   */
  scheduleDelayedSave(documentName, ydoc, userId) {
    console.log('⏰ DocumentService: Scheduling delayed save:', {
      document: documentName,
      userId: userId,
      delay: `${config.document.saveDelayMs}ms`,
      hasExistingTimer: this.saveTimers.has(documentName)
    })
    
    // Store user ID for this document
    this.documentUsers.set(documentName, userId)
    
    // Clear existing timer if any
    if (this.saveTimers.has(documentName)) {
      console.log('🔄 Clearing existing save timer for:', documentName)
      clearTimeout(this.saveTimers.get(documentName))
    }

    // Schedule new save timer
    const timer = setTimeout(async () => {
      console.log('⏰ Save timer triggered for:', documentName)
      await this.saveImmediately(documentName, ydoc, userId)
      this.saveTimers.delete(documentName)
    }, config.document.saveDelayMs)

    this.saveTimers.set(documentName, timer)
    console.log('✅ Save timer scheduled for:', documentName)
  }

  /**
   * Save document immediately to the backend service
   * @param {string} documentName - Document identifier
   * @param {Y.Doc} ydoc - Y.js document instance
   * @param {string} userId - User identifier
   * @param {number} retryCount - Current retry attempt
   */
  async saveImmediately(documentName, ydoc, userId, retryCount = 0) {
    console.log('💾 DocumentService: Starting immediate save:', {
      document: documentName,
      userId: userId,
      retryCount: retryCount,
      hasTimer: this.saveTimers.has(documentName)
    })
    
    try {
      // Clear any pending timer
      if (this.saveTimers.has(documentName)) {
        console.log('🔄 Clearing save timer before immediate save:', documentName)
        clearTimeout(this.saveTimers.get(documentName))
        this.saveTimers.delete(documentName)
      }

      // Get user ID from stored context or parameter
      const actualUserId = userId || this.documentUsers.get(documentName) || 'anonymous-user'
      console.log('👤 Using user ID for save:', {
        document: documentName,
        actualUserId: actualUserId
      })

      // Extract content using ContentFormatter
      const extractionResult = ContentFormatter.extractFromYDoc(ydoc)
      
      if (!extractionResult.success) {
        throw new Error(`Content extraction failed: ${extractionResult.extractionMethod}`)
      }

      // Sanitize content
      const content = ContentFormatter.sanitizeContent(extractionResult.content)
      
      console.log('📄 Content ready for save:', {
        document: documentName,
        extractionMethod: extractionResult.extractionMethod,
        contentLength: content.length,
        contentPreview: content.substring(0, 200) + (content.length > 200 ? '...' : '')
      })

      // Prepare API request
      const apiUrl = `${config.services.docsService.url}/api/docs/pages/${documentName}`
      const requestData = { content }
      const requestHeaders = {
        'Content-Type': 'application/json',
        'X-User-Id': actualUserId
      }
      
      console.log('🔗 Making API request:', {
        url: apiUrl,
        method: 'PUT',
        headers: requestHeaders,
        bodySize: JSON.stringify(requestData).length
      })

      // Call docs-service API with timeout
      const response = await axios.put(apiUrl, requestData, { 
        headers: requestHeaders,
        timeout: config.services.docsService.timeout
      })

      console.log('✅ Document saved successfully:', {
        document: documentName,
        contentLength: content.length,
        userId: actualUserId,
        responseStatus: response.status,
        responseData: response.data
      })

      // Clear retry count on success
      this.saveRetries.delete(documentName)

    } catch (error) {
      console.error('❌ Document save failed:', {
        document: documentName,
        userId: userId,
        retryCount: retryCount,
        error: error.response?.data || error.message,
        status: error.response?.status
      })

      // Implement retry logic
      if (retryCount < config.document.maxRetries) {
        const nextRetry = retryCount + 1
        console.log(`🔄 Retrying save (${nextRetry}/${config.document.maxRetries}) in ${config.document.retryDelayMs}ms`)
        
        setTimeout(() => {
          this.saveImmediately(documentName, ydoc, userId, nextRetry)
        }, config.document.retryDelayMs)
      } else {
        console.error('❌ All save retries exhausted for:', documentName)
        this.saveRetries.delete(documentName)
      }
    }
  }

  /**
   * Load document content from the backend service
   * @param {string} documentName - Document identifier
   * @param {string} userId - User identifier
   * @returns {string|null} - Document content or null if not found
   */
  async loadDocument(documentName, userId) {
    console.log('📄 DocumentService: Loading document:', {
      document: documentName,
      userId: userId
    })
    
    try {
      const apiUrl = `${config.services.docsService.url}/api/docs/pages/${documentName}`
      const requestHeaders = { 
        'X-User-Id': userId,
        'timeout': config.services.docsService.timeout
      }
      
      console.log('🔗 Making document load request:', {
        url: apiUrl,
        headers: requestHeaders
      })
      
      const response = await axios.get(apiUrl, { 
        headers: requestHeaders,
        timeout: config.services.docsService.timeout
      })
      
      if (response.data && response.data.content) {
        const content = response.data.content
        
        console.log('✅ Document loaded successfully:', {
          document: documentName,
          contentLength: content.length,
          owner: response.data.ownerId,
          userId: userId,
          responseStatus: response.status,
          contentPreview: content.substring(0, 100) + (content.length > 100 ? '...' : '')
        })

        // Convert HTML content back to format suitable for Y.js
        // For now, return the HTML content directly - Tiptap can handle HTML
        return content
        
      } else {
        console.log('📄 Empty document response:', {
          document: documentName,
          responseData: response.data
        })
        return null
      }
    } catch (error) {
      console.log('❌ Document load failed:', {
        document: documentName,
        userId: userId,
        error: error.response?.status || error.message,
        errorData: error.response?.data
      })
      
      if (error.response?.status === 404) {
        console.log('📄 Document not found, will start with empty document')
        return null
      }
      
      if (error.response?.status === 403) {
        console.log('🔒 Access denied for document')
        throw new Error('Access denied')
      }
      
      // For other errors, log but return null to start with empty document
      console.log('📄 Starting with empty document due to error')
      return null
    }
  }

  /**
   * Set user ID for a document
   * @param {string} documentName - Document identifier
   * @param {string} userId - User identifier
   */
  setDocumentUser(documentName, userId) {
    this.documentUsers.set(documentName, userId)
    console.log('👤 User set for document:', { document: documentName, userId })
  }

  /**
   * Get user ID for a document
   * @param {string} documentName - Document identifier
   * @returns {string} - User identifier
   */
  getDocumentUser(documentName) {
    return this.documentUsers.get(documentName) || 'anonymous-user'
  }

  /**
   * Clean up resources for a document
   * @param {string} documentName - Document identifier
   */
  cleanupDocument(documentName) {
    // Clear any pending save timer
    if (this.saveTimers.has(documentName)) {
      clearTimeout(this.saveTimers.get(documentName))
      this.saveTimers.delete(documentName)
    }
    
    // Remove user tracking
    this.documentUsers.delete(documentName)
    this.saveRetries.delete(documentName)
    
    console.log('🧹 Cleaned up resources for document:', documentName)
  }

  /**
   * Get service statistics
   * @returns {Object} - Service statistics
   */
  getStats() {
    return {
      activeDocuments: this.documentUsers.size,
      pendingSaves: this.saveTimers.size,
      documentsWithRetries: this.saveRetries.size,
      documents: Array.from(this.documentUsers.keys())
    }
  }
}

module.exports = DocumentService