
<script>
import { ref, onMounted} from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import SearchBar from '@/components/SearchBar.vue'
import auth from '@/utils/auth'
import axios from 'axios'
import FolderCard from '@/components/FolderCard.vue'
import DocumentCard from '@/components/DocumentCard.vue'
import CreateFolderModal from '@/components/CreateFolderModal.vue'
import CreateDocumentModal from '@/components/CreateDocumentModal.vue'
import RenameFolderModal from '@/components/RenameFolderModal.vue'
import RenameDocumentModal from '@/components/RenameDocumentModal.vue'
import ShareDocumentModal from '@/components/ShareDocumentModal.vue'
import NewDropdown from '@/components/NewDropdown.vue'
import { driveService } from '@/utils/driveService'

export default {
  name: 'DriveView',
  components: {
    SearchBar,
    FolderCard,
    DocumentCard,
    CreateFolderModal,
    CreateDocumentModal,
    RenameFolderModal,
    RenameDocumentModal,
    ShareDocumentModal,
    NewDropdown
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const { isDark, toggleTheme, initTheme } = useTheme()

    const loading = ref(false)
    const folders = ref([])
    const documents = ref([])
    const currentFolderId = ref(null)
    const currentFolder = ref(null)
    const breadcrumbs = ref([])

    const showCreateFolderModal = ref(false)
    const showCreateDocModal = ref(false)
    const showRenameFolderModal = ref(false)
    const showRenameDocModal = ref(false)
    const showShareDocModal = ref(false)
    const folderToRename = ref(null)
    const documentToRename = ref(null)
    const documentToShare = ref(null)
    const showNotebookDropdown = ref(false)
    const searchQuery = ref('')

    const loadContent = async (folderId = null) => {
      loading.value = true
      try {
        currentFolderId.value = folderId

        // Load folders
        if (folderId) {
          folders.value = await driveService.getChildFolders(folderId)
          currentFolder.value = await driveService.getFolder(folderId)
          breadcrumbs.value = await driveService.getFolderPath(folderId)
        } else {
          folders.value = await driveService.getRootFolders()
          currentFolder.value = null
          breadcrumbs.value = []
        }

        // Load documents - filter by current folder
        const allDocuments = await driveService.getAllDocuments()
        documents.value = allDocuments.filter(doc => doc.folderId === folderId)
      } catch (error) {
        console.error('Error loading content:', error)
        // Don't show alert for authentication errors (user will be redirected)
        if (error.status !== 401) {
          alert('Failed to load content. Please try again.')
        }
      } finally {
        loading.value = false
      }
    }

    const navigateToRoot = () => {
      router.push('/drive')
      loadContent(null)
    }

    const navigateToFolder = (folderId) => {
      router.push(`/drive/${folderId}`)
      loadContent(folderId)
    }

    const openDocument = (pageId) => {
      router.push(`/docs/${pageId}`)
    }

    const deleteFolder = async (folderId) => {
      if (!confirm('Are you sure you want to delete this folder? This will delete all contents inside.')) {
        return
      }
      try {
        await driveService.deleteFolder(folderId, true)
        await loadContent(currentFolderId.value)
      } catch (error) {
        console.error('Error deleting folder:', error)
        // Don't show alert for authentication errors (user will be redirected)
        if (error.status !== 401) {
          alert('Failed to delete folder. Please try again.')
        }
      }
    }

    const deleteDocument = async (pageId) => {
      if (!confirm('Are you sure you want to delete this document?')) {
        return
      }
      try {
        await driveService.deleteDocument(pageId)
        await loadContent(currentFolderId.value)
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

    const startRenameFolder = (folder) => {
      folderToRename.value = folder
      showRenameFolderModal.value = true
    }

    const startRenameDocument = (doc) => {
      documentToRename.value = doc
      showRenameDocModal.value = true
    }

    const startShareDocument = (doc) => {
      documentToShare.value = doc
      showShareDocModal.value = true
    }

    const onFolderCreated = () => {
      showCreateFolderModal.value = false
      loadContent(currentFolderId.value)
    }

    const onDocumentCreated = (document) => {
      showCreateDocModal.value = false
      // Navigate to the new document
      router.push(`/docs/${document.pageId}`)
    }

    const onFolderRenamed = () => {
      showRenameFolderModal.value = false
      folderToRename.value = null
      loadContent(currentFolderId.value)
    }

    const onDocumentRenamed = () => {
      showRenameDocModal.value = false
      documentToRename.value = null
      loadContent(currentFolderId.value)
    }

    const onDocumentShared = () => {
      // Modal handles sharing internally, just keep it open
      // User can close it manually when done
      console.log('Member added successfully')
    }
    
    const handleNavigate = (destination) => {
      switch (destination) {
        case 'home':
        case 'notebook':
          navigateToRoot()
          break
        case 'search':
          // Focus search bar - implement later
          break
        case 'settings':
        case 'trash':
        case 'shared':
        case 'favorites':
        case 'invite':
          // Navigate to these pages when implemented
          console.log(`Navigate to ${destination}`)
          break
      }
    }

    const handleSearch = async (query) => {
      if (!query.trim()) {
        // Reload all content if search is cleared
        await loadContent(currentFolderId.value)
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        // Get all pages the user has access to
        const response = await axios.get(
          'http://localhost:8082/api/docs/pages',
          { headers }
        )
        
        const pages = response.data || []
        const searchTerm = query.toLowerCase().trim()
        
        // Filter documents by title match
        documents.value = pages
          .filter(page => page.title.toLowerCase().includes(searchTerm))
        
        // Clear folders during search to show only matching documents
        folders.value = []
      } catch (error) {
        console.error('Search failed:', error)
      }
    }

    onMounted(() => {
      initTheme()
      const folderId = route.params.folderId || null
      loadContent(folderId)
      
      // Listen for sidebar new document button
      window.addEventListener('open-create-document-modal', () => {
        showCreateDocModal.value = true
      })
      
      // Listen for sidebar new folder button
      window.addEventListener('open-create-folder-modal', () => {
        showCreateFolderModal.value = true
      })
    })

    return {
      isDark,
      toggleTheme,
      loading,
      folders,
      documents,
      currentFolderId,
      currentFolder,
      breadcrumbs,
      showCreateFolderModal,
      showCreateDocModal,
      showRenameFolderModal,
      showRenameDocModal,
      showShareDocModal,
      showNotebookDropdown,
      searchQuery,
      folderToRename,
      documentToRename,
      documentToShare,
      navigateToRoot,
      navigateToFolder,
      openDocument,
      deleteFolder,
      deleteDocument,
      startRenameFolder,
      startRenameDocument,
      startShareDocument,
      onFolderCreated,
      onDocumentCreated,
      onFolderRenamed,
      onDocumentRenamed,
      onDocumentShared,
      handleNavigate,
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

        <!-- Section Header with Dropdown -->
        <div class="px-8 py-3 border-t-2 border-accent">
          <div class="flex items-center justify-between">
            <div class="relative">
              <button 
                @click="showNotebookDropdown = !showNotebookDropdown"
                class="flex items-center space-x-2 text-2xl font-semibold text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark py-1 rounded"
              >
                <span>{{ currentFolder ? currentFolder.name : 'My Notebooks' }}</span>
                <svg class="w-5 h-5 transition-transform" :class="{ 'rotate-180': showNotebookDropdown }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              <!-- Dropdown Menu -->
              <NewDropdown 
                :is-open="showNotebookDropdown"
                @new-papair="showCreateDocModal = true"
                @new-folder="showCreateFolderModal = true"
                @close="showNotebookDropdown = false"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Content Area -->
      <div class="pt-2 px-8 pb-8">
        <!-- Section Labels -->
        <div class="mb-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-accent">Notebooks</h3>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="flex justify-center items-center py-20">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
        </div>

        <!-- Empty State -->
        <div v-else-if="folders.length === 0 && documents.length === 0" class="text-center py-20">
          <div class="text-6xl mb-4">Nothing to see here</div>
          <h3 class="text-xl font-medium text-content-primary dark:text-content-inverse mb-2">
            No items yet
          </h3>
          <p class="text-content-secondary mb-4">
            Create a new folder or document to get started
          </p>
          <button 
            @click="showCreateDocModal = true"
            class="px-6 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg transition-colors"
          >
            Create First Document
          </button>
        </div>

        <!-- Content Grid -->
        <div v-else>
          <!-- Folders Grid -->
          <div v-if="folders.length > 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 mb-8">
            <FolderCard 
              v-for="folder in folders" 
              :key="folder.folderId"
              :folder="folder"
              @click="navigateToFolder(folder.folderId)"
              @delete="deleteFolder(folder.folderId)"
              @rename="startRenameFolder(folder)"
            />
          </div>

          <!-- Documents Section -->
          <div v-if="documents.length > 0">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-sm font-medium text-accent">Papairs</h3>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
              <DocumentCard 
                v-for="doc in documents" 
                :key="doc.pageId"
                :document="doc"
                @click="openDocument(doc.pageId)"
                @delete="deleteDocument(doc.pageId)"
                @rename="startRenameDocument(doc)"
                @share="startShareDocument(doc)"
              />
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Create Folder Modal -->
    <CreateFolderModal 
      v-if="showCreateFolderModal"
      :parent-folder-id="currentFolderId"
      @close="showCreateFolderModal = false"
      @created="onFolderCreated"
    />

    <!-- Create Document Modal -->
    <CreateDocumentModal 
      v-if="showCreateDocModal"
      :folder-id="currentFolderId"
      @close="showCreateDocModal = false"
      @created="onDocumentCreated"
    />

    <!-- Rename Folder Modal -->
    <RenameFolderModal 
      v-if="showRenameFolderModal"
      :folder="folderToRename"
      @close="showRenameFolderModal = false"
      @renamed="onFolderRenamed"
    />

    <!-- Rename Document Modal -->
    <RenameDocumentModal 
      v-if="showRenameDocModal"
      :document="documentToRename"
      @close="showRenameDocModal = false"
      @renamed="onDocumentRenamed"
    />

    <!-- Share Document Modal -->
    <ShareDocumentModal 
      v-if="showShareDocModal"
      :document="documentToShare"
      @close="showShareDocModal = false"
      @shared="onDocumentShared"
    />
  </div>
</template>