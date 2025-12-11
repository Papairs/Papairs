import { safe, retry, createErrorHandler } from '@/utils/errorHandler'

describe('errorHandler', () => {
  let consoleSpy

  beforeEach(() => {
    consoleSpy = {
      error: jest.spyOn(console, 'error').mockImplementation(),
      warn: jest.spyOn(console, 'warn').mockImplementation()
    }
  })

  afterEach(() => {
    consoleSpy.error.mockRestore()
    consoleSpy.warn.mockRestore()
  })

  describe('safe', () => {
    test('should execute sync function and return result', () => {
      const fn = () => 'success'
      const result = safe(fn)
      expect(result).toBe('success')
    })

    test('should catch sync errors and return fallback', () => {
      const fn = () => {
        throw new Error('Sync error')
      }
      const result = safe(fn, 'TestContext', 'fallback-value')
      
      expect(result).toBe('fallback-value')
      expect(consoleSpy.error).toHaveBeenCalledWith(
        '[TestContext] Operation failed:',
        expect.any(Error)
      )
    })

    test('should handle async function success', async () => {
      const fn = async () => 'async-success'
      const result = await safe(fn)
      expect(result).toBe('async-success')
    })

    test('should catch async errors and return fallback', async () => {
      const fn = async () => {
        throw new Error('Async error')
      }
      const result = await safe(fn, 'AsyncContext', null)
      
      expect(result).toBeNull()
      expect(consoleSpy.error).toHaveBeenCalledWith(
        '[AsyncContext] Async operation failed:',
        expect.any(Error)
      )
    })

    test('should use default context when not provided', () => {
      const fn = () => {
        throw new Error('Test error')
      }
      safe(fn, undefined, 'default')
      
      expect(consoleSpy.error).toHaveBeenCalledWith(
        '[Operation] Operation failed:',
        expect.any(Error)
      )
    })

    test('should return null as default fallback', () => {
      const fn = () => {
        throw new Error('Test error')
      }
      const result = safe(fn)
      expect(result).toBeNull()
    })
  })

  describe('retry', () => {
    test('should return result on first success', async () => {
      const fn = jest.fn().mockResolvedValue('success')
      const result = await retry(fn, 3, 100)
      
      expect(result).toBe('success')
      expect(fn).toHaveBeenCalledTimes(1)
    })

    test('should retry on failure and eventually succeed', async () => {
      const fn = jest.fn()
        .mockRejectedValueOnce(new Error('Attempt 1 failed'))
        .mockRejectedValueOnce(new Error('Attempt 2 failed'))
        .mockResolvedValueOnce('success')
      
      const result = await retry(fn, 3, 10)
      
      expect(result).toBe('success')
      expect(fn).toHaveBeenCalledTimes(3)
    })

    test('should throw error after max attempts', async () => {
      const error = new Error('Persistent failure')
      const fn = jest.fn().mockRejectedValue(error)
      
      await expect(retry(fn, 3, 10)).rejects.toThrow('Persistent failure')
      expect(fn).toHaveBeenCalledTimes(3)
    })

    test('should use exponential backoff', async () => {
      const fn = jest.fn()
        .mockRejectedValueOnce(new Error('Fail 1'))
        .mockRejectedValueOnce(new Error('Fail 2'))
        .mockResolvedValueOnce('success')
      
      const startTime = Date.now()
      await retry(fn, 3, 10)
      const duration = Date.now() - startTime
      
      // Should wait 10ms + 20ms = 30ms minimum (with some margin for execution)
      expect(duration).toBeGreaterThanOrEqual(25)
    })

    test('should log retry attempts', async () => {
      const fn = jest.fn()
        .mockRejectedValueOnce(new Error('Fail'))
        .mockResolvedValueOnce('success')
      
      await retry(fn, 3, 10, 'RetryTest')
      
      expect(consoleSpy.warn).toHaveBeenCalledWith(
        expect.stringContaining('[RetryTest] Attempt 1/3 failed')
      )
    })
  })

  describe('createErrorHandler', () => {
    test('should create handler with all methods', () => {
      const handler = createErrorHandler()
      
      expect(handler).toHaveProperty('websocket')
      expect(handler).toHaveProperty('document')
      expect(handler).toHaveProperty('safe')
      expect(handler).toHaveProperty('retry')
      expect(typeof handler.websocket).toBe('function')
      expect(typeof handler.document).toBe('function')
      expect(typeof handler.safe).toBe('function')
      expect(typeof handler.retry).toBe('function')
    })

    test('websocket method should log with WebSocket context', () => {
      const handler = createErrorHandler()
      const error = new Error('WebSocket error')
      
      handler.websocket('Connection failed', error)
      
      expect(consoleSpy.error).toHaveBeenCalledWith(
        '[WebSocket] Connection failed:',
        error
      )
    })

    test('document method should log with Document context', () => {
      const handler = createErrorHandler()
      const error = new Error('Document error')
      
      handler.document('Save failed', error)
      
      expect(consoleSpy.error).toHaveBeenCalledWith(
        '[Document] Save failed:',
        error
      )
    })

    test('safe method should work correctly', () => {
      const handler = createErrorHandler()
      const fn = () => 'test-result'
      
      const result = handler.safe(fn, 'fallback')
      expect(result).toBe('test-result')
    })

    test('safe method should return fallback on error', () => {
      const handler = createErrorHandler()
      const fn = () => {
        throw new Error('Test error')
      }
      
      const result = handler.safe(fn, 'fallback-value')
      expect(result).toBe('fallback-value')
    })

    test('retry method should work correctly', async () => {
      const handler = createErrorHandler()
      const fn = jest.fn().mockResolvedValue('retry-success')
      
      const result = await handler.retry(fn, 2)
      expect(result).toBe('retry-success')
      expect(fn).toHaveBeenCalledTimes(1)
    })
  })
})
