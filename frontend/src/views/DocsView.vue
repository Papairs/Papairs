<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light">
    <div class="flex flex-col w-[250px] bg-surface-light-secondary border-r border-border-dark">
      <div class="h-[60px] w-full">
        <div></div>
      </div>
    </div>
    <div class="flex flex-col h-full w-full">
      <div class="flex flex-row h-[60px] w-full border-b-2 border-accent"></div>
      <div class="flex flex-col h-full w-full justify-center items-center">
        <div class="flex flex-row h-[50px] w-[1200px] border-b border-b-border-light-subtle"></div>
        <div class="h-full w-[1000px] bg-white border-x border-x-border-light-subtle p-12">
          <textarea name="" id="" class="w-full h-full focus:outline-none justify-start items-start"></textarea>
        </div>

      </div>
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