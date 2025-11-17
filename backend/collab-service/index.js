const { Server } = require('@hocuspocus/server')
const { Logger } = require('@hocuspocus/extension-logger')
const axios = require('axios')

const PORT = process.env.PORT || 3004
const DOCS_SERVICE_URL = process.env.DOCS_SERVICE_URL || 'http://docs-service:8082'
const SAVE_DELAY_MS = 5000 // 5 seconds

// Track document save timers and user context
const saveTimers = new Map()
const documentUsers = new Map() // Map document names to user IDs

/**
 * Schedule a delayed save for a document (5 seconds after last change)
 */
function scheduleDocumentSave(documentName, ydoc, userId) {
  console.log('⏰ Scheduling save for document:', {
    document: documentName,
    userId: userId,
    delay: `${SAVE_DELAY_MS}ms`,
    hasExistingTimer: saveTimers.has(documentName)
  })
  
  // Store user ID for this document
  documentUsers.set(documentName, userId)
  
  // Clear existing timer if any
  if (saveTimers.has(documentName)) {
    console.log('🔄 Clearing existing save timer for:', documentName)
    clearTimeout(saveTimers.get(documentName))
  }

  // Schedule new save timer
  const timer = setTimeout(async () => {
    console.log('⏰ Save timer triggered for:', documentName)
    await saveDocumentImmediately(documentName, ydoc, userId)
    saveTimers.delete(documentName)
  }, SAVE_DELAY_MS)

  saveTimers.set(documentName, timer)
  console.log('✅ Save timer scheduled for:', documentName)
}

/**
 * Save document immediately to docs-service
 */
