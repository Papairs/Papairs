<script>
import { ref } from 'vue'
import FolderIcon from '@/components/icons/FolderIcon.vue'
import FolderOpenIcon from '@/components/icons/FolderOpenIcon.vue'
import DocumentIcon from '@/components/icons/DocumentIcon.vue'

export default {
  name: 'FolderTreeItem',
  components: {
    FolderIcon,
    FolderOpenIcon,
    DocumentIcon
  },
  props: {
    folder: {
      type: Object,
      required: true
    },
    level: {
      type: Number,
      default: 0
    }
  },
  setup() {
    const isExpanded = ref(false)

    const toggleExpand = () => {
      isExpanded.value = !isExpanded.value
    }

    const handleDocumentClick = (document) => {
      // Use window.location to force a full page refresh
      window.location.href = `/docs/${document.pageId}`
    }

    return {
      isExpanded,
      toggleExpand,
      handleDocumentClick
    }
  }
}
</script>

<template>
  <div class="folder-tree-item">
    <!-- Folder Header -->
    <div 
      class="flex items-center gap-2 py-1 px-2 hover:bg-gray-100 rounded cursor-pointer"
      :style="{ paddingLeft: `${level * 12 + 8}px` }"
      @click="toggleExpand"
    >
      <!-- Chevron Icon -->
      <svg 
        :class="['w-4 h-4 transition-transform', { 'rotate-90': isExpanded }]"
        fill="none" 
        stroke="currentColor" 
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
      
      <!-- Folder Icon -->
      <FolderOpenIcon v-if="isExpanded" :size="16" class="text-content-primary dark:text-content-inverse" />
      <FolderIcon v-else :size="16" class="text-content-primary dark:text-content-inverse" />
      
      <!-- Folder Name -->
      <span class="text-sm flex-1">{{ folder.name }}</span>
      
      <!-- Item Count -->
      <span class="text-xs text-gray-400">
        {{ folder.childCount + folder.pageCount }}
      </span>
    </div>

    <!-- Expanded Content -->
    <div v-if="isExpanded" class="folder-content">
      <!-- Child Folders (sorted first) -->
      <FolderTreeItem
        v-for="child in folder.children"
        :key="child.folderId"
        :folder="child"
        :level="level + 1"
      />
      
      <!-- Documents -->
      <div
        v-for="document in folder.documents"
        :key="document.pageId"
        class="flex items-center gap-2 py-1 px-2 hover:bg-gray-100 rounded cursor-pointer"
        :style="{ paddingLeft: `${(level + 1) * 12 + 8 + 16}px` }"
        @click="handleDocumentClick(document)"
      >
        <!-- Document Icon -->
        <DocumentIcon :size="16" class="text-content-primary dark:text-content-inverse" />
        
        <!-- Document Title -->
        <span class="text-sm">{{ document.title }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.folder-tree-item {
  user-select: none;
}
</style>
