const { Server } = require('@hocuspocus/server')
const { Logger } = require('@hocuspocus/extension-logger')

const PORT = process.env.PORT || 3004

// Create Hocuspocus server with minimal, working configuration
const server = Server.configure({
  port: PORT,
  
  extensions: [
    new Logger(),
  ],

  // Simple authentication - accept all connections (including without token)
  async onAuthenticate(data) {
    console.log('🔐 Client authenticating for document:', data.documentName)
    // Important: Return object with empty user to allow anonymous connections
    // Returning {} alone may cause auth to fail in some Hocuspocus versions
    return {
      user: {
        id: 'anonymous',
        name: 'Anonymous User'
      }
    }
  },

  // Document lifecycle hooks
  async onLoadDocument(data) {
    console.log('📄 Loading document:', data.documentName)
    // Return null to start with empty document
    return null
  },

  async onStoreDocument(data) {
    console.log('💾 Document stored:', data.documentName)
    // TODO: Save to database here
  },

  // Connection hooks
  async onConnect(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 1
    console.log('� Client connected:', {
      document: data.documentName,
      connections: connectionCount
    })
  },

  async onDisconnect(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 0
    console.log('❌ Client disconnected:', {
      document: data.documentName,
      connections: connectionCount
    })
  },

  async onChange(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 0
    console.log('📝 Document changed:', {
      document: data.documentName,
      connections: connectionCount
    })
  },
})

// Start server
server.listen().then(() => {
  console.log('✅ Hocuspocus server started successfully')
  console.log(`   WebSocket: ws://localhost:${PORT}`)
  console.log(`   HTTP: http://localhost:${PORT}`)
  console.log('   Ready for connections!')
})
