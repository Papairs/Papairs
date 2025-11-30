<script>
import TiptapEditor from '@/components/TiptapEditor.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useTiptapDocument } from '@/composables/useTiptapDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import { createDocumentWebSocketService } from '@/services/documentWebSocketService'

export default {
  name: 'DocsView',
  components: { 
    TiptapEditor
  },
  props: {
    id: {
      type: String,
      required: true
    }
  },
  setup(props) {
    // Initialize error handler
    const errorHandler = createErrorHandler()
    
    // Use document ID from props or default to 'demo'
    const documentId = computed(() => props.id)
    
    // Initialize WebSocket service
    const wsService = createDocumentWebSocketService()
    wsService.init()
    
    // Initialize document manager
    const document = useTiptapDocument(wsService.getClientId())
    
    // UI refs
    const editor = ref(null)
    const isConnected = ref(false)
    const isReceivingUpdate = ref(false)
    const hasError = ref(false)
    const errorMessage = ref('')

    const updateEditorContent = (newContent) => {
      if (!editor.value || editor.value.getHTML() === newContent) return
      
      isReceivingUpdate.value = true
      setTimeout(() => {
        editor.value.commands.setContent(newContent, false)
        setTimeout(() => { isReceivingUpdate.value = false }, 100)
      }, 50)
    }

    // Validate document ID
    if (!props.id) {
      hasError.value = true
      errorMessage.value = 'No document ID provided'
    }

    // Setup WebSocket event handlers
    wsService.on('onConnected', () => {
      errorHandler.safe(() => {
        if (hasError.value) return // Don't try to join if already errored
        
        const userId = wsService.getUserId()
        wsService.joinDocument(documentId.value, userId)
        isConnected.value = true
      }, () => {
        hasError.value = true
        errorMessage.value = 'There was an error finding your page'
      })
    })

    wsService.on('onDisconnected', () => {
      isConnected.value = false
      // If disconnected due to error, don't reconnect
      if (hasError.value) {
        wsService.disconnect()
      }
    })

    wsService.on('onSnapshot', (message) => {
      errorHandler.safe(() => {
        if (message.type === 'error') {
          hasError.value = true
          errorMessage.value = 'There was an error finding your page'
          wsService.disconnect()
          return
        }
        if (document.handleSnapshot(message)) {
          updateEditorContent(document.htmlContent.value)
        }
      })
    })

    wsService.on('onOperation', (message) => {
      errorHandler.safe(() => {
        if (document.handleServerOperation(message)) {
          updateEditorContent(document.htmlContent.value)
        }
      })
    })

    wsService.on('onUserJoined', () => {})
    wsService.on('onUserLeft', () => {})

    wsService.on('onError', (error) => {
      errorHandler.websocket('WebSocket error', error)
      hasError.value = true
      errorMessage.value = 'There was an error finding your page'
    })

    const handleContentChange = (html) => {
      if (isReceivingUpdate.value) return
      
      errorHandler.safe(() => {
        const operation = document.handleHTMLInput(html)
        if (operation && isConnected.value) {
          const userId = wsService.getUserId()
          wsService.sendOperation(documentId.value, operation, userId)
        }
      })
    }
    

    const handleEditorReady = (editorInstance) => {
      editor.value = editorInstance
      if (document.htmlContent.value) {
        editorInstance.commands.setContent(document.htmlContent.value, false)
      }
    }

    // Lifecycle hooks
    onMounted(() => {
      if (!hasError.value) {
        wsService.connect()
      }
    })

    onBeforeUnmount(() => {
      wsService.disconnect()
      document.cleanup()
    })

    return {
      // Document state
      htmlContent: document.htmlContent,
      version: document.version,
      hasPendingOperations: document.hasPendingOperations,
      isLoading: document.isLoading,
      
      // WebSocket state
      connectionState: wsService.connectionState,
      connectionError: wsService.connectionError,
      isConnected,
      
      // Error state
      hasError,
      errorMessage,
      
      // Document info
      documentId,
      
      // Event handlers
      handleContentChange,
      handleEditorReady
    }
  }
}

</script>


<template>
  <div class="flex flex-row h-screen w-full bg-surface-light overflow-hidden">
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Top Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-4">
        <div class="flex items-center gap-4">
          <h1 class="text-lg font-semibold text-gray-800">Document {{ documentId }}</h1>
          
          <!-- Connection Status -->
          <div class="flex items-center gap-2">
            <div 
              :class="[
                'w-2 h-2 rounded-full',
                isConnected ? 'bg-green-500' : 'bg-red-500'
              ]"
            ></div>
            <span class="text-sm text-gray-600">
              {{ isConnected ? 'Connected' : 'Disconnected' }}
            </span>
          </div>
          
          <!-- Loading Status -->
          <div v-if="isLoading" class="flex items-center gap-2 text-sm text-gray-600">
            <div class="animate-spin w-3 h-3 border border-gray-400 border-t-transparent rounded-full"></div>
            <span>Loading...</span>
          </div>
          
          <!-- Pending Operations -->
          <div v-if="hasPendingOperations" class="flex items-center gap-2 text-sm text-orange-600">
            <div class="animate-pulse w-2 h-2 bg-orange-500 rounded-full"></div>
            <span>Syncing...</span>
          </div>
        </div>
      </div>
      
      <!-- Document Content Area -->
      <div class="flex flex-col flex-1 h-full w-full overflow-auto relative">
        <!-- Error Overlay -->
        <div 
          v-if="hasError"
          class="absolute inset-0 bg-white z-50 flex items-center justify-center"
        >
          <div class="text-center">
            <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h2 class="text-2xl font-semibold text-gray-800 mb-2">{{ errorMessage }}</h2>
            <p class="text-gray-600">The document you're looking for doesn't exist or you don't have access to it.</p>
          </div>
        </div>
        
        <!-- Editor -->
        <TiptapEditor
          v-if="!hasError"
          :model-value="htmlContent"
          @content-change="handleContentChange"
          @ready="handleEditorReady"
          placeholder="Start writing your document..."
        />
      </div>
    </div>
  </div>
</template>