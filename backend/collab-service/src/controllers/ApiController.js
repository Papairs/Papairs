const express = require('express')
const cors = require('cors')

/**
 * HTTP API controller for collaboration service
 * Provides REST endpoints for document operations
 */
class ApiController {
  constructor(documentService, userManager) {
    this.documentService = documentService
    this.userManager = userManager
    this.app = express()
    this.setupMiddleware()
    this.setupRoutes()
  }

  /**
   * Setup Express middleware
   */
  setupMiddleware() {
    this.app.use(cors())
    this.app.use(express.json())
    
    // Request logging
    this.app.use((req, res, next) => {
      console.log(`📡 API Request: ${req.method} ${req.path}`, {
        userId: req.headers['x-user-id'],
        userAgent: req.headers['user-agent']
      })
      next()
    })
  }

  /**
   * Setup API routes
   */
  setupRoutes() {
    // Health check
    this.app.get('/health', (req, res) => {
      res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
      })
    })

    // Get document initial content (for loading existing documents)
    this.app.get('/api/documents/:documentId/initial-content', async (req, res) => {
      try {
        const { documentId } = req.params
        const userId = req.headers['x-user-id'] || 'anonymous-user'
        
        console.log('📄 API: Getting initial content:', {
          documentId,
          userId
        })
        
        // Load content from database
        const content = await this.documentService.loadDocument(documentId, userId)
        
        if (content) {
          res.json({
            success: true,
            content: content,
            contentLength: content.length
          })
        } else {
          res.json({
            success: true,
            content: null,
            message: 'Document not found or empty'
          })
        }
      } catch (error) {
        console.error('❌ API Error getting initial content:', error)
        
        if (error.message === 'Access denied') {
          res.status(403).json({
            success: false,
            error: 'Access denied'
          })
        } else {
          res.status(500).json({
            success: false,
            error: 'Failed to load document content'
          })
        }
      }
    })

    // Get service statistics
    this.app.get('/api/stats', (req, res) => {
      const documentStats = this.documentService.getStats()
      const userStats = this.userManager.getStats()
      
      res.json({
        documents: documentStats,
        users: userStats,
        timestamp: new Date().toISOString()
      })
    })

    // 404 handler
    this.app.use((req, res) => {
      res.status(404).json({
        success: false,
        error: 'Not found',
        path: req.path
      })
    })

    // Error handler
    this.app.use((error, req, res, next) => {
      console.error('❌ API Error:', error)
      
      res.status(500).json({
        success: false,
        error: 'Internal server error'
      })
    })
  }

  /**
   * Start the HTTP API server
   * @param {number} port - Port to listen on
   * @returns {Promise} - Server start promise
   */
  async start(port) {
    return new Promise((resolve, reject) => {
      const server = this.app.listen(port, (error) => {
        if (error) {
          reject(error)
        } else {
          console.log(`✅ API server listening on port ${port}`)
          resolve(server)
        }
      })
    })
  }

  /**
   * Get Express app instance
   * @returns {Express} - Express app
   */
  getApp() {
    return this.app
  }
}

module.exports = ApiController