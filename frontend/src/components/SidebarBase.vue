<script>
import { ref, computed, onMounted, watch} from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import User3Icon from '@/components/icons/User3Icon.vue'
import SearchIcon from '@/components/icons/SearchIcon.vue'
import HomeIcon from '@/components/icons/HomeIcon.vue'
import AssistedIcon from '@/components/icons/AssistedIcon.vue'
import PlusIcon from '@/components/icons/PlusIcon.vue'
import FolderTreeItem from '@/components/FolderTreeItem.vue'
import { driveService } from '@/utils/driveService'
import auth from '@/utils/auth'

export default {
  name: 'SidebarBase',
  components: {
    User3Icon,
    SearchIcon,
    HomeIcon,
    AssistedIcon,
    PlusIcon,
    FolderTreeItem
  },
  setup() {
    const route = useRoute()
    const { isDark, toggleTheme } = useTheme()
    const loading = ref(false)
    const folderTree = ref([])
    const isDropdownOpen = ref(false)
    const assistedWritingEnabled = ref(false)

    const isDocsView = computed(() => route.path.startsWith('/docs'))
    const isDriveView = computed(() => route.path.startsWith('/drive') || route.path === '/')

    const userDisplayName = computed(() => {
      const user = auth.getUser()
      // Check if username exists
      if (user?.username) {
        return user.username
      }
      // Otherwise use first 4 characters of userId
      const userId = auth.getUserId()
      return userId ? userId.substring(0, 4) : 'User'
    })

    const toggleDropdown = () => {
      isDropdownOpen.value = !isDropdownOpen.value
    }

    const toggleAssistedWriting = () => {
      assistedWritingEnabled.value = !assistedWritingEnabled.value
      console.log('Assisted Writing:', assistedWritingEnabled.value ? 'Enabled' : 'Disabled')
    }

    const handleLogout = () => {
      auth.logout()
      window.location.href = '/login'
    }

    const openNewDocumentModal = () => {
      // Emit event to parent to open modal
      window.dispatchEvent(new CustomEvent('open-create-document-modal'))
    }

    const loadFolderTree = async () => {
      loading.value = true
      try {
        folderTree.value = await driveService.getUserFolderTree()
      } catch (error) {
        console.error('Error loading folder tree:', error)
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      if (isDocsView.value) {
        loadFolderTree()
      }
    })

    // Watch for route changes to docs view
    watch(() => route.path, (newPath) => {
      if (newPath.startsWith('/docs')) {
        loadFolderTree()
      }
    })

    return {
        loading,
        folderTree,
        userDisplayName,
        isDropdownOpen,
        assistedWritingEnabled,
        isDocsView,
        isDriveView,
        isDark,
        toggleDropdown,
        toggleAssistedWriting,
        toggleTheme,
        handleLogout,
        openNewDocumentModal
    }
  }
}


</script>

