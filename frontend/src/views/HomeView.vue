<template>
  <div class="text-center">
    <h1 class="text-4xl font-bold text-gray-900 mb-4">Welcome to Papairs</h1>
    <p class="text-lg text-gray-600 mb-8">A simple Vue.js frontend with Spring Boot backends</p>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto">
      <div class="bg-white p-6 rounded-lg shadow-md">
        <h2 class="text-2xl font-semibold text-gray-800 mb-4">Authentication Service</h2>
        <p class="text-gray-600 mb-4">Handle user authentication and authorization</p>
        <button @click="testAuth" class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
          Test Auth Service
        </button>
        <div v-if="authResult" class="mt-4 p-2 bg-gray-100 rounded">
          <pre>{{ authResult }}</pre>
        </div>
      </div>
      
      <div class="bg-white p-6 rounded-lg shadow-md">
        <h2 class="text-2xl font-semibold text-gray-800 mb-4">Documentation Service</h2>
        <p class="text-gray-600 mb-4">Manage and serve documentation</p>
        <button @click="testDocs" class="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
          Test Docs Service
        </button>
        <div v-if="docsResult" class="mt-4 p-2 bg-gray-100 rounded">
          <pre>{{ docsResult }}</pre>
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