async function saveDocumentImmediately(documentName, ydoc, userId) {
  console.log('💾 Starting immediate save for:', {
    document: documentName,
    userId: userId,
    hasTimer: saveTimers.has(documentName)
  })
  
  try {
    // Clear any pending timer
    if (saveTimers.has(documentName)) {
      console.log('🔄 Clearing save timer before immediate save:', documentName)
      clearTimeout(saveTimers.get(documentName))
      saveTimers.delete(documentName)
    }

    // Get user ID from stored context or parameter
    const actualUserId = userId || documentUsers.get(documentName) || 'anonymous-user'
    console.log('👤 Using user ID for save:', {
      document: documentName,
      passedUserId: userId,
      storedUserId: documentUsers.get(documentName),
      actualUserId: actualUserId
    })

    // Extract text content from Y.Doc
    console.log('🔍 Inspecting Y.Doc structure:', {
      document: documentName,
      yDocKeys: Array.from(ydoc.share.keys()),
      yDocSize: ydoc.share.size
    })
    
    // Try different ways to extract content from Y.Doc
    let content = ''
    let extractionMethod = 'none'
    
    // Method 1: Check what shared types exist and their details
    try {
      console.log('🔍 Available shared types:')
      for (const [key, sharedType] of ydoc.share) {
        console.log(`  - "${key}": ${sharedType.constructor.name} (length: ${sharedType.length || 0})`)
        
        // For debugging: try different ways to inspect the type
        console.log(`    Type details:`, {
          typeName: sharedType.constructor.name,
          hasToString: typeof sharedType.toString === 'function',
          hasToJSON: typeof sharedType.toJSON === 'function',
          length: sharedType.length || 0,
          keys: sharedType.share ? Array.from(sharedType.share.keys()) : 'no share',
        })
      }
    } catch (e) {
      console.log('Error inspecting shared types:', e.message)
    }
    
    // Method 2: Try the standard Tiptap approach - get XmlFragment for ProseMirror
    try {
      const prosemirrorFragment = ydoc.getXmlFragment('default')
      
      if (prosemirrorFragment) {
        console.log('🔍 Found ProseMirror fragment:', {
          type: prosemirrorFragment.constructor.name,
          toString: typeof prosemirrorFragment.toString,
          length: prosemirrorFragment.length || 0
        })
        
        // Try to get content from ProseMirror structure
        const xmlString = prosemirrorFragment.toString()
        console.log('ProseMirror XML content:', xmlString)
        
        if (xmlString && xmlString.length > 0) {
          console.log('🔄 Converting ProseMirror XML to formatted text...')
          
          // Convert ProseMirror XML to formatted text with proper line breaks
          content = xmlString
            // Replace paragraph tags with line breaks
            .replace(/<paragraph>/g, '')
            .replace(/<\/paragraph>/g, '\n')
            // Replace hard breaks with line breaks
            .replace(/<hardbreak><\/hardbreak>/g, '\n')
            .replace(/<hardbreak\/>/g, '\n')
            // Replace heading tags with line breaks
            .replace(/<heading[^>]*>/g, '')
            .replace(/<\/heading>/g, '\n')
            // Replace list items with line breaks and bullets
            .replace(/<listItem>/g, '• ')
            .replace(/<\/listItem>/g, '\n')
            // Replace ordered list items
            .replace(/<orderedList[^>]*>/g, '')
            .replace(/<\/orderedList>/g, '\n')
            .replace(/<bulletList[^>]*>/g, '')
            .replace(/<\/bulletList>/g, '\n')
            // Handle text formatting (keep the text, remove tags)
            .replace(/<strong>/g, '**')
            .replace(/<\/strong>/g, '**')
            .replace(/<em>/g, '*')
            .replace(/<\/em>/g, '*')
            .replace(/<code>/g, '`')
            .replace(/<\/code>/g, '`')
            // Remove any remaining XML tags
            .replace(/<[^>]*>/g, '')
            // Clean up extra whitespace but preserve intentional line breaks
            .replace(/\n\s*\n/g, '\n\n') // Convert multiple newlines to double newline
            .replace(/^\s+|\s+$/g, '') // Trim start and end
            .replace(/\n\s+/g, '\n') // Remove spaces after newlines
            .trim()
          
          console.log('📝 Formatted content result:', {
            originalXmlLength: xmlString.length,
            formattedLength: content.length,
            formattedPreview: content.substring(0, 200) + (content.length > 200 ? '...' : ''),
            lineBreaks: (content.match(/\n/g) || []).length
          })
          
          extractionMethod = 'prosemirror-fragment-formatted'
        }
      }
    } catch (e) {
      console.log('ProseMirror fragment extraction failed:', e.message)
    }
    
    // Method 3: Try getting YText if ProseMirror doesn't work
    if (!content) {
      try {
        const yText = ydoc.getText('default')
        
        if (yText) {
          console.log('🔍 Found YText:', {
            type: yText.constructor.name,
            length: yText.length || 0,
            toString: typeof yText.toString
          })
          
          const textContent = yText.toString()
          console.log('YText content:', JSON.stringify(textContent))
          
          if (textContent && textContent.length > 0) {
            content = textContent
            extractionMethod = 'ytext'
          }
        }
      } catch (e) {
        console.log('YText extraction failed:', e.message)
      }
    }
    
    // Method 4: Try direct access to shared types with proper casting
    if (!content) {
      try {
        if (ydoc.share.has('default')) {
          const defaultType = ydoc.share.get('default')
          
          // Check the actual class name and try appropriate methods
          if (defaultType.constructor.name === 'YXmlFragment' || 
              defaultType.constructor.name === 'AbstractType') {
            
            console.log('🔍 Trying direct type access:', defaultType.constructor.name)
            
            // Try toString method
            if (typeof defaultType.toString === 'function') {
              const stringContent = defaultType.toString()
              console.log('Direct toString result:', JSON.stringify(stringContent))
              
              if (stringContent && stringContent.length > 0) {
                content = stringContent.replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
                extractionMethod = 'direct-toString'
              }
            }
            
            // Try toJSON if toString didn't work
            if (!content && typeof defaultType.toJSON === 'function') {
              const jsonContent = defaultType.toJSON()
              console.log('Direct toJSON result:', jsonContent)
              
              if (jsonContent && typeof jsonContent === 'string' && jsonContent.length > 0) {
                content = jsonContent
                extractionMethod = 'direct-toJSON'
              }
            }
          }
        }
      } catch (e) {
        console.log('Direct type access failed:', e.message)
      }
    }
    
    // Method 5: Try using Y.js low-level API
    if (!content) {
      try {
        console.log('🔍 Trying Y.js low-level extraction')
        
        // Iterate through the document to find any text content
        const extractFromYDoc = (doc) => {
          let foundText = ''
          
          // Try to get any shared type and extract content
          for (const [key, sharedType] of doc.share) {
            console.log(`Examining shared type "${key}":`, sharedType.constructor.name)
            
            // Different approaches for different types
            if (sharedType._start) {
              console.log('Type has _start property, trying to iterate')
              let item = sharedType._start
              while (item) {
                if (item.content && item.content.str) {
                  foundText += item.content.str
                }
                if (item.content && item.content.type && item.content.type._start) {
                  // Recursive search for nested content
                  let nestedItem = item.content.type._start
                  while (nestedItem) {
                    if (nestedItem.content && nestedItem.content.str) {
                      foundText += nestedItem.content.str
                    }
                    nestedItem = nestedItem.right
                  }
                }
                item = item.right
              }
            }
          }
          
          return foundText
        }
        
        content = extractFromYDoc(ydoc)
        if (content) {
          extractionMethod = 'low-level-iteration'
        }
        
        console.log('Low-level extraction result:', JSON.stringify(content))
        
      } catch (e) {
        console.log('Low-level extraction failed:', e.message)
      }
    }
    
    console.log('📄 Content extraction result:', {
      document: documentName,
      extractionMethod: extractionMethod,
      contentLength: content.length,
      contentPreview: content.substring(0, 200) + (content.length > 200 ? '...' : ''),
      rawContent: content.length < 100 ? JSON.stringify(content) : 'too long to show'
    })

    // Prepare API request
    const apiUrl = `${DOCS_SERVICE_URL}/api/docs/pages/${documentName}`
    const requestData = { content }
    const requestHeaders = {
      'Content-Type': 'application/json',
      'X-User-Id': actualUserId
    }
    
    console.log('🔗 Making API request:', {
      url: apiUrl,
      method: 'PUT',
      headers: requestHeaders,
      bodySize: JSON.stringify(requestData).length
    })

    // Call docs-service API to save with the actual user ID
    const response = await axios.put(apiUrl, requestData, { headers: requestHeaders })

    console.log('✅ Document saved to database:', {
      document: documentName,
      contentLength: content.length,
      content: content,
      userId: actualUserId,
      responseStatus: response.status,
      responseData: response.data
    })
  } catch (error) {
    console.error('❌ Failed to save document:', {
      document: documentName,
      userId: userId,
      error: error.response?.data || error.message,
      status: error.response?.status,
      url: error.config?.url,
      headers: error.config?.headers,
      requestData: error.config?.data
    })
    
    if (error.response) {
      console.error('📋 Full error response:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data,
        headers: error.response.headers
      })
    }
  }
}

