<script>
import TiptapEditor from '@/components/TiptapEditor.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import TrashIcon from '@/components/icons/TrashIcon.vue'
import { useTiptapDocument } from '@/composables/useTiptapDocument'
import { createErrorHandler } from '@/utils/errorHandler'
import { createDocumentWebSocketService } from '@/services/documentWebSocketService'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'DocsView',
  components: { 
    TiptapEditor,
    TrashIcon
  },
  props: {
    id: {
      type: String,
      required: true
    }
  },
  setup(props) {
    // Initialize router and error handler
    const router = useRouter()
    const errorHandler = createErrorHandler()
    
    // Use document ID from props or default to 'demo'
    const documentId = computed(() => props.id)
    
    // Initialize WebSocket service
    const wsService = createDocumentWebSocketService()
    wsService.init()
    
    // Initialize document manager
    const document = useTiptapDocument(wsService.getClientId())
    
    const editor = ref(null)
    const isGenerating = ref(false)    
    const isConnected = ref(false)
    const isReceivingUpdate = ref(false)
    const hasError = ref(false)
    const errorMessage = ref('')
    
    // Flashcard state
    const showSuccess = ref(false)
    const numberOfCards = ref(5)
    const showFlashcards = ref(false)
    const pageFlashcards = ref([])
    const isLoadingFlashcards = ref(false)

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

    async function generateFlashcards() {
      const content = document.text.value.trim()
      if (!content || content.length < 50) {
        alert('Please write at least 50 characters before generating flashcards.')
        return
      }
      isGenerating.value = true
      showSuccess.value = false
      try {
        const aiResponse = await axios.post('http://localhost:3001/generate-flashcards', {
          content: content,
          numberOfCards: numberOfCards.value
        })
        const flashcards = aiResponse.data.flashcards
        if (!flashcards || flashcards.length === 0) {
          throw new Error('No flashcards generated')
        }
        const headers = await auth.getAuthHeaders(router)
        for (const card of flashcards) {
          await axios.post('http://localhost:8082/api/docs/flashcards', {
            pageId: documentId.value,
            question: card.question,
            answer: card.answer,
            tags: []
          }, {
            headers: { ...headers, 'Content-Type': 'application/json' }
          })
        }
        showSuccess.value = true
        setTimeout(() => { showSuccess.value = false }, 3000)
        console.log('Successfully generated and saved ' + flashcards.length + ' flashcards')
        if (showFlashcards.value) {
          await loadPageFlashcards()
        }
      } catch (error) {
        console.error('Failed to generate flashcards:', error)
        alert('Failed to generate flashcards. Please try again.')
      } finally {
        isGenerating.value = false
      }
    }

    async function loadPageFlashcards() {
      isLoadingFlashcards.value = true
      try {
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get('http://localhost:8082/api/docs/flashcards/page/' + documentId.value, { headers })
        pageFlashcards.value = response.data.data || []
      } catch (error) {
        console.error('Failed to load flashcards:', error)
        pageFlashcards.value = []
      } finally {
        isLoadingFlashcards.value = false
      }
    }

    async function toggleFlashcardsPanel() {
      showFlashcards.value = !showFlashcards.value
      if (showFlashcards.value && pageFlashcards.value.length === 0) {
        await loadPageFlashcards()
      }
    }

    async function deleteFlashcard(flashcardId) {
      if (!confirm('Are you sure you want to delete this flashcard?')) {
        return
      }
      try {
        const headers = await auth.getAuthHeaders(router)
        await axios.delete('http://localhost:8082/api/docs/flashcards/' + flashcardId, { headers })
        await loadPageFlashcards()
      } catch (error) {
        console.error('Failed to delete flashcard:', error)
        alert('Failed to delete flashcard. Please try again.')
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
      text: document.text,
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
      handleEditorReady,
      
      // Flashcard functionality
      isGenerating,
      showSuccess,
      numberOfCards,
      showFlashcards,
      pageFlashcards,
      isLoadingFlashcards,
      generateFlashcards,
      toggleFlashcardsPanel,
      deleteFlashcard
    }
  }
}

</script>

