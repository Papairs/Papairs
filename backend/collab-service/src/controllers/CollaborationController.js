const { Server } = require('@hocuspocus/server')
const { Logger } = require('@hocuspocus/extension-logger')
const config = require('../config/config')
const DocumentService = require('../services/DocumentService')
const UserManager = require('../services/UserManager')
const ApiController = require('./ApiController')

/**
 * Main controller for the collaboration server
 * Orchestrates document collaboration, user management, and WebSocket connections
 */
class CollaborationController {
  constructor() {
    this.documentService = new DocumentService()
    this.userManager = new UserManager()
    this.apiController = new ApiController(this.documentService, this.userManager)
    this.server = null
    this.apiServer = null
    
    console.log('🚀 CollaborationController initialized with config:', {
      port: config.server.port,
      environment: config.server.environment,
      docsServiceUrl: config.services.docsService.url,
      saveDelayMs: config.document.saveDelayMs
    })
  }

  /**
   * Initialize and configure the Hocuspocus server
   * @returns {Server} - Configured Hocuspocus server instance
   */
  createServer() {
    console.log('⚙️ Creating Hocuspocus server...')
    
    this.server = Server.configure({
      port: config.server.port,
      
      extensions: [
        new Logger({
          onLog: (message) => {
            if (config.logging.enableDebug) {
              console.log('📝 Hocuspocus:', message)
            }
          }
        }),
      ],

      // Authentication handler
      onAuthenticate: async (data) => {
        return this.handleAuthentication(data)
      },

      // Document lifecycle handlers
      onLoadDocument: async (data) => {
        return this.handleLoadDocument(data)
      },

      onStoreDocument: async (data) => {
        return this.handleStoreDocument(data)
      },

      // Connection lifecycle handlers
      onConnect: async (data) => {
        return this.handleConnect(data)
      },

      onDisconnect: async (data) => {
        return this.handleDisconnect(data)
      },

      // Document change handler
      onChange: async (data) => {
        return this.handleChange(data)
      },
    })

    console.log('✅ Hocuspocus server configured')
    return this.server
  }

  /**
   * Handle user authentication
   * @param {Object} data - Authentication data from client
   * @returns {Object} - Authentication result
   */
  async handleAuthentication(data) {
    try {
      const authResult = this.userManager.authenticateUser(data)
      
      console.log('🔐 Authentication successful:', {
        document: data.documentName,
        userId: authResult.user.id,
        isAnonymous: authResult.user.isAnonymous
      })
      
      return authResult
    } catch (error) {
      console.error('❌ Authentication failed:', {
        document: data.documentName,
        error: error.message
      })
      
      // Return anonymous user on authentication failure
      return this.userManager.authenticateUser({
        documentName: data.documentName,
        token: null
      })
    }
  }

  /**
   * Handle document loading
   * @param {Object} data - Document load data with context
   * @returns {string|null} - HTML content or null (let Hocuspocus handle Y.js conversion)
   */
  async handleLoadDocument(data) {
    try {
      const userId = this.userManager.getDocumentUser(data.documentName)
      
      console.log('📄 Loading document:', {
        document: data.documentName,
        userId: userId
      })
      
      const content = await this.documentService.loadDocument(data.documentName, userId)
      
      console.log('🔍 Raw content from database:', {
        document: data.documentName,
        hasContent: !!content,
        contentType: typeof content,
        contentLength: content ? content.length : 0,
        contentPreview: content ? content.substring(0, 200) + '...' : 'null'
      })
      
      if (content && content.trim() !== '') {
        console.log('✅ Document loaded with content:', {
          document: data.documentName,
          contentLength: content.length,
          contentPreview: content.substring(0, 100) + '...'
        })
        
        // Return HTML content directly - let Hocuspocus handle Y.js conversion internally
        // This approach should work better than manually creating Y.js documents
        console.log('🔄 Returning HTML for Hocuspocus internal processing:', content)
        return content
        
      } else {
        console.log('📄 Starting with empty document:', data.documentName)
        return null
      }
    } catch (error) {
      console.error('❌ Document load failed:', {
        document: data.documentName,
        error: error.message
      })
      
      if (error.message === 'Access denied') {
        throw error
      }
      
      return null
    }
  }

