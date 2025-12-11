/**
 * Integration Tests - Authentication Flow
 * Tests the complete authentication flow including AuthController, API calls, and localStorage
 */

import { AuthController } from '@/controllers/AuthController'

describe('Authentication Flow Integration', () => {
  let authController
  let mockFetch

  beforeEach(() => {
    authController = new AuthController()
    localStorage.clear()
    
    // Mock window.location
    delete window.location
    window.location = { href: '' }
    
    // Mock fetch globally
    global.fetch = jest.fn()
    mockFetch = global.fetch
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  describe('Complete Login Flow', () => {
    test('should handle successful login flow end-to-end', async () => {
      // Mock successful API response
      const mockHeaders = new Map([['content-type', 'application/json']])
      const mockUser = {
        id: 'user-123',
        email: 'test@example.com',
        emailVerified: true,
        active: true
      }

      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          token: 'jwt-token-abc123',
          user: mockUser
        })
      })

      // Execute login
      const result = await authController.login({
        email: 'test@example.com',
        password: 'password123'
      })

      // Verify API was called correctly
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/auth/login'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({
            email: 'test@example.com',
            password: 'password123'
          })
        })
      )

      // Verify response structure
      expect(result.success).toBe(true)
      expect(result.data.token).toBe('jwt-token-abc123')
      expect(result.data.user).toEqual(mockUser)

      // Verify auth data was stored
      expect(localStorage.getItem('papairs_token')).toBe('jwt-token-abc123')
      const storedUser = JSON.parse(localStorage.getItem('papairs_user'))
      expect(storedUser).toEqual(mockUser)

      // Verify user is now authenticated
      expect(authController.isLoggedIn()).toBe(true)
      expect(authController.getCurrentUserId()).toBe('user-123')
    })

    test('should handle failed login and not store credentials', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          message: 'Invalid credentials'
        })
      })

      // Attempt login (will redirect and not resolve)
      const loginPromise = authController.login({
        email: 'test@example.com',
        password: 'wrong-password'
      })

      // Give redirect time to process
      await new Promise(resolve => setTimeout(resolve, 50))

      // Verify no credentials were stored
      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()

      // Verify user is not authenticated
      expect(authController.isLoggedIn()).toBe(false)
      expect(authController.getCurrentUserId()).toBeNull()

      // Verify redirect to login
      expect(window.location.href).toBe('/login')

      // Promise should not resolve
      const result = await Promise.race([
        loginPromise.then(() => 'resolved', () => 'rejected'),
        new Promise(resolve => setTimeout(() => resolve('timeout'), 100))
      ])
      expect(result).toBe('timeout')
    })
  })

  describe('Complete Logout Flow', () => {
    test('should handle logout flow end-to-end', async () => {
      // Setup: User is logged in
      localStorage.setItem('papairs_token', 'existing-token')
      localStorage.setItem('papairs_user', JSON.stringify({
        id: 'user-123',
        email: 'test@example.com'
      }))

      // Mock logout API response
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({ message: 'Logged out successfully' })
      })

      // Execute logout
      const result = await authController.logout()

      // Verify API was called with auth headers
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/auth/logout'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer existing-token'
          })
        })
      )

      // Verify success response
      expect(result.success).toBe(true)

      // Verify credentials were cleared
      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()

      // Verify user is no longer authenticated
      expect(authController.isLoggedIn()).toBe(false)
      expect(authController.getCurrentUserId()).toBeNull()
    })
  })

  describe('Session Validation Flow', () => {
    test('should validate active session successfully', async () => {
      // Setup: User has existing session
      const existingToken = 'existing-jwt-token'
      localStorage.setItem('papairs_token', existingToken)
      localStorage.setItem('papairs_user', JSON.stringify({
        id: 'user-123',
        email: 'test@example.com'
      }))

      // Mock successful validation
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          valid: true,
          userId: 'user-123'
        })
      })

      // Validate token
      const result = await authController.validateToken()

      // Verify API was called with token
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/auth/validation'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': `Bearer ${existingToken}`
          })
        })
      )

      // Verify validation succeeded
      expect(result.success).toBe(true)
      expect(result.data.valid).toBe(true)

      // Session should still be active
      expect(authController.isLoggedIn()).toBe(true)
    })

    test('should handle invalid/expired session', async () => {
      // Setup: User has expired session
      localStorage.setItem('papairs_token', 'expired-token')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-123' }))

      // Mock 401 response
      const mockHeaders = new Map()
      mockFetch.mockResolvedValue({
        ok: false,
        status: 401,
        headers: {
          get: () => null
        }
      })

      // Validate token (will redirect and not resolve)
      const validatePromise = authController.validateToken()

      // Give it time to process
      await new Promise(resolve => setTimeout(resolve, 50))

      // Should have cleared session and redirected
      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
      expect(window.location.href).toBe('/login')

      // Promise should not resolve
      const result = await Promise.race([
        validatePromise.then(() => 'resolved', () => 'rejected'),
        new Promise(resolve => setTimeout(() => resolve('timeout'), 100))
      ])
      expect(result).toBe('timeout')
    })
  })

  describe('Register and Login Flow', () => {
    test('should register new user then login', async () => {
      const newUser = {
        email: 'newuser@example.com',
        password: 'securepass123'
      }

      // Step 1: Register
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: 'user-new-123',
          email: newUser.email,
          message: 'User registered successfully'
        })
      })

      const registerResult = await authController.register(newUser)
      expect(registerResult.success).toBe(true)
      expect(registerResult.data.email).toBe(newUser.email)

      // User shouldn't be logged in yet
      expect(authController.isLoggedIn()).toBe(false)

      // Step 2: Login with new credentials
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          token: 'new-user-token',
          user: {
            id: 'user-new-123',
            email: newUser.email,
            emailVerified: false,
            active: true
          }
        })
      })

      const loginResult = await authController.login(newUser)
      expect(loginResult.success).toBe(true)

      // Now user should be logged in
      expect(authController.isLoggedIn()).toBe(true)
      expect(authController.getCurrentUserId()).toBe('user-new-123')
      expect(localStorage.getItem('papairs_token')).toBe('new-user-token')
    })
  })
})