<template>
  <div class="flex flex-row h-screen w-full bg-surface-light overflow-hidden">
    <div class="flex flex-col h-full w-full overflow-hidden">
      <!-- Top Header -->
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-4 justify-between">
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
        
        <!-- Flashcard Controls -->
        <div class="flex items-center gap-3">
          <!-- Number of cards input -->
          <div class="flex items-center gap-2">
            <label for="cardCount" class="text-sm text-gray-600">Cards:</label>
            <input 
              id="cardCount"
              v-model.number="numberOfCards" 
              type="number" 
              min="1" 
              max="20" 
              class="w-16 px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:border-accent"
            />
          </div>
          
          <!-- Generate button -->
          <button
            @click="generateFlashcards"
            :disabled="isGenerating"
            class="px-4 py-1.5 text-sm font-medium text-white bg-accent hover:bg-accent-dark rounded transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <span v-if="isGenerating" class="animate-spin w-3 h-3 border border-white border-t-transparent rounded-full"></span>
            <span>{{ isGenerating ? 'Generating...' : 'Generate Flashcards' }}</span>
          </button>
          
          <!-- Success message -->
          <transition name="fade">
            <span v-if="showSuccess" class="text-sm text-green-600 font-medium animate-fade-in">
              ✓ Flashcards saved!
            </span>
          </transition>
          
          <!-- Toggle flashcards panel button -->
          <button
            @click="toggleFlashcardsPanel"
            class="px-4 py-1.5 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded transition-colors flex items-center gap-2"
          >
            <span>{{ showFlashcards ? 'Hide' : 'Show' }} Flashcards</span>
          </button>
        </div>
      </div>
      
        <!-- Document Content Area -->
      <div class="flex flex-row w-full flex-1 relative overflow-auto">
        <!-- Error Overlay (Full Screen) -->
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
        
        <!-- Editor Container -->
        <div 
          class="flex flex-col flex-1 h-full w-full overflow-auto transition-all duration-300 ease-out"
          :class="{ 'mr-96': showFlashcards }"
        >
          <TiptapEditor
            v-if="!hasError"
            :model-value="htmlContent"
            @content-change="handleContentChange"
            @ready="handleEditorReady"
            placeholder="Start writing your document..."
          />
        </div>
        <!-- Flashcards Panel -->
        <transition name="slide">
          <div v-if="showFlashcards && !hasError" class=" absolute right-0 top-0 h-full w-96 bg-white border-l-2 border-accent overflow-y-auto">
              <div class="sticky top-0 bg-white border-b border-border-light-subtle p-4 flex justify-between items-center">
                <h3 class="font-semibold text-content-primary">Page Flashcards</h3>
                <button @click="toggleFlashcardsPanel" class="text-content-secondary hover:text-content-primary transition-colors">✕</button>
              </div>
              <div v-if="isLoadingFlashcards" class="p-6 text-center text-content-secondary">Loading flashcards...</div>
              <div v-else-if="pageFlashcards.length === 0" class="p-6 text-center text-content-secondary">
                <p class="mb-2">No flashcards yet</p>
                <p class="text-sm">Generate some flashcards from your document!</p>
              </div>
              <div v-else class="p-4 space-y-3">
                <div v-for="card in pageFlashcards" :key="card.flashcardId" class="bg-surface-light border border-border-light-subtle rounded-lg p-4 hover:shadow-md transition-shadow">
                  <div class="flex justify-end mb-2">
                    <button @click="deleteFlashcard(card.flashcardId)" class="text-xs text-content-secondary hover:text-red-600 transition-colors" title="Delete flashcard">
                      <TrashIcon :size="16" />
                    </button>
                  </div>
                  <div class="mb-3">
                    <p class="text-xs text-content-secondary font-medium mb-1">Question:</p>
                    <p class="text-sm text-content-primary">{{ card.question }}</p>
                  </div>
                  <div>
                    <p class="text-xs text-content-secondary font-medium mb-1">Answer:</p>
                    <p class="text-sm text-content-primary">{{ card.answer }}</p>
                  </div>
                  <div v-if="card.tags && card.tags.length > 0" class="mt-3 flex flex-wrap gap-1">
                    <span v-for="tag in card.tags" :key="tag" class="text-xs px-2 py-1 bg-accent/10 text-accent rounded">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
        </transition>
      </div>
    </div>
    
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in { animation: fadeIn 0.3s ease-out; }
.slide-enter-active, .slide-leave-active { transition: transform 0.3s ease-out; }
.slide-enter-from { transform: translateX(100%); }
.slide-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
