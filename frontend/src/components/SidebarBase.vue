<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import User3Icon from '@/components/icons/User3Icon.vue'
import SearchIcon from '@/components/icons/SearchIcon.vue'
import HomeIconNew from '@/components/icons/HomeIconNew.vue'
import AssistedIcon from '@/components/icons/AssistedIcon.vue'
import PlusIcon from '@/components/icons/PlusIcon.vue'
import FlashcardIcon from '@/components/icons/FlashcardIcon.vue'
import FolderIcon from '@/components/icons/FolderIcon.vue'
import DocumentIcon from '@/components/icons/DocumentIcon.vue'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import NewDropdown from '@/components/NewDropdown.vue'
import { driveService } from '@/utils/driveService'
import auth from '@/utils/auth'
import axios from 'axios'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon,
    SearchIcon,
    HomeIconNew,
    AssistedIcon,
    PlusIcon,
    FlashcardIcon,
    FolderIcon,
    DocumentIcon,
    FolderTreeItem,
    NewDropdown
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const { isDark, toggleTheme } = useTheme()

    // State
    const loading = ref(false)
    const folderTree = ref([])
    const isDropdownOpen = ref(false)
    const isSettingsDropdownOpen = ref(false)
    const isNewDropdownOpen = ref(false)
    const isFolderTreeDropdownOpen = ref(false)
    const assistedWritingEnabled = ref(false)
    const isSearchActive = ref(false)
    const searchQuery = ref('')
    const searchResults = ref([])

    // Computed
    const userDisplayName = computed(() => {
      const user = auth.getUser()
      if (user?.username) return user.username
      const userId = auth.getUserId()
      return userId ? userId.substring(0, 4) : 'User'
    })

    const isHomeActive = computed(() => route.path === '/' || route.path === '/drive')
    const isSharedActive = computed(() => route.path === '/drive/shared')
    const isFlashcardsView = computed(() => route.path.startsWith('/flashcards'))

    // Navigation
    const navigateTo = (path) => router.push(path)
    
    const navigateToPage = (pageId) => {
      router.push(`/docs/${pageId}`)
      clearSearch()
    }

    // Dropdowns
    const toggleDropdown = () => {
      isDropdownOpen.value = !isDropdownOpen.value
    }

    const toggleSettingsDropdown = () => {
      isSettingsDropdownOpen.value = !isSettingsDropdownOpen.value
    }

    const toggleNewDropdown = () => {
      isNewDropdownOpen.value = !isNewDropdownOpen.value
    }

    const toggleFolderTreeDropdown = () => {
      isFolderTreeDropdownOpen.value = !isFolderTreeDropdownOpen.value
    }

    // Settings
    const toggleAssistedWriting = () => {
      assistedWritingEnabled.value = !assistedWritingEnabled.value
      console.log('Assisted Writing:', assistedWritingEnabled.value ? 'Enabled' : 'Disabled')
    }

    const handleLogout = () => {
      auth.logout()
      window.location.href = '/login'
    }

    // Modals
    const openNewDocumentModal = () => {
      isNewDropdownOpen.value = false
      window.dispatchEvent(new CustomEvent('open-create-document-modal'))
    }

    const openNewFolderModal = () => {
      isNewDropdownOpen.value = false
      window.dispatchEvent(new CustomEvent('open-create-folder-modal'))
    }

    // Folder Tree
    const loadFolderTree = async () => {
      if (!auth.getUserId()) {
        folderTree.value = []
        return
      }
      
      loading.value = true
      try {
        folderTree.value = await driveService.getUserFolderTree()
      } catch (error) {
        folderTree.value = []
        if (error.response?.status !== 401 && error.code !== 'ERR_NETWORK') {
          console.error('Error loading folder tree:', error)
        }
      } finally {
        loading.value = false
      }
    }

    // Search
    const activateSearch = () => {
      isSearchActive.value = true
      setTimeout(() => {
        document.getElementById('sidebar-search-input')?.focus()
      }, 0)
    }

    const deactivateSearch = () => {
      if (!searchQuery.value) {
        isSearchActive.value = false
        searchResults.value = []
      }
    }

    const performSearch = async () => {
      if (!searchQuery.value.trim()) {
        searchResults.value = []
        return
      }

      try {
        const headers = await auth.getAuthHeaders(router)
        const response = await axios.get('http://localhost:8082/api/docs/pages', { headers })
        const pages = response.data || []
        const query = searchQuery.value.toLowerCase().trim()
        
        searchResults.value = pages
          .filter(page => page.title.toLowerCase().includes(query))
          .map(page => ({ pageId: page.pageId, title: page.title, content: '' }))
          .slice(0, 10)
      } catch (error) {
        console.error('Search failed:', error)
        searchResults.value = []
      }
    }

    const clearSearch = () => {
      searchQuery.value = ''
      searchResults.value = []
      isSearchActive.value = false
    }

    // Lifecycle
    onMounted(() => {
      if (auth.getUserId()) {
        loadFolderTree()
      }
    })
    
    watch(() => route.path, (newPath) => {
      if (auth.getUserId() && (newPath.startsWith('/docs') || newPath.startsWith('/drive') || newPath === '/')) {
        loadFolderTree()
      }
    })

    return {
      loading,
      folderTree,
      userDisplayName,
      isDropdownOpen,
      isSettingsDropdownOpen,
      isNewDropdownOpen,
      isFolderTreeDropdownOpen,
      assistedWritingEnabled,
      isHomeActive,
      isSharedActive,
      isFlashcardsView,
      isDark,
      isSearchActive,
      searchQuery,
      searchResults,
      toggleDropdown,
      toggleSettingsDropdown,
      toggleNewDropdown,
      toggleFolderTreeDropdown,
      toggleAssistedWriting,
      toggleTheme,
      handleLogout,
      openNewDocumentModal,
      openNewFolderModal,
      navigateTo,
      activateSearch,
      deactivateSearch,
      performSearch,
      clearSearch,
      navigateToPage
    }
  }
}


