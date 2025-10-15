<template>
  <div>
    <h1 class="text-3xl font-bold text-dark-text mb-6">Documentation</h1>
    
    <div class="bg-white rounded-lg shadow-md p-6">
      <h2 class="text-2xl font-semibold text-dark-text mb-4">Available Documents</h2>
      
      <div v-if="loading" class="text-center py-4">
        <p class="text-light-text">Loading documents...</p>
      </div>
      
      <div v-else-if="documents.length > 0" class="space-y-4">
        <div v-for="doc in documents" :key="doc.id" class="border border-gray-200 rounded-lg p-4">
          <h3 class="text-xl font-semibold text-dark-text">{{ doc.title }}</h3>
          <p class="text-light-text mt-2">{{ doc.description }}</p>
          <p class="text-sm text-gray-400 mt-2">Created: {{ doc.createdAt }}</p>
        </div>
      </div>
      
      <div v-else class="text-center py-8">
        <p class="text-gray-500">No documents available</p>
      </div>
      
      <button @click="loadDocuments" class="mt-4 bg-blue-500 hover:bg-blue-700 text-white-text font-bold py-2 px-4 rounded">
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