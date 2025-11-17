/**
 * Main entry point for the Papairs Collaboration Service
 * Provides real-time collaborative editing with document persistence
 */

const CollaborationController = require('./controllers/CollaborationController')
const config = require('./config/config')

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
  console.error('💥 Uncaught Exception:', error)
  process.exit(1)
})

process.on('unhandledRejection', (reason, promise) => {
  console.error('💥 Unhandled Rejection at:', promise, 'reason:', reason)
  process.exit(1)
})

/**
 * Main application startup
 */
async function main() {
  console.log('🚀 Papairs Collaboration Service starting...')
  console.log('📋 Environment:', config.server.environment)
  console.log('🔧 Configuration:', {
    port: config.server.port,
    docsServiceUrl: config.services.docsService.url,
    saveDelayMs: config.document.saveDelayMs
  })
  
  try {
    // Initialize collaboration controller
    const collaborationController = new CollaborationController()
    
    // Start the server
    await collaborationController.start()
    
    // Handle graceful shutdown
    process.on('SIGINT', async () => {
      console.log('\n🛑 Received SIGINT, shutting down gracefully...')
      await collaborationController.stop()
      process.exit(0)
    })
    
    process.on('SIGTERM', async () => {
      console.log('\n🛑 Received SIGTERM, shutting down gracefully...')
      await collaborationController.stop()
      process.exit(0)
    })
    
    console.log('✅ Papairs Collaboration Service is ready!')
    
  } catch (error) {
    console.error('❌ Failed to start collaboration service:', error)
    process.exit(1)
  }
}

// Start the application
main().catch((error) => {
  console.error('💥 Application startup failed:', error)
  process.exit(1)
})