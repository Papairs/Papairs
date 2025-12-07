<script>
export default {
  name: 'NewDropdown',
  props: {
    isOpen: {
      type: Boolean,
      default: false
    }
  },
  emits: ['new-papair', 'new-folder', 'close'],
  setup(props, { emit }) {
    const handleNewPapair = () => {
      emit('new-papair')
      emit('close')
    }

    const handleNewFolder = () => {
      emit('new-folder')
      emit('close')
    }

    return {
      handleNewPapair,
      handleNewFolder
    }
  }
}
</script>

<template>
  <div 
    v-if="isOpen"
    class="absolute left-0 mt-2 w-56 bg-surface-light dark:bg-surface-dark border border-border-light dark:border-border-dark rounded-lg shadow-lg overflow-hidden z-20"
  >
    <button 
      @click="handleNewPapair"
      class="w-full flex items-center space-x-3 px-4 py-3 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary text-content-primary dark:text-content-inverse transition-colors text-left"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <span class="text-sm font-medium">New Papair</span>
    </button>
    <button 
      @click="handleNewFolder"
      class="w-full flex items-center space-x-3 px-4 py-3 hover:bg-surface-light-secondary dark:hover:bg-surface-dark-secondary text-content-primary dark:text-content-inverse transition-colors text-left"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
      </svg>
      <span class="text-sm font-medium">New Folder</span>
    </button>
  </div>
</template>
