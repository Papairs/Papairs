<template>
  <div>
    <h1 class="text-3xl font-bold text-content-primary dark:text-content-inverse mb-6 transition-colors">
      Documentation
    </h1>
    
    <div class="bg-surface-light dark:bg-surface-dark-secondary rounded-lg shadow-md p-6 transition-colors">
      <h2 class="text-2xl font-semibold text-content-primary dark:text-content-inverse mb-4 transition-colors">
        Available Documents
      </h2>
      
      <div v-if="loading" class="text-center py-4">
        <p class="text-content-secondary transition-colors">Loading documents...</p>
      </div>
      
      <div v-else-if="documents.length > 0" class="space-y-4">
        <div 
          v-for="doc in documents" 
          :key="doc.id" 
          class="border border-border-light-subtle dark:border-border-dark-subtle rounded-lg p-4 bg-surface-light-secondary dark:bg-surface-dark transition-colors"
        >
          <h3 class="text-xl font-semibold text-content-primary dark:text-content-inverse transition-colors">
            {{ doc.title }}
          </h3>
          <p class="text-content-secondary mt-2 transition-colors">
            {{ doc.description }}
          </p>
          <p class="text-sm text-content-secondary mt-2 transition-colors">
            Created: {{ doc.createdAt }}
          </p>
        </div>
      </div>
      
      <div v-else class="text-center py-8">
        <p class="text-content-secondary transition-colors">No documents available</p>
      </div>
      
      <button 
        @click="loadDocuments" 
        class="mt-4 bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded transition-colors"
      >
        Refresh Documents
      </button>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'DocsView',
  data() {
    return {
      documents: [],
      loading: false
    }
  },
  async mounted() {
    await this.loadDocuments();
  },
  methods: {
    async loadDocuments() {
      this.loading = true;
      try {
        const response = await axios.get('http://localhost:8082/api/docs/all');
        this.documents = response.data;
      } catch (error) {
        console.error('Error loading documents:', error);
        this.documents = [];
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>