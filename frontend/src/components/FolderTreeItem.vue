<script>
import { ref } from 'vue'

export default {
  name: 'FolderTreeItem',
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
      <svg class="w-4 h-4 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
        <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
      </svg>
      
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
        <svg class="w-4 h-4 text-blue-500" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd" />
        </svg>
        
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
