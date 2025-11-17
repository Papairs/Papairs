/**
 * Configuration management for the collaboration service
 * Centralizes all environment variables and constants
 */

const config = {
  // Server configuration
  server: {
    port: parseInt(process.env.PORT) || 3004,
    environment: process.env.NODE_ENV || 'development'
  },

  // External services
  services: {
    docsService: {
      url: process.env.DOCS_SERVICE_URL || 'http://docs-service:8082',
      timeout: parseInt(process.env.DOCS_SERVICE_TIMEOUT) || 30000
    }
  },

  // Document management
  document: {
    saveDelayMs: parseInt(process.env.SAVE_DELAY_MS) || 5000,
    maxRetries: parseInt(process.env.MAX_SAVE_RETRIES) || 3,
    retryDelayMs: parseInt(process.env.RETRY_DELAY_MS) || 1000
  },

  // Logging
  logging: {
    level: process.env.LOG_LEVEL || 'info',
    enableDebug: process.env.DEBUG === 'true' || process.env.NODE_ENV === 'development'
  }
}

module.exports = config