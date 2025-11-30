<template>
  <div 
    class="group relative bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg hover:shadow-md transition-all cursor-pointer overflow-visible"
    style="height: 50px; width: 100%"
    @click="$emit('click')"
  >
    <!-- Header with Icon, Title and Menu on same line -->
    <div class="flex items-center justify-between px-4 h-full">
      <!-- Icon and Title -->
      <div class="flex items-center space-x-3 flex-1 min-w-0">
        <!-- Folder Icon -->
        <div class="flex-shrink-0">
          <svg class="w-5 h-5 text-content-primary dark:text-content-inverse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
        </div>
        
        <!-- Folder Name -->
        <h3 class="text-sm font-medium text-content-primary dark:text-content-inverse truncate">
          {{ folder.name }}
        </h3>
      </div>
      
      <!-- Menu Button Container -->
      <div class="relative" ref="menuRef">
        <button 
          @click.stop="showMenu = !showMenu"
          class="flex-shrink-0 p-1 rounded hover:bg-surface-light-secondary dark:hover:bg-surface-dark transition-colors"
        >
          <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        
        <!-- Dropdown Menu -->
        <div 
          v-if="showMenu"
          @click.stop
          class="absolute right-0 top-full mt-1 w-40 bg-white dark:bg-surface-dark-secondary border border-border-light dark:border-border-dark rounded-lg shadow-lg z-50"
        >
          <button 
            @click.stop="handleRename"
            class="w-full text-left px-4 py-2 text-sm text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark first:rounded-t-lg"
          >
            Rename
          </button>
          <button 
            @click.stop="handleDelete"
            class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900 last:rounded-b-lg"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'

export default {
  name: 'FolderCard',
  props: {
    folder: {
      type: Object,
      required: true
    }
  },
  emits: ['click', 'delete', 'rename'],
  setup(props, { emit }) {
    const showMenu = ref(false)
    const menuRef = ref(null)

    const formatDate = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    }

    const handleClickOutside = (event) => {
      if (menuRef.value && !menuRef.value.contains(event.target)) {
        showMenu.value = false
      }
    }

    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
    })

    onBeforeUnmount(() => {
      document.removeEventListener('click', handleClickOutside)
    })

    const handleDelete = () => {
      showMenu.value = false
      emit('delete')
    }

    const handleRename = () => {
      showMenu.value = false
      emit('rename')
    }

    return {
      showMenu,
      menuRef,
      formatDate,
      handleDelete,
      handleRename
    }
  }
}
</script>