<template>
    <div class="flex flex-col w-[280px] bg-surface-light dark:bg-surface-dark border-r border-border-light dark:border-border-dark h-screen">
        <div class="px-4 py-3 flex flex-row items-center relative border-b border-border-light dark:border-border-dark">
            <User3Icon :size="32" class="w-8 h-8 text-accent flex-shrink-0" />
            <div class="flex flex-col flex-1 ml-2">
                <div class="flex flex-row justify-between w-full items-center">
                    <h1 class="text-sm font-medium text-surface-dark dark:text-surface-light">{{ userDisplayName }}</h1>
                    <button @click="toggleDropdown" class="focus:outline-none p-1">
                        <img 
                            src="@/assets/icons/chevron-down.svg" 
                            alt="Menu" 
                            class="w-5 h-5 transition-transform"
                            :class="{ 'rotate-180': isDropdownOpen }"
                        />
                    </button>
                </div>
            </div>
            
            <!-- Dropdown Menu -->
            <div 
                v-if="isDropdownOpen" 
                class="absolute top-14 right-4 bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark rounded-lg shadow-lg z-10 min-w-[120px]"
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
            <div class="mt-4 flex flex-col gap-1 px-3">
            <!-- New Button - Only on Drive View -->
            <button 
                v-if="isDriveView"
                @click="openNewDocumentModal"
                class="flex items-center justify-center gap-2 px-4 py-3 mb-4 bg-surface-light dark:bg-surface-dark border-2 border-surface-light-secondary dark:border-surface-dark-secondary rounded-lg hover:bg-gray-50 transition-colors"
            >
                <span class="text-lg font-semibold text-surface-dark dark:text-surface-light">New</span>
                <span class="text-2xl text-accent font-bold">+</span>
            </button>
            
            <!-- Search -->
            <div class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                <SearchIcon :size="24" class="w-6 h-6 text-gray-600" />
                <span class="text-base text-surface-dark dark:text-surface-light">Search</span>
            </div>
            
            <!-- Home -->
            <a href="/drive" class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded">
                <HomeIcon :size="24" class="w-6 h-6 text-gray-600" />
                <span class="text-base text-surface-dark dark:text-surface-light">Home</span>
            </a>
            
            <!-- Drive View Specific Items -->
            <div v-if="isDriveView">
                <!-- My papairs Section -->
                <div class="mt-4 mb-2">
                    <h2 class="text-xs font-semibold text-surface-dark-secondary dark:text-surface-light-secondary px-2">My papairs</h2>
                </div>
                
                <!-- My notebook -->
                <a href="/drive" class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded">
                    <svg class="w-5 h-5 text-accent" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
                    </svg>
                    <span class="text-base text-accent font-medium">My notebook</span>
                </a>
                
                <!-- Shared with me -->
                <a href="/drive/shared" class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                    <svg class="w-5 h-5 text-surface-dark dark:text-surface-light" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                    </svg>
                    <span class="text-base text-surface-dark dark:text-surface-light">Shared with me</span>
                </a>
                
                <!-- Favorites -->
                <button class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                    <svg class="w-5 h-5 text-surface-dark dark:text-surface-light" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                    </svg>
                    <span class="text-base text-surface-dark dark:text-surface-light">Favorites</span>
                </button>
            </div>
            
            <!-- Papairs AI Settings Section -->
            <div class="mt-4 mb-2">
                <h2 class="text-xs font-semibold text-surface-dark-secondary dark:text-surface-light-secondary px-2">Papairs AI Settings</h2>
            </div>
            
            <!-- Assisted Writing -->
            <div class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer" @click="toggleAssistedWriting">
                <AssistedIcon :size="24" class="w-6 h-6 text-accent" />
                <span class="text-base text-surface-dark dark:text-surface-light flex-1">Assisted Writing</span>
                <div 
                    class="w-10 h-5 rounded-full relative transition-colors duration-200 cursor-pointer"
                    :class="assistedWritingEnabled ? 'bg-accent' : 'bg-gray-300'"
                >
                    <div 
                        class="absolute top-0.5 w-4 h-4 bg-white rounded-full transition-all duration-200"
                        :class="assistedWritingEnabled ? 'right-0.5' : 'left-0.5'"
                    ></div>
                </div>
            </div>
            
            <!-- Papairs Section - Only show on docs view -->
            <div v-if="isDocsView" class="mt-6 mb-2">
                <div class="flex justify-between items-center px-2">
                    <h2 class="text-xs font-semibold text-surface-dark-secondary dark:text-surface-light-secondary">Papairs</h2>
                    <button class="text-accent hover:text-orange-600">
                        <PlusIcon :size="20" class="w-5 h-5" />
                    </button>
                </div>
            </div>
            
            <!-- Folder Tree - Only show on docs view -->
            <div v-if="isDocsView" class="folder-tree-container">
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
        <div class="border-t border-border-light dark:border-border-dark px-3 py-3">
            <div class="flex flex-col gap-1">
                <!-- Settings -->
                <button class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                    <svg class="w-5 h-5 text-surface-dark-secondary dark:text-surface-light-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    <span class="text-base text-surface-dark dark:text-surface-light">Settings</span>
                </button>
                
                <!-- Dark Mode -->
                <button @click="toggleTheme" class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                    <svg class="w-5 h-5 text-surface-dark-secondary dark:text-surface-light-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                    </svg>
                    <span class="text-base text-surface-dark dark:text-surface-light">Dark Mode</span>
                </button>
                
                <!-- Trash -->
                <button class="flex gap-3 items-center px-2 py-2 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary rounded cursor-pointer">
                    <svg class="w-5 h-5 text-surface-dark-secondary dark:text-surface-light-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    <span class="text-base text-surface-dark dark:text-surface-light">Trash</span>
                </button>
            </div>
            
            <!-- Version -->
            <div class="mt-3 px-2">
                <p class="text-xs text-accent font-medium">Papairs v1.0</p>
            </div>
        </div>
    </div>

</template>
