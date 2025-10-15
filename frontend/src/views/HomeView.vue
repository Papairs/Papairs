<template>
  <div class="text-center">
    <h1 class="text-4xl font-bold text-content-primary dark:text-content-inverse mb-4 transition-colors">
      Welcome to Papairs
    </h1>
    <p class="text-lg text-content-secondary mb-8 transition-colors">
      A simple Vue.js frontend with Spring Boot backends
    </p>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto">

      <div class="bg-surface-light dark:bg-surface-dark-secondary p-6 rounded-lg shadow-md transition-colors">
        <h2 class="text-2xl font-semibold text-content-primary dark:text-content-inverse mb-4 transition-colors">
          Authentication Service
        </h2>
        <p class="text-content-secondary mb-4 transition-colors">
          Handle user authentication and authorization
        </p>
        <button 
          @click="testAuth" 
          class="bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded transition-colors"
        >
          Test Auth Service
        </button>
        <div 
          v-if="authResult" 
          class="mt-4 p-2 bg-surface-light-secondary dark:bg-surface-dark rounded transition-colors"
        >
          <pre class="text-content-primary dark:text-content-inverse text-sm overflow-x-auto">{{ authResult }}</pre>
        </div>
      </div>
      
      <div class="bg-surface-light dark:bg-surface-dark-secondary p-6 rounded-lg shadow-md transition-colors">
        <h2 class="text-2xl font-semibold text-content-primary dark:text-content-inverse mb-4 transition-colors">
          Documentation Service
        </h2>
        <p class="text-content-secondary mb-4 transition-colors">
          Manage and serve documentation
        </p>
        <button 
          @click="testDocs" 
          class="bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded transition-colors"
        >
          Test Docs Service
        </button>
        <div 
          v-if="docsResult" 
          class="mt-4 p-2 bg-surface-light-secondary dark:bg-surface-dark rounded transition-colors"
        >
          <pre class="text-content-primary dark:text-content-inverse text-sm overflow-x-auto">{{ docsResult }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'HomeView',
  data() {
    return {
      authResult: null,
      docsResult: null
    }
  },
  methods: {
    async testAuth() {
      try {
        const response = await axios.get('http://localhost:8081/api/auth/health');
        this.authResult = JSON.stringify(response.data, null, 2);
      } catch (error) {
        this.authResult = `Error: ${error.message}`;
      }
    },
    async testDocs() {
      try {
        const response = await axios.get('http://localhost:8082/api/docs/health');
        this.docsResult = JSON.stringify(response.data, null, 2);
      } catch (error) {
        this.docsResult = `Error: ${error.message}`;
      }
    }
  }
}
</script>