// Create Hocuspocus server with minimal, working configuration
const server = Server.configure({
  port: PORT,
  
  extensions: [
    new Logger(),
  ],

  // Simple authentication - accept all connections and extract user ID
  async onAuthenticate(data) {
    const userId = data?.token || 'anonymous-user'
    console.log('🔐 Client authenticating for document:', {
      document: data.documentName,
      userId: userId
    })
    
    // Store user ID for this document
    documentUsers.set(data.documentName, userId)
    
    return {
      user: {
        id: userId,
        name: `User ${userId}`
      }
    }
  },

  // Document lifecycle hooks
  async onLoadDocument(data) {
    console.log('📄 Loading document:', {
      document: data.documentName,
      timestamp: new Date().toISOString()
    })
    
    // Get user ID for this document
    const userId = documentUsers.get(data.documentName) || 'anonymous-user'
    console.log('👤 Using user ID for document load:', {
      document: data.documentName,
      userId: userId
    })
    
    try {
      const apiUrl = `${DOCS_SERVICE_URL}/api/docs/pages/${data.documentName}`
      const requestHeaders = { 'X-User-Id': userId }
      
      console.log('🔗 Making document load request:', {
        url: apiUrl,
        headers: requestHeaders
      })
      
      // Try to fetch existing document content from docs-service
      const response = await axios.get(apiUrl, { headers: requestHeaders })
      
      if (response.data && response.data.content) {
        console.log('✅ Loaded existing document content:', {
          document: data.documentName,
          contentLength: response.data.content.length,
          owner: response.data.ownerId,
          userId: userId,
          responseStatus: response.status,
          contentPreview: response.data.content.substring(0, 100) + (response.data.content.length > 100 ? '...' : '')
        })
        return response.data.content
      } else {
        console.log('📄 Empty response data:', {
          document: data.documentName,
          responseData: response.data
        })
      }
    } catch (error) {
      console.log('📄 Document load failed:', {
        document: data.documentName,
        userId: userId,
        error: error.response?.status || error.message,
        errorData: error.response?.data,
        message: 'Starting with empty document'
      })
      
      if (error.response) {
        console.error('📋 Full load error response:', {
          status: error.response.status,
          statusText: error.response.statusText,
          data: error.response.data
        })
      }
    }
    
    // Return null to start with empty document if not found or no access
    console.log('📄 Returning null (empty document):', data.documentName)
    return null
  },

  async onStoreDocument(data) {
    console.log('💾 Storing document:', data.documentName)
    // This is called by Hocuspocus when document should be persisted
    // We'll use our delayed save mechanism instead
  },

  // Connection hooks
  async onConnect(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 1
    console.log('� Client connected:', {
      document: data.documentName,
      connections: connectionCount
    })
  },

  async onDisconnect(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 0
    const userId = documentUsers.get(data.documentName) || 'anonymous-user'
    
    console.log('❌ Client disconnected:', {
      document: data.documentName,
      connections: connectionCount,
      userId: userId,
      willSaveImmediately: connectionCount === 0
    })

    // If last user disconnected, save immediately
    if (connectionCount === 0) {
      console.log('👋 Last user disconnected, saving immediately:', data.documentName)
      await saveDocumentImmediately(data.documentName, data.document, userId)
    }
  },

  async onChange(data) {
    const connectionCount = data.instance.documents.get(data.documentName)?.getConnectionsCount() || 0
    const userId = documentUsers.get(data.documentName) || 'anonymous-user'
    
    console.log('📝 Document changed:', {
      document: data.documentName,
      connections: connectionCount,
      userId: userId,
      timestamp: new Date().toISOString()
    })

    // Schedule delayed save to docs-service
    scheduleDocumentSave(data.documentName, data.document, userId)
  },
})

// Start server
server.listen().then(() => {
  console.log('✅ Hocuspocus server started successfully')
  console.log(`   WebSocket: ws://localhost:${PORT}`)
  console.log(`   HTTP: http://localhost:${PORT}`)
  console.log('   Ready for connections!')
})
