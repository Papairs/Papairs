<script>
import SidebarBase from '@/components/SidebarBase.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { useDocument } from '@/composables/useDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import auth from '@/utils/auth'
import { marked } from 'marked'

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

    // Markdown functionality
    const markdownInput = ref('')
    
    // Configure marked for security
    marked.setOptions({
      sanitize: false, // We'll handle sanitization if needed
      breaks: true,
      gfm: true
    })

    // Computed markdown output
    const compiledMarkdown = computed(() => {
      return marked(markdownInput.value || '')
    })

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

    // Simple debounce function
    function debounce(func, wait) {
      let timeout
      return function executedFunction(...args) {
        const later = () => {
          clearTimeout(timeout)
          func(...args)
        }
        clearTimeout(timeout)
        timeout = setTimeout(later, wait)
      }
    }

    // Debounced markdown update
    const updateMarkdown = debounce((value) => {
      markdownInput.value = value
    }, 300)

    // Text input handling
    function onInput(event) {
      errorHandler.safe(() => {
        // Auto-resize textarea to fit content
        autoResizeTextarea(event.target)
        
        // Update markdown with debounce
        updateMarkdown(event.target.value)
        
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
        // Update markdown immediately for external changes
        markdownInput.value = document.text.value
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
      
      // Markdown state
      markdownInput,
      compiledMarkdown,
      
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
            <!-- Editor Panel -->
            <div class="w-[500px] bg-white border-r border-border-light-subtle">
              <textarea
                ref="textarea"
                :value="text"
                @input="onInput"
                name="document-content"
                id="document-editor"
                placeholder="Start writing your markdown document..."
                class="w-full bg-white px-6 resize-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12 overflow-hidden min-h-[500px]"
                style="height: auto;"
              ></textarea>
            </div>
            
            <!-- Markdown Preview Panel -->
            <div class="w-[500px] bg-white border-l border-border-light-subtle">
              <div 
                class="px-6 py-12 prose prose-gray max-w-none overflow-hidden min-h-[500px]"
                v-html="compiledMarkdown"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>