  /**
   * Handle document storage (called by Hocuspocus)
   * @param {Object} data - Document store data
   */
  async handleStoreDocument(data) {
    console.log('💾 Store document called (using delayed save instead):', data.documentName)
    // We handle saving through onChange with delayed saves
  }

  /**
   * Handle client connection
   * @param {Object} data - Connection data
   */
  async handleConnect(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 1
    const userId = this.userManager.getDocumentUser(data.documentName)
    
    console.log('🔗 Client connected:', {
      document: data.documentName,
      userId: userId,
      connections: connectionCount
    })
    
    // Update user activity
    this.userManager.updateUserActivity(userId)
  }

  /**
   * Handle client disconnection
   * @param {Object} data - Disconnection data
   */
  async handleDisconnect(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 0
    const userId = this.userManager.handleUserDisconnect(data.documentName)
    
    console.log('❌ Client disconnected:', {
      document: data.documentName,
      userId: userId,
      connections: connectionCount,
      willSaveImmediately: connectionCount === 0
    })

    // If last user disconnected, save immediately and cleanup
    if (connectionCount === 0) {
      console.log('👋 Last user disconnected, saving and cleaning up:', data.documentName)
      
      try {
        await this.documentService.saveImmediately(data.documentName, data.document, userId)
        this.documentService.cleanupDocument(data.documentName)
        this.userManager.cleanupDocument(data.documentName)
      } catch (error) {
        console.error('❌ Error during disconnect cleanup:', {
          document: data.documentName,
          error: error.message
        })
      }
    }
  }

  /**
   * Handle document changes
   * @param {Object} data - Change data
   */
  async handleChange(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 0
    const userId = this.userManager.getDocumentUser(data.documentName)
    
    console.log('📝 Document changed:', {
      document: data.documentName,
      userId: userId,
      connections: connectionCount,
      timestamp: new Date().toISOString()
    })

    // Update user activity
    this.userManager.updateUserActivity(userId)

    // Schedule delayed save
    this.documentService.scheduleDelayedSave(data.documentName, data.document, userId)
  }

  /**
   * Start the collaboration server
   * @returns {Promise} - Server start promise
   */
  async start() {
    if (!this.server) {
      this.createServer()
    }
    
    console.log('🚀 Starting collaboration server...')
    
    // Start WebSocket server
    await this.server.listen()
    
    // Start HTTP API server on port +1
    const apiPort = config.server.port + 1
    this.apiServer = await this.apiController.start(apiPort)
    
    console.log('✅ Collaboration server started successfully')
    console.log(`   WebSocket: ws://localhost:${config.server.port}`)
    console.log(`   HTTP API: http://localhost:${apiPort}`)
    console.log(`   HTTP: http://localhost:${config.server.port}`)
    console.log('   Ready for connections!')
    
    // Start periodic cleanup
    this.startPeriodicCleanup()
    
    return this.server
  }

  /**
   * Start periodic cleanup of inactive sessions
   */
  startPeriodicCleanup() {
    // Clean up inactive sessions every hour
    setInterval(() => {
      const cleanedSessions = this.userManager.cleanupInactiveSessions()
      
      if (cleanedSessions > 0) {
        console.log(`🧹 Cleaned up ${cleanedSessions} inactive user sessions`)
      }
    }, 60 * 60 * 1000) // 1 hour
    
    console.log('🧹 Periodic cleanup started (1 hour interval)')
  }

  /**
   * Stop the collaboration server
   * @returns {Promise} - Server stop promise
   */
  async stop() {
    if (this.server) {
      console.log('🛑 Stopping collaboration server...')
      await this.server.destroy()
      console.log('✅ WebSocket server stopped')
    }
    
    if (this.apiServer) {
      console.log('🛑 Stopping API server...')
      await new Promise(resolve => this.apiServer.close(resolve))
      console.log('✅ API server stopped')
    }
    
    console.log('✅ All servers stopped')
  }

  /**
   * Get server statistics
   * @returns {Object} - Server statistics
   */
  getStats() {
    return {
      server: {
        port: config.server.port,
        environment: config.server.environment
      },
      documents: this.documentService.getStats(),
      users: this.userManager.getStats()
    }
  }
}

module.exports = CollaborationController