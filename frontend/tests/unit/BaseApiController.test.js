import { BaseApiController } from '@/controllers/BaseApiController'

describe('BaseApiController', () => {
  let controller
  let mockFetch

  beforeEach(() => {
    controller = new BaseApiController('http://localhost:8080')
    localStorage.clear()
    
    // Mock window.location
    delete window.location
    window.location = { href: '' }
    
    // Mock fetch
    global.fetch = jest.fn()
    mockFetch = global.fetch
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  describe('constructor', () => {
    test('should initialize with baseURL and default headers', () => {
      expect(controller.baseURL).toBe('http://localhost:8080')
      expect(controller.defaultHeaders).toEqual({
        'Content-Type': 'application/json'
      })
    })
  })

  describe('getAuthHeaders', () => {
    test('should return empty object when no token exists', () => {
      expect(controller.getAuthHeaders()).toEqual({})
    })

    test('should return Authorization header when token exists', () => {
      localStorage.setItem('papairs_token', 'test-token-123')
      expect(controller.getAuthHeaders()).toEqual({
        'Authorization': 'Bearer test-token-123'
      })
    })
  })

  describe('getUserHeaders', () => {
    test('should return empty object when no user data exists', () => {
      expect(controller.getUserHeaders()).toEqual({})
    })

    test('should return X-User-Id header when user exists', () => {
      const user = { id: 'user-123', email: 'test@example.com' }
      localStorage.setItem('papairs_user', JSON.stringify(user))
      
      expect(controller.getUserHeaders()).toEqual({
        'X-User-Id': 'user-123'
      })
    })

    test('should handle invalid JSON gracefully', () => {
      localStorage.setItem('papairs_user', 'invalid-json')
      expect(controller.getUserHeaders()).toEqual({})
    })
  })

  describe('getHeaders', () => {
    test('should combine all headers', () => {
      localStorage.setItem('papairs_token', 'test-token')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-1' }))
      
      const headers = controller.getHeaders({ 'Custom-Header': 'value' })
      
      expect(headers).toEqual({
        'Content-Type': 'application/json',
        'Authorization': 'Bearer test-token',
        'X-User-Id': 'user-1',
        'Custom-Header': 'value'
      })
    })
  })

  describe('handleResponse', () => {
    test('should handle 401 status by clearing auth and redirecting', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])
      const mockResponse = {
        ok: false,
        status: 401,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        }
      }

      localStorage.setItem('papairs_token', 'test-token')
      localStorage.setItem('papairs_user', JSON.stringify({ id: 'user-1' }))

      const promise = controller.handleResponse(mockResponse)

      // Give it a moment to execute the synchronous parts
      await new Promise(resolve => setTimeout(resolve, 10))

      // Should clear auth data
      expect(localStorage.getItem('papairs_token')).toBeNull()
      expect(localStorage.getItem('papairs_user')).toBeNull()
      
      // Should redirect
      expect(window.location.href).toBe('/login')
      
      // Promise should never resolve
      const resolved = await Promise.race([
        promise.then(() => 'resolved'),
        new Promise(resolve => setTimeout(() => resolve('timeout'), 100))
      ])
      expect(resolved).toBe('timeout')
    })

    test('should parse JSON response on success', async () => {
      const mockData = { id: 1, name: 'Test' }
      const mockHeaders = new Map([['content-type', 'application/json']])
      const mockResponse = {
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue(mockData)
      }

      const result = await controller.handleResponse(mockResponse)
      expect(result).toEqual(mockData)
    })

    test('should return null for 204 No Content', async () => {
      const mockResponse = {
        ok: true,
        status: 204,
        headers: {
          get: () => null
        }
      }

      const result = await controller.handleResponse(mockResponse)
      expect(result).toBeNull()
    })

    test('should throw error with message for non-401 errors', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])
      const mockResponse = {
        ok: false,
        status: 404,
        statusText: 'Not Found',
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({ message: 'Resource not found' })
      }

      await expect(controller.handleResponse(mockResponse)).rejects.toThrow('Resource not found')
    })
  })

  describe('makeRequest', () => {
    test('should make GET request successfully', async () => {
      const mockData = { id: 1, name: 'Test' }
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue(mockData)
      })

      const result = await controller.makeRequest('GET', '/test')
      
      expect(result).toEqual({
        success: true,
        data: mockData,
        status: 200
      })
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({ method: 'GET' })
      )
    })

    test('should handle trailing slashes in baseURL and endpoint', async () => {
      controller.baseURL = 'http://localhost:8080/'
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({})
      })

      await controller.makeRequest('GET', '/test')
      
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.any(Object)
      )
    })

    test('should include body for POST requests', async () => {
      const postData = { name: 'Test' }
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: true,
        status: 201,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue(postData)
      })

      await controller.makeRequest('POST', '/test', postData)
      
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(postData)
        })
      )
    })

    test('should return error object for failed requests', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({ message: 'Server error' })
      })

      const result = await controller.makeRequest('GET', '/test')
      
      expect(result.success).toBe(false)
      expect(result.status).toBe(500)
      expect(result.error).toBe('Server error')
    })

    test('should handle 401 errors and redirect', async () => {
      const mockHeaders = new Map()
      mockFetch.mockResolvedValue({
        ok: false,
        status: 401,
        headers: {
          get: () => null
        }
      })

      // Start the request (which will redirect and not resolve)
      const promise = controller.makeRequest('GET', '/test')

      // Give it a moment to process the redirect
      await new Promise(resolve => setTimeout(resolve, 50))

      // Should have redirected
      expect(window.location.href).toBe('/login')
      
      // Promise should still be pending (race condition test)
      const result = await Promise.race([
        promise.then(() => 'resolved', () => 'rejected'),
        new Promise(resolve => setTimeout(() => resolve('timeout'), 100))
      ])
      expect(result).toBe('timeout')
    })
  })

  describe('HTTP method shortcuts', () => {
    beforeEach(() => {
      const mockHeaders = new Map([['content-type', 'application/json']])
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({ success: true })
      })
    })

    test('get() should make GET request', async () => {
      await controller.get('/test')
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({ method: 'GET' })
      )
    })

    test('post() should make POST request with data', async () => {
      const data = { name: 'Test' }
      await controller.post('/test', data)
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(data)
        })
      )
    })

    test('put() should make PUT request with data', async () => {
      const data = { name: 'Updated' }
      await controller.put('/test', data)
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(data)
        })
      )
    })

    test('patch() should make PATCH request with data', async () => {
      const data = { name: 'Patched' }
      await controller.patch('/test', data)
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify(data)
        })
      )
    })

    test('delete() should make DELETE request', async () => {
      await controller.delete('/test')
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/test',
        expect.objectContaining({ method: 'DELETE' })
      )
    })
  })
})
