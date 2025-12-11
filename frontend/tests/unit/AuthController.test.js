import { AuthController } from '@/controllers/AuthController'

// Mock the BaseApiController
jest.mock('@/controllers/BaseApiController')

describe('AuthController', () => {
  let authController

  beforeEach(() => {
    authController = new AuthController()
    localStorage.clear()
    
    // Reset mocks
    jest.clearAllMocks()
  })

  describe('constructor', () => {
    test('should initialize with correct service path', () => {
      expect(authController.servicePath).toBe('/api/auth')
    })
  })

  describe('login', () => {
    test('should store token and user data on successful login', async () => {
      const mockResponse = {
        success: true,
        data: {
          token: 'test-token-123',
          user: {
            id: 'user-123',
            email: 'test@example.com',
            emailVerified: true,
            active: true
          }
        }
      }

      authController.post = jest.fn().mockResolvedValue(mockResponse)

      const result = await authController.login({
        email: 'test@example.com',
        password: 'password123'
      })

      expect(result.success).toBe(true)
      expect(localStorage.getItem('papairs_token')).toBe('test-token-123')
      
      const storedUser = JSON.parse(localStorage.getItem('papairs_user'))
      expect(storedUser).toEqual({
        id: 'user-123',
        email: 'test@example.com',
        emailVerified: true,
        active: true
      })
    })

    test('should not store data on failed login', async () => {
      const mockResponse = {
        success: false,
        error: 'Invalid credentials'
      }

      authController.post = jest.fn().mockResolvedValue(mockResponse)

      await authController.login({
        email: 'test@example.com',
        password: 'wrong-password'
      })

      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
    })

    test('should handle login with default user properties', async () => {
      const mockResponse = {
        success: true,
        data: {
          token: 'test-token-123',
          user: {
            id: 'user-123',
            email: 'test@example.com'
            // Missing emailVerified and active
          }
        }
      }

      authController.post = jest.fn().mockResolvedValue(mockResponse)

      await authController.login({
        email: 'test@example.com',
        password: 'password123'
      })

      const storedUser = JSON.parse(localStorage.getItem('papairs_user'))
      expect(storedUser.emailVerified).toBe(false)
      expect(storedUser.active).toBe(true)
    })
  })

  describe('register', () => {
    test('should call register endpoint with user details', async () => {
      const userDetails = {
        email: 'newuser@example.com',
        password: 'password123'
      }

      const mockResponse = { success: true, data: { id: 'user-456' } }
      authController.post = jest.fn().mockResolvedValue(mockResponse)

      const result = await authController.register(userDetails)

      expect(authController.post).toHaveBeenCalledWith('/api/auth/register', userDetails)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('logout', () => {
    test('should clear auth data and call logout endpoint', async () => {
      localStorage.setItem('papairs_token', 'test-token')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-123' }))

      const mockResponse = { success: true }
      authController.post = jest.fn().mockResolvedValue(mockResponse)

      const result = await authController.logout()

      expect(authController.post).toHaveBeenCalledWith('/api/auth/logout')
      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
      expect(result).toEqual(mockResponse)
    })

    test('should clear auth data even if API call fails', async () => {
      localStorage.setItem('papairs_token', 'test-token')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-123' }))

      authController.post = jest.fn().mockResolvedValue({ success: false })

      await authController.logout()

      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
    })
  })

  describe('validateToken', () => {
    test('should call validation endpoint', async () => {
      const mockResponse = { success: true, data: { valid: true } }
      authController.post = jest.fn().mockResolvedValue(mockResponse)

      const result = await authController.validateToken()

      expect(authController.post).toHaveBeenCalledWith('/api/auth/validation')
      expect(result).toEqual(mockResponse)
    })
  })

  describe('changePassword', () => {
    test('should call change-password endpoint with password data', async () => {
      const passwordData = {
        currentPassword: 'oldpass123',
        newPassword: 'newpass456'
      }

      const mockResponse = { success: true }
      authController.post = jest.fn().mockResolvedValue(mockResponse)

      const result = await authController.changePassword(passwordData)

      expect(authController.post).toHaveBeenCalledWith('/api/auth/change-password', passwordData)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('isLoggedIn', () => {
    test('should return false when no token exists', () => {
      expect(authController.isLoggedIn()).toBe(false)
    })

    test('should return true when token exists', () => {
      localStorage.setItem('papairs_token', 'test-token')
      expect(authController.isLoggedIn()).toBe(true)
    })
  })

  describe('getCurrentUserId', () => {
    test('should return null when no user data exists', () => {
      expect(authController.getCurrentUserId()).toBeNull()
    })

    test('should return user ID when user data exists', () => {
      const user = { id: 'user-123', email: 'test@example.com' }
      localStorage.setItem('papairs_user', JSON.stringify(user))

      expect(authController.getCurrentUserId()).toBe('user-123')
    })

    test('should return null for invalid JSON', () => {
      localStorage.setItem('papairs_user', 'invalid-json')
      expect(authController.getCurrentUserId()).toBeNull()
    })

    test('should return null when user has no ID', () => {
      localStorage.setItem('papairs_user', JSON.stringify({ email: 'test@example.com' }))
      expect(authController.getCurrentUserId()).toBeNull()
    })
  })

  describe('clearAuthData', () => {
    test('should remove all auth data from localStorage', () => {
      localStorage.setItem('papairs_token', 'test-token')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-123' }))

      authController.clearAuthData()

      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
    })
  })

  describe('checkHealth', () => {
    test('should call health check endpoint', async () => {
      const mockResponse = { success: true, data: { status: 'UP' } }
      authController.get = jest.fn().mockResolvedValue(mockResponse)

      const result = await authController.checkHealth()

      expect(authController.get).toHaveBeenCalledWith('/actuator/health/services/authService')
      expect(result).toEqual(mockResponse)
    })
  })
})
