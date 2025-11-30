<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="$emit('close')">
    <div class="bg-white dark:bg-surface-dark-secondary rounded-lg shadow-xl p-6 w-full max-w-md">
      <h2 class="text-xl font-semibold text-content-primary dark:text-content-inverse mb-4">
        Rename Document
      </h2>
      
      <form @submit.prevent="handleSubmit">
        <div class="mb-4">
          <label class="block text-sm font-medium text-content-primary dark:text-content-inverse mb-2">
            Document Title
          </label>
          <input 
            v-model="documentTitle"
            type="text"
            placeholder="Enter document title..."
            class="w-full px-4 py-2 border border-border-light dark:border-border-dark rounded-lg focus:outline-none focus:border-accent bg-white dark:bg-surface-dark text-content-primary dark:text-content-inverse"
            :disabled="loading"
            autofocus
          />
        </div>

        <div v-if="error" class="mb-4 p-3 bg-red-100 dark:bg-red-900 border border-red-300 dark:border-red-700 rounded-lg">
          <p class="text-sm text-red-700 dark:text-red-300">{{ error }}</p>
        </div>

        <div class="flex justify-end space-x-3">
          <button 
            type="button"
            @click="$emit('close')"
            class="px-4 py-2 border border-border-light dark:border-border-dark rounded-lg text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
            :disabled="loading"
          >
            Cancel
          </button>
          <button 
            type="submit"
            class="px-4 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg disabled:bg-gray-400 disabled:cursor-not-allowed"
            :disabled="!documentTitle.trim() || loading"
          >
            {{ loading ? 'Renaming...' : 'Rename' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { driveService } from '@/utils/driveService'

export default {
  name: 'RenameDocumentModal',
  props: {
    document: {
      type: Object,
      required: true
    }
  },
  emits: ['close', 'renamed'],
  setup(props, { emit }) {
    const documentTitle = ref('')
    const loading = ref(false)
    const error = ref(null)

    onMounted(() => {
      documentTitle.value = props.document.title
    })

    const handleSubmit = async () => {
      error.value = null
      loading.value = true

      try {
        await driveService.renameDocument(props.document.pageId, documentTitle.value.trim())
        emit('renamed')
      } catch (err) {
        console.error('Error renaming document:', err)
        if (err.response?.status === 403) {
          error.value = 'You do not have permission to rename this document.'
        } else {
          error.value = err.response?.data?.message || 'Failed to rename document. Please try again.'
        }
      } finally {
        loading.value = false
      }
    }

    return {
      documentTitle,
      loading,
      error,
      handleSubmit
    }
  }
}
</script>
