/**
 * Integration Tests - Folder Operations Flow
 * Tests complete folder management workflows including navigation and hierarchy
 */

import { FolderController } from '@/controllers/FolderController'

describe('Folder Operations Integration', () => {
  let folderController
  let mockFetch

  beforeEach(() => {
    folderController = new FolderController()
    localStorage.clear()
    
    // Setup auth state
    localStorage.setItem('papairs_token', 'test-token-123')
    localStorage.setItem('papairs_user', JSON.stringify({
      id: 'user-123',
      email: 'test@example.com'
    }))
    
    // Mock window.location
    delete window.location
    window.location = { href: '' }
    
    global.fetch = jest.fn()
    mockFetch = global.fetch
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  describe('Create Folder Hierarchy', () => {
    test('should create nested folder structure', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      // Create root folder
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: 'folder-root-1',
          name: 'Projects',
          parentFolderId: null
        })
      })

      const rootResult = await folderController.createFolder({
        name: 'Projects'
      })

      expect(rootResult.success).toBe(true)
      expect(rootResult.data.id).toBe('folder-root-1')
      expect(rootResult.data.parentFolderId).toBeNull()

      // Verify auth headers were sent
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/docs/folders'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer test-token-123',
            'X-User-Id': 'user-123'
          })
        })
      )

      // Create child folder
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: 'folder-child-1',
          name: 'Frontend',
          parentFolderId: 'folder-root-1'
        })
      })

      const childResult = await folderController.createFolder({
        name: 'Frontend',
        parentFolderId: 'folder-root-1'
      })

      expect(childResult.success).toBe(true)
      expect(childResult.data.parentFolderId).toBe('folder-root-1')

      // Create grandchild folder
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: 'folder-grandchild-1',
          name: 'Components',
          parentFolderId: 'folder-child-1'
        })
      })

      const grandchildResult = await folderController.createFolder({
        name: 'Components',
        parentFolderId: 'folder-child-1'
      })

      expect(grandchildResult.success).toBe(true)
      expect(grandchildResult.data.parentFolderId).toBe('folder-child-1')

      // Verify 3 folders were created
      expect(mockFetch).toHaveBeenCalledTimes(3)
    })
  })

  describe('Folder Navigation Flow', () => {
    test('should navigate folder hierarchy and retrieve path', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      // Get root folders
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue([
          { id: 'folder-1', name: 'Projects', parentFolderId: null },
          { id: 'folder-2', name: 'Archive', parentFolderId: null }
        ])
      })

      const rootsResult = await folderController.getRootFolders()
      expect(rootsResult.success).toBe(true)
      expect(rootsResult.data).toHaveLength(2)

      // Navigate into Projects folder - get children
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue([
          { id: 'folder-1-1', name: 'Frontend', parentFolderId: 'folder-1' },
          { id: 'folder-1-2', name: 'Backend', parentFolderId: 'folder-1' }
        ])
      })

      const childrenResult = await folderController.getChildFolders('folder-1')
      expect(childrenResult.success).toBe(true)
      expect(childrenResult.data).toHaveLength(2)

      // Get path from deep folder to root
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue([
          { id: 'folder-1', name: 'Projects' },
          { id: 'folder-1-1', name: 'Frontend' },
          { id: 'folder-1-1-1', name: 'Components' }
        ])
      })

      const pathResult = await folderController.getFolderPath('folder-1-1-1')
      expect(pathResult.success).toBe(true)
      expect(pathResult.data).toHaveLength(3)
      expect(pathResult.data[0].name).toBe('Projects')
      expect(pathResult.data[2].name).toBe('Components')
    })
  })

  describe('Folder CRUD Operations', () => {
    test('should perform complete folder lifecycle', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      // 1. CREATE
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: 'folder-test-1',
          name: 'Test Folder',
          parentFolderId: null
        })
      })

      const createResult = await folderController.createFolder({
        name: 'Test Folder'
      })
      expect(createResult.success).toBe(true)
      const folderId = createResult.data.id

      // 2. READ
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: folderId,
          name: 'Test Folder',
          parentFolderId: null,
          createdAt: '2025-12-11T10:00:00Z'
        })
      })

      const getResult = await folderController.getFolder(folderId)
      expect(getResult.success).toBe(true)
      expect(getResult.data.id).toBe(folderId)

      // 3. UPDATE (Rename)
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: folderId,
          name: 'Renamed Folder',
          parentFolderId: null
        })
      })

      const renameResult = await folderController.renameFolder(folderId, {
        newName: 'Renamed Folder'
      })
      expect(renameResult.success).toBe(true)
      expect(renameResult.data.name).toBe('Renamed Folder')

      // Verify PATCH was used
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining(`/folders/${folderId}`),
        expect.objectContaining({
          method: 'PATCH'
        })
      )

      // 4. DELETE
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: {
          get: () => null
        }
      })

      const deleteResult = await folderController.deleteFolder(folderId)
      expect(deleteResult.success).toBe(true)
      expect(deleteResult.data).toBeNull() // 204 returns no content

      // Verify DELETE was called
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining(`/folders/${folderId}`),
        expect.objectContaining({
          method: 'DELETE'
        })
      )
    })
  })

  describe('Folder Move Operations', () => {
    test('should move folder to different parent', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      // Create source and destination folders
      const sourceFolderId = 'folder-source'
      const targetFolderId = 'folder-target'

      // Move folder
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: sourceFolderId,
          name: 'Moved Folder',
          parentFolderId: targetFolderId
        })
      })

      const moveResult = await folderController.moveFolder(sourceFolderId, {
        parentFolderId: targetFolderId
      })

      expect(moveResult.success).toBe(true)
      expect(moveResult.data.parentFolderId).toBe(targetFolderId)

      // Verify the move endpoint was called
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining(`/folders/${sourceFolderId}/move`),
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ parentFolderId: targetFolderId })
        })
      )
    })
  })

  describe('Folder Tree Operations', () => {
    test('should get complete folder tree structure', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      // Get user's complete folder tree
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue([
          {
            id: 'folder-1',
            name: 'Projects',
            children: [
              {
                id: 'folder-1-1',
                name: 'Frontend',
                children: []
              },
              {
                id: 'folder-1-2',
                name: 'Backend',
                children: []
              }
            ]
          },
          {
            id: 'folder-2',
            name: 'Archive',
            children: []
          }
        ])
      })

      const treeResult = await folderController.getUserFolderTrees()
      expect(treeResult.success).toBe(true)
      expect(treeResult.data).toHaveLength(2)
      expect(treeResult.data[0].children).toHaveLength(2)
      expect(treeResult.data[0].children[0].name).toBe('Frontend')
    })

    test('should get tree from specific folder', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          id: 'folder-1',
          name: 'Projects',
          children: [
            { id: 'folder-1-1', name: 'Frontend', children: [] }
          ]
        })
      })

      const treeResult = await folderController.getFolderTree('folder-1')
      expect(treeResult.success).toBe(true)
      expect(treeResult.data.id).toBe('folder-1')
      expect(treeResult.data.children).toHaveLength(1)
    })
  })

  describe('Error Handling in Folder Operations', () => {
    test('should handle folder creation errors gracefully', async () => {
      const mockHeaders = new Map([['content-type', 'application/json']])

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        headers: {
          get: (key) => mockHeaders.get(key) || null
        },
        json: jest.fn().mockResolvedValue({
          message: 'Folder name is required'
        })
      })

      const result = await folderController.createFolder({})
      
      expect(result.success).toBe(false)
      expect(result.error).toBe('Folder name is required')
      expect(result.status).toBe(400)
    })

    test('should handle recursive delete', async () => {
      const mockHeaders = new Map()

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: {
          get: () => null
        }
      })

      const result = await folderController.deleteFolder('folder-123', true)
      
      expect(result.success).toBe(true)
      
      // Verify recursive query param was added
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('?recursive=true'),
        expect.any(Object)
      )
    })
  })
})
