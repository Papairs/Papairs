<script>
import SidebarBase from '@/components/SidebarBase.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useDocument } from '@/composables/useDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import auth from '@/utils/auth'

export default {
  name: 'DocsView',
  components: { SidebarBase },
  props: {
    id: {
      type: String,
      default: 'demo'
    }
  },
  setup(props) {
    // Initialize error handler and client ID
    const errorHandler = createErrorHandler()
    const clientId = crypto.randomUUID()
    
    // Use document ID from props or default to 'demo'
    const documentId = computed(() => props.id)
    
    // Initialize document and WebSocket
    const document = useDocument(clientId)
    const webSocket = useWebSocket('ws://localhost:8082/ws/doc')
    
    // UI refs
    const textarea = ref(null)

    // WebSocket event handlers
    webSocket.onOpen(() => {
      errorHandler.safe(() => {
        const userId = auth.getUserId() || 'anonymous'
        webSocket.send({ action: 'join', docId: documentId.value, userId: userId })
        console.log('[Application] Joined document session:', documentId.value, 'as user:', userId)
      })
    })

    webSocket.onMessage((event) => {
      errorHandler.safe(() => {
        const message = JSON.parse(event.data)
        console.log('[Application] Message received:', message.type)
        
        const success = message.type === 'snapshot' 
          ? document.handleSnapshot(message)
          : document.handleServerOperation(message)
          
        if (success) {
          updateTextarea()
        } else {
          errorHandler.document('Failed to process server message')
        }
      })
    })

    webSocket.onError((error) => {
      errorHandler.websocket('Connection error', error)
    })

    webSocket.onClose((event) => {
      console.log('[Application] WebSocket closed:', event.code)
    })

    // Text input handling
    function onInput(event) {
      errorHandler.safe(() => {
        // Auto-resize textarea to fit content
        autoResizeTextarea(event.target)
        
        const operation = document.handleTextInput(event.target.value)
        if (operation) {
          // Add user ID to the operation
          const userId = auth.getUserId() || 'anonymous'
          const messageWithUser = { 
            action: 'op', 
            docId: documentId.value, 
            op: operation,
            userId: userId
          }
          if (!webSocket.send(messageWithUser)) {
            errorHandler.websocket('Failed to send operation to server')
          }
        }
        updateTextarea()
      })
    }

    // Auto-resize textarea to fit all content
    function autoResizeTextarea(element) {
      // Reset height to auto to get the correct scrollHeight
      element.style.height = 'auto'
      // Set height to match content
      element.style.height = element.scrollHeight + 'px'
    }

    // Update textarea value to match document state
    function updateTextarea() {
      if (textarea.value && textarea.value.value !== document.text.value) {
        textarea.value.value = document.text.value
        // Auto-resize after content update
        autoResizeTextarea(textarea.value)
      }
    }

    // Lifecycle hooks
    onMounted(() => {
      console.log('[Application] Initializing document editor')
      webSocket.connect()
    })

    onBeforeUnmount(() => {
      console.log('[Application] Cleaning up document editor')
      webSocket.disconnect()
    })

    return {
      // UI refs
      textarea,
      
      // Document state
      text: document.text,
      version: document.version,
      hasPendingOperations: document.hasPendingOperations,
      
      // WebSocket state
      connectionState: webSocket.connectionState,
      connectionError: webSocket.connectionError,
      
      // Document info
      documentId,
      
      // Event handlers
      onInput
    }
  }
}
</script>


<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0"></div>
      <div class="flex flex-col flex-1 w-full justify-center items-center overflow-hidden">
        <div class="flex flex-row h-[50px] w-[1200px] border-b border-border-light-subtle flex-shrink-0"></div>
        <div class="flex flex-row flex-1 w-full overflow-y-auto overflow-x-hidden">
          <div class="flex flex-row w-full justify-center items-start">
            <textarea
              ref="textarea"
              :value="text"
              @input="onInput"
              name="document-content"
              id="document-editor"
              placeholder="Start writing your document..."
              class="w-[1000px] bg-white border-x border-border-light-subtle px-12 resize-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12 overflow-hidden min-h-[500px]"
              style="height: auto; min-height: 100%;"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>