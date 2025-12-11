import auth from '@/utils/auth'

describe('Auth Utility', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear()
  })

  describe('getToken', () => {
    test('should return null when no token is stored', () => {
      expect(auth.getToken()).toBeNull()
    })

    test('should return token when stored', () => {
      localStorage.setItem('papairs_token', 'test-token-123')
      expect(auth.getToken()).toBe('test-token-123')
    })
  })

  describe('isAuthenticated', () => {
    test('should return false when no token exists', () => {
      expect(auth.isAuthenticated()).toBe(false)
    })

    test('should return true when token exists', () => {
      localStorage.setItem('papairs_token', 'test-token-123')
      expect(auth.isAuthenticated()).toBe(true)
    })
  })

  describe('setAuthData', () => {
    test('should store token and user data', () => {
      const token = 'test-token-123'
      const user = {
        id: 'user-123',
        email: 'test@example.com',
        emailVerified: true,
        isActive: true
      }

      auth.setAuthData(token, user)

      expect(localStorage.getItem('papairs_token')).toBe(token)
      expect(JSON.parse(localStorage.getItem('papairs_user'))).toEqual(user)
    })
  })

  describe('logout', () => {
    test('should clear all auth data', () => {
      localStorage.setItem('papairs_token', 'test-token-123')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-123' }))

      auth.logout()

      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
    })
  })

  describe('getUserId', () => {
    test('should return null when no user is stored', () => {
      expect(auth.getUserId()).toBeNull()
    })

    test('should return user ID when user is stored', () => {
      const user = { id: 'user-123', email: 'test@example.com' }
      localStorage.setItem('papairs_user', JSON.stringify(user))

      expect(auth.getUserId()).toBe('user-123')
    })
  })

  describe('getAuthHeader', () => {
    test('should return empty object when no token exists', () => {
      expect(auth.getAuthHeader()).toEqual({})
    })

    test('should return Authorization header when token exists', () => {
      localStorage.setItem('papairs_token', 'test-token-123')

      expect(auth.getAuthHeader()).toEqual({
        'Authorization': 'Bearer test-token-123'
      })
    })
  })
})
