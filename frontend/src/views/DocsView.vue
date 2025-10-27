<template>
  <div class="flex flex-row">
    <div class="flex">

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