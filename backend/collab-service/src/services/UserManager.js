/**
 * User management service for handling authentication and user tracking
 * Manages user sessions and document access control
 */
class UserManager {
  constructor() {
    this.userSessions = new Map() // Map user IDs to session info
    this.documentUsers = new Map() // Map document names to active user IDs
  }

  /**
   * Authenticate a user for document access
   * @param {Object} authData - Authentication data from client
   * @returns {Object} - User object with authentication result
   */
  authenticateUser(authData) {
    const userId = authData?.token || this.generateAnonymousId()
    
    console.log('🔐 UserManager: Authenticating user:', {
      document: authData.documentName,
      userId: userId,
      hasToken: !!authData?.token
    })
    
    // Create user session
    const userSession = {
      id: userId,
      name: authData?.name || `User ${userId}`,
      authenticatedAt: new Date().toISOString(),
      isAnonymous: !authData?.token,
      lastActivity: new Date().toISOString()
    }
    
    // Store session
    this.userSessions.set(userId, userSession)
    
    // Associate user with document
    if (authData.documentName) {
      this.setDocumentUser(authData.documentName, userId)
    }
    
    console.log('✅ User authenticated:', {
      userId: userId,
      isAnonymous: userSession.isAnonymous,
      document: authData.documentName
    })
    
    return {
      user: userSession,
      success: true
    }
  }

  /**
   * Set user for a specific document
   * @param {string} documentName - Document identifier
   * @param {string} userId - User identifier
   */
  setDocumentUser(documentName, userId) {
    this.documentUsers.set(documentName, userId)
    
    // Update last activity
    if (this.userSessions.has(userId)) {
      this.userSessions.get(userId).lastActivity = new Date().toISOString()
    }
    
    console.log('👤 UserManager: User assigned to document:', {
      document: documentName,
      userId: userId
    })
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
   * Get user session information
   * @param {string} userId - User identifier
   * @returns {Object|null} - User session or null if not found
   */
  getUserSession(userId) {
    return this.userSessions.get(userId) || null
  }

  /**
   * Update user activity timestamp
   * @param {string} userId - User identifier
   */
  updateUserActivity(userId) {
    if (this.userSessions.has(userId)) {
      this.userSessions.get(userId).lastActivity = new Date().toISOString()
    }
  }

  /**
   * Generate anonymous user ID
   * @returns {string} - Anonymous user identifier
   */
  generateAnonymousId() {
    const timestamp = Date.now()
    const random = Math.random().toString(36).substring(2, 8)
    return `anon_${timestamp}_${random}`
  }

  /**
   * Handle user disconnect from document
   * @param {string} documentName - Document identifier
   * @param {string} userId - User identifier (optional)
   */
  handleUserDisconnect(documentName, userId = null) {
    const actualUserId = userId || this.getDocumentUser(documentName)
    
    console.log('❌ UserManager: User disconnecting from document:', {
      document: documentName,
      userId: actualUserId
    })
    
    // Update session if user exists
    if (this.userSessions.has(actualUserId)) {
      this.userSessions.get(actualUserId).lastActivity = new Date().toISOString()
    }
    
    return actualUserId
  }

  /**
   * Clean up user data for a document
   * @param {string} documentName - Document identifier
   */
  cleanupDocument(documentName) {
    this.documentUsers.delete(documentName)
    console.log('🧹 UserManager: Cleaned up document user data:', documentName)
  }

  /**
   * Clean up inactive user sessions
   * @param {number} maxIdleTimeMs - Maximum idle time in milliseconds
   */
  cleanupInactiveSessions(maxIdleTimeMs = 24 * 60 * 60 * 1000) { // 24 hours default
    const now = new Date()
    const sessionsToRemove = []
    
    for (const [userId, session] of this.userSessions) {
      const lastActivity = new Date(session.lastActivity)
      const idleTime = now - lastActivity
      
      if (idleTime > maxIdleTimeMs) {
        sessionsToRemove.push(userId)
      }
    }
    
    sessionsToRemove.forEach(userId => {
      this.userSessions.delete(userId)
      console.log('🧹 UserManager: Cleaned up inactive session:', userId)
    })
    
    return sessionsToRemove.length
  }

  /**
   * Get active users for a document
   * @param {string} documentName - Document identifier
   * @returns {Array} - Array of active user sessions
   */
  getActiveDocumentUsers(documentName) {
    const userId = this.getDocumentUser(documentName)
    const session = this.getUserSession(userId)
    
    return session ? [session] : []
  }

  /**
   * Get user manager statistics
   * @returns {Object} - Statistics about active users and sessions
   */
  getStats() {
    const now = new Date()
    const activeUsers = Array.from(this.userSessions.values()).filter(session => {
      const lastActivity = new Date(session.lastActivity)
      const idleTime = now - lastActivity
      return idleTime < 30 * 60 * 1000 // Active within last 30 minutes
    })
    
    return {
      totalSessions: this.userSessions.size,
      activeUsers: activeUsers.length,
      documentsWithUsers: this.documentUsers.size,
      anonymousUsers: Array.from(this.userSessions.values()).filter(s => s.isAnonymous).length,
      authenticatedUsers: Array.from(this.userSessions.values()).filter(s => !s.isAnonymous).length
    }
  }
}

module.exports = UserManager