</script>

<template>
    <div class="flex flex-col w-[280px] bg-surface-light dark:bg-surface-dark border-r border-border-light dark:border-border-dark h-screen">
        <div class="h-[50px] px-4 flex flex-row items-center relative">
            <User3Icon :size="28" class="w-7 h-7 text-accent flex-shrink-0" />
            <div class="flex flex-col flex-1 ml-2">
                <div class="flex flex-row justify-between w-full items-center">
                    <h1 class="text-sm font-medium text-surface-dark dark:text-surface-light">{{ userDisplayName }}</h1>
                    <button @click="toggleDropdown" class="focus:outline-none p-0.5">
                        <img 
                            src="@/assets/icons/chevron-down.svg" 
                            alt="Menu" 
                            class="w-4 h-4 transition-transform"
                            :class="{ 'rotate-180': isDropdownOpen }"
                        />
                    </button>
                </div>
            </div>
            
            <!-- Dropdown Menu -->
            <div 
                v-if="isDropdownOpen" 
                class="absolute top-12 right-4 bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark rounded-lg shadow-lg z-10 min-w-[120px]"
            >
                <button 
                    @click="handleLogout" 
                    class="w-full text-left px-4 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary text-sm rounded-lg"
                >
                    Logout
                </button>
            </div>
        </div>
        
        <!-- Scrollable Content -->
        <div class="flex-1 overflow-y-auto">
            <div class="mt-3 flex flex-col gap-0.5 px-3">
            <!-- New Button with Dropdown -->
            <div class="relative mb-3">
                <button 
                    @click="toggleNewDropdown"
                    class="inline-flex items-center justify-center px-3 py-2.5 w-[120px] bg-surface-light dark:bg-surface-dark border-[1.5px] border-content-primary dark:border-content-inverse rounded-lg hover:bg-gray-50 dark:hover:bg-surface-dark-secondary transition-colors"
                >
                    <span class="text-sm font-semibold text-surface-dark dark:text-surface-light">New</span>
                    <span class="text-base text-accent font-bold leading-none ml-0.5 -mt-1.5">+</span>
                </button>
                
                <NewDropdown 
                    :is-open="isNewDropdownOpen"
                    @new-papair="openNewDocumentModal"
                    @new-folder="openNewFolderModal"
                    @close="isNewDropdownOpen = false"
                />
            </div>
            
            <!-- Search -->
            <div 
                v-if="!isSearchActive"
                @click="activateSearch"
                class="flex gap-3 items-center px-2 py-2 rounded cursor-pointer hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary"
            >
                <SearchIcon :size="24" class="w-6 h-6 text-content-primary dark:text-content-inverse" />
                <span class="text-base text-surface-dark dark:text-surface-light">Search</span>
            </div>
            <div 
                v-else
                class="flex gap-3 items-center px-2 py-2 rounded bg-surface-light-secondary dark:bg-surface-dark-secondary relative"
            >
                <SearchIcon :size="20" class="text-accent flex-shrink-0" />
                <input
                    id="sidebar-search-input"
                    v-model="searchQuery"
                    @input="performSearch"
                    @blur="deactivateSearch"
                    type="text"
                    placeholder="Search..."
                    class="flex-1 bg-transparent outline-none text-content-primary dark:text-content-inverse text-base"
                />
                <button 
                    v-if="searchQuery"
                    @mousedown.prevent="clearSearch"
                    class="text-gray-400 hover:text-gray-600"
                >
                    ✕
                </button>
            </div>
            
            <!-- Search Results Dropdown -->
            <div 
                v-if="isSearchActive && searchResults.length > 0" 
                class="bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark rounded-lg shadow-lg mt-1 max-h-64 overflow-y-auto mx-2"
            >
                <button
                    v-for="result in searchResults"
                    :key="result.pageId"
                    @mousedown.prevent="navigateToPage(result.pageId)"
                    class="w-full text-left px-3 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary flex items-start gap-2 border-b border-border-light-subtle dark:border-border-dark-subtle last:border-0"
                >
                    <DocumentIcon :size="14" class="text-content-primary dark:text-content-inverse mt-1 flex-shrink-0" />
                    <div class="flex-1 min-w-0">
                        <div class="text-sm font-medium text-surface-dark dark:text-surface-light truncate">{{ result.title }}</div>
                    </div>
                </button>
            </div>
            
            <!-- No Results Message -->
            <div 
                v-if="isSearchActive && searchQuery && searchResults.length === 0" 
                class="bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark rounded-lg shadow-lg mt-1 mx-2 px-3 py-2 text-sm text-gray-500 text-center"
            >
                No results found
            </div>
            
            <!-- Home -->
            <a 
                href="/drive" 
                class="flex gap-3 items-center px-2 py-2 rounded"
                :class="isHomeActive ? 'bg-surface-light-secondary dark:bg-surface-dark-secondary' : 'hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary'"
            >
                <HomeIconNew :size="24" class="w-6 h-6" :class="isHomeActive ? 'text-accent' : 'text-content-primary dark:text-content-inverse'" />
                <span class="text-base" :class="isHomeActive ? 'text-accent font-medium' : 'text-surface-dark dark:text-surface-light'">Home</span>
            </a>
            
            <!-- Flashcards -->
            <button
                @click="navigateTo('/flashcards')"
                class="flex gap-3 items-center px-2 py-2 rounded cursor-pointer"
                :class="isFlashcardsView ? 'bg-surface-light-secondary dark:bg-surface-dark-secondary' : 'hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary'"
            >
                <FlashcardIcon :size="24" :class="isFlashcardsView ? 'text-accent' : 'text-content-primary dark:text-content-inverse'" />
                <span class="text-base" :class="isFlashcardsView ? 'text-accent font-medium' : 'text-surface-dark dark:text-surface-light'">Flashcards</span>
            </button>
            
            <!-- My papairs Section -->
            <div class="mt-3 mb-1">
                <h2 class="text-xs font-semibold text-surface-dark-secondary dark:text-surface-light-secondary px-2">My papairs</h2>
            </div>
            
            <!-- My notebook -->
            <a 
                href="/drive" 
                class="flex gap-3 items-center px-2 py-2 rounded"
                :class="isHomeActive ? 'bg-surface-light-secondary dark:bg-surface-dark-secondary' : 'hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary'"
            >
                <FolderIcon :size="20" :class="isHomeActive ? 'text-accent' : 'text-content-primary dark:text-content-inverse'" />
                <span class="text-base" :class="isHomeActive ? 'text-accent font-medium' : 'text-surface-dark dark:text-surface-light'">My notebook</span>
            </a>
            
            <!-- Shared with me -->
            <a 
                href="/drive/shared" 
                class="flex gap-3 items-center px-2 py-2 rounded cursor-pointer"
                :class="isSharedActive ? 'bg-surface-light-secondary dark:bg-surface-dark-secondary' : 'hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary'"
            >
                <svg class="w-5 h-5" :class="isSharedActive ? 'text-accent' : 'text-surface-dark dark:text-surface-light'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                <span class="text-base" :class="isSharedActive ? 'text-accent font-medium' : 'text-surface-dark dark:text-surface-light'">Shared with me</span>
            </a>
            
            <!-- Papairs Section -->
            <div class="mt-3 mb-1">
                <div class="flex justify-between items-center px-2 relative">
                    <h2 class="text-xs font-semibold text-surface-dark-secondary dark:text-surface-light-secondary">Papairs</h2>
                    <button @click="toggleFolderTreeDropdown" class="text-accent hover:text-orange-600">
                        <PlusIcon :size="20" class="w-5 h-5" />
                    </button>
                    
                    <NewDropdown 
                        :is-open="isFolderTreeDropdownOpen"
                        @new-papair="openNewDocumentModal"
                        @new-folder="openNewFolderModal"
                        @close="isFolderTreeDropdownOpen = false"
                    />
                </div>
            </div>
            
            <!-- Folder Tree -->
            <div class="folder-tree-container">
                <div v-if="loading" class="text-sm text-gray-500 px-2">Loading...</div>
                <div v-else-if="folderTree.length === 0" class="text-sm text-gray-500 px-2">No folders yet</div>
                <div v-else>
                    <FolderTreeItem
                        v-for="folder in folderTree"
                        :key="folder.folderId"
                        :folder="folder"
                        :level="0"
                    />
                </div>
            </div>
        </div>
        </div>
        
        <!-- Bottom Settings Section -->
        <div class="px-3 py-1.5">
            <!-- Version -->
            <div class="mb-2 px-2">
                <p class="text-xs text-accent font-medium">Papairs v1.0</p>
            </div>
            
            <div class="flex flex-col gap-0.5">
                <!-- Settings with Dropdown -->
                <div class="relative">
                    <button 
                        @click="toggleSettingsDropdown"
                        class="w-full flex gap-3 items-center px-2 py-1.5 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer"
                    >
                        <svg class="w-5 h-5 text-surface-dark-secondary dark:text-surface-light-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                        <span class="text-sm text-surface-dark dark:text-surface-light text-left flex-1">Settings</span>
                        <img 
                            src="@/assets/icons/chevron-down.svg" 
                            alt="Expand" 
                            class="w-4 h-4 transition-transform"
                            :class="{ 'rotate-180': isSettingsDropdownOpen }"
                        />
                    </button>
                    
                    <!-- Settings Dropdown -->
                    <div 
                        v-if="isSettingsDropdownOpen"
                        class="mt-1 ml-4 pl-3 border-l-2 border-border-light dark:border-border-dark space-y-0.5"
                    >
                        <!-- Assisted Writing -->
                        <div class="flex gap-2 items-center px-2 py-1.5 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer" @click="toggleAssistedWriting">
                            <AssistedIcon :size="20" class="w-5 h-5 text-accent flex-shrink-0" />
                            <span class="text-sm text-surface-dark dark:text-surface-light flex-1">Assisted Writing</span>
                            <div 
                                class="w-8 h-4 rounded-full relative transition-colors duration-200 cursor-pointer flex-shrink-0"
                                :class="assistedWritingEnabled ? 'bg-accent' : 'bg-gray-300'"
                            >
                                <div 
                                    class="absolute top-0.5 w-3 h-3 bg-white rounded-full transition-all duration-200"
                                    :class="assistedWritingEnabled ? 'right-0.5' : 'left-0.5'"
                                ></div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Dark Mode -->
                <button @click="toggleTheme" class="flex gap-3 items-center px-2 py-1.5 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                    <svg class="w-5 h-5 text-surface-dark-secondary dark:text-surface-light-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                    </svg>
                    <span class="text-sm text-surface-dark dark:text-surface-light">Dark Mode</span>
                </button>
            </div>
        </div>
    </div>
</template>
