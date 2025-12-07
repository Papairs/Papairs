<script>
import { ref, onMounted} from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import SearchBar from '@/components/SearchBar.vue'
import DocumentCard from '@/components/DocumentCard.vue'
import RenameDocumentModal from '@/components/RenameDocumentModal.vue'
import { driveService } from '@/utils/driveService'

export default {
  name: 'SharedView',
  components: {
    SearchBar,
    DocumentCard,
    RenameDocumentModal
  },
  setup() {
    const router = useRouter()
    const { isDark, toggleTheme, initTheme } = useTheme()

    const loading = ref(false)
    const sharedDocuments = ref([])
    const showRenameDocModal = ref(false)
    const documentToRename = ref(null)
    const searchQuery = ref('')

    const loadSharedDocuments = async () => {
      loading.value = true
      try {
        sharedDocuments.value = await driveService.getSharedDocuments()
        console.log('Shared documents:', sharedDocuments.value)
      } catch (error) {
        console.error('Error loading shared documents:', error)
        // Don't show alert for authentication errors (user will be redirected)
        if (error.status !== 401) {
          alert('Failed to load shared documents. Please try again.')
        }
      } finally {
        loading.value = false
      }
    }

    const openDocument = (pageId) => {
      router.push(`/docs/${pageId}`)
    }

    const deleteDocument = async (pageId) => {
      if (!confirm('Are you sure you want to remove this shared document?')) {
        return
      }
      try {
        await driveService.deleteDocument(pageId)
        await loadSharedDocuments()
      } catch (error) {
        console.error('Error deleting document:', error)
        // Don't show alert for authentication errors (user will be redirected)
        if (error.status === 401) {
          return
        } else if (error.response?.status === 403 || error.status === 403) {
          alert('You do not have permission to delete this document.')
        } else {
          alert('Failed to delete document. Please try again.')
        }
      }
    }

    const startRenameDocument = (doc) => {
      documentToRename.value = doc
      showRenameDocModal.value = true
    }

    const onDocumentRenamed = () => {
      showRenameDocModal.value = false
      documentToRename.value = null
      loadSharedDocuments()
    }

    const handleSearch = (query) => {
      console.log('Searching for:', query)
      // Implement search functionality later
    }

    onMounted(() => {
      initTheme()
      loadSharedDocuments()
    })

    return {
      isDark,
      toggleTheme,
      loading,
      sharedDocuments,
      showRenameDocModal,
      searchQuery,
      documentToRename,
      openDocument,
      deleteDocument,
      startRenameDocument,
      onDocumentRenamed,
      handleSearch
    }
  }
}
</script>

<template>
  <div class="flex flex-col h-full bg-surface-light dark:bg-surface-dark transition-colors overflow-hidden">
    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto">
      <!-- Top Bar -->
      <div class="bg-white dark:bg-surface-dark-secondary border-border-light dark:border-border-dark">
        <div class="px-8 py-4">
          <div class="flex items-center justify-between">
            <!-- Search -->
            <div class="flex-1 max-w-2xl">
              <SearchBar 
                v-model="searchQuery"
                @search="handleSearch"
              />
            </div>
          </div>
        </div>

        <!-- Section Header -->
        <div class="px-8 py-3 border-t-2 border-accent">
          <div class="flex items-center justify-between">
            <h2 class="text-2xl font-semibold text-content-primary dark:text-content-inverse">
              Shared with me
            </h2>
          </div>
        </div>
      </div>

      <!-- Content Area -->
      <div class="pt-2 px-8 pb-8">
        <!-- Section Labels -->
        <div class="mb-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-accent">Papairs</h3>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="flex justify-center items-center py-20">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
        </div>

        <!-- Empty State -->
        <div v-else-if="sharedDocuments.length === 0" class="text-center py-20">
          <div class="text-6xl mb-4">Nothing to see here</div>
          <h3 class="text-xl font-medium text-content-primary dark:text-content-inverse mb-2">
            No shared documents
          </h3>
          <p class="text-content-secondary mb-4">
            Documents that others share with you will appear here
          </p>
        </div>

        <!-- Documents Grid -->
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          <DocumentCard 
            v-for="doc in sharedDocuments" 
            :key="doc.pageId"
            :document="doc"
            @click="openDocument(doc.pageId)"
            @delete="deleteDocument(doc.pageId)"
            @rename="startRenameDocument(doc)"
          />
        </div>
      </div>
    </main>

    <!-- Rename Document Modal -->
    <RenameDocumentModal 
      v-if="showRenameDocModal"
      :document="documentToRename"
      @close="showRenameDocModal = false"
      @renamed="onDocumentRenamed"
    />
  </div>
</template>
