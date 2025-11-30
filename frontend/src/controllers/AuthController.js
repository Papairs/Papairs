import { BaseApiController } from './BaseApiController.js';

/**
 * Authentication Service Controller
 * Handles all authentication-related API calls
 */
export class AuthController extends BaseApiController {
  constructor() {
    super('http://localhost:8080');
    this.servicePath = '/api/auth';
  }

  /**
   * Check service health
   */
  async checkHealth() {
    return this.get('/actuator/health/services/authService');
  }

  /**
   * Login user
   * @param {Object} credentials - { email, password }
   */
  async login(credentials) {
    const result = await this.post(`${this.servicePath}/login`, credentials);
    
    // Store auth data on successful login
    if (result.success && result.data.token) {
      localStorage.setItem('papairs_token', result.data.token);
      if (result.data.user) {
        localStorage.setItem('papairs_user', JSON.stringify({
          id: result.data.user.id,
          email: result.data.user.email,
          emailVerified: result.data.user.emailVerified || false,
          active: result.data.user.active || true
        }));
      }
    }
    
    return result;
  }

  /**
   * Register new user
   * @param {Object} userDetails - { email, password }
   */
  async register(userDetails) {
    return this.post(`${this.servicePath}/register`, userDetails);
  }

  /**
   * Logout current user
   */
  async logout() {
    const result = await this.post(`${this.servicePath}/logout`);
    
    // Clear stored auth data
    localStorage.removeItem('papairs_token');
    localStorage.removeItem('papairs_user');
    
    return result;
  }

  /**
   * Validate current session token
   */
  async validateToken() {
    return this.post(`${this.servicePath}/validation`);
  }

  /**
   * Change user password
   * @param {Object} passwordData - { currentPassword, newPassword }
   */
  async changePassword(passwordData) {
    return this.post(`${this.servicePath}/change-password`, passwordData);
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn() {
    return !!localStorage.getItem('papairs_token');
  }

  /**
   * Get current user ID
   */
  getCurrentUserId() {
    try {
      const userData = localStorage.getItem('papairs_user');
      const user = userData ? JSON.parse(userData) : null;
      return user?.id || null;
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error);
      return null;
    }
  }

  /**
   * Clear authentication data
   */
  clearAuthData() {
    localStorage.removeItem('papairs_token');
    localStorage.removeItem('papairs_user');
  }
}