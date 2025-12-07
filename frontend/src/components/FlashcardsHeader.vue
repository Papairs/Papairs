<script>
export default {
  name: 'FlashcardsHeader',
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    flashcardsCount: {
      type: Number,
      default: 0
    },
    selectedPageId: {
      type: String,
      default: 'all'
    },
    pages: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:selectedPageId', 'start-study', 'reset-all'],
  methods: {
    handlePageChange(event) {
      this.$emit('update:selectedPageId', event.target.value)
    },
    handleStartStudy() {
      this.$emit('start-study')
    },
    handleResetAll() {
      this.$emit('reset-all')
    },
    getPageTitle(pageId) {
      return this.pages.find(p => p.pageId === pageId)?.title || pageId
    }
  }
}
</script>

<template>
  <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center px-6 justify-between">
    <h1 class="text-2xl font-bold text-content-primary dark:text-content-inverse">My Flashcards</h1>
    
    <!-- Filter by Document and Study Button -->
    <div v-if="!loading && flashcardsCount > 0" class="flex items-center gap-3">
      <label for="page-filter" class="text-sm text-content-secondary font-medium">
        Filter by Document:
      </label>
      <select
        id="page-filter"
        :value="selectedPageId"
        @change="handlePageChange"
        class="px-4 py-2 border border-border-light-subtle dark:border-border-dark-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-accent bg-white dark:bg-surface-dark-secondary text-content-primary dark:text-content-inverse"
      >
        <option value="all">All Documents ({{ flashcardsCount }})</option>
        <option
          v-for="page in pages"
          :key="page.pageId"
          :value="page.pageId"
        >
          {{ page.title }} ({{ page.count }})
        </option>
      </select>
      
      <button
        @click="handleStartStudy"
        class="px-4 py-2 bg-accent text-white rounded-md text-sm font-medium hover:bg-accent-dark transition-colors flex items-center gap-2"
      >
        Study Mode
      </button>
      
      <button
        @click="handleResetAll"
        class="px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 transition-colors flex items-center gap-2"
        title="Reset all flashcards to unlearned"
      >
        Reset All
      </button>
    </div>
  </div>
</template>
