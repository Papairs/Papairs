<script>
  import axios from 'axios'
  import auth from '@/utils/auth'
  import { useTheme } from '@/composables/useTheme';

  export default {
    name: 'HomeView',
    data() {
      return {
        authResult: null,
        docsResult: null,
        pageName: '',
        pageResult: null,
        pageError: null,
        creatingPage: false
      }
    },
    setup() {
      const { isDark, toggleTheme, initTheme } = useTheme();
      
      return {
        isDark,
        toggleTheme,
        initTheme
      }
    },
    mounted() {
      this.initTheme();
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
      },
      async createPage() {
        // Reset previous results
        this.pageResult = null
        this.pageError = null
        
        // Validate inputs
        if (!this.pageName.trim()) {
          this.pageError = 'Page name is required'
          return
        }
        
        const userId = auth.getUserId()
        console.log('User ID from auth.getUserId():', userId)
        console.log('User data from localStorage:', auth.getUser())
        console.log('Auth headers:', auth.getUserIdHeader())
        
        // Temporary workaround: use a known user ID if no user is logged in
        const actualUserId = userId
        console.log('Using user ID:', actualUserId)
        if (!actualUserId) {
          this.pageError = 'You must be logged in to create a page'
          return
        }
        
        this.creatingPage = true
        
        try {
          const response = await axios.post('http://localhost:8082/api/docs/pages', {
            title: this.pageName.trim(),
            folderId: null // Optional - no folder for now
          }, {
            headers: {
              'X-User-Id': actualUserId, // Use the fallback user ID
              'Content-Type': 'application/json'
            }
          })
          
          this.pageResult = response.data
          this.pageName = '' // Clear the form
          
          console.log('Page created successfully:', response.data)
          
        } catch (error) {
          console.error('Error creating page:', error)
          this.pageError = error.response?.data?.message || error.message || 'Failed to create page'
        } finally {
          this.creatingPage = false
        }
      }
    }
  }
</script>

<template>
  <div class="bg-surface-light dark:bg-surface-dark transition-colors">
    <nav class="bg-surface-light dark:bg-surface-dark-secondary shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <h1 class="text-xl font-bold text-content-primary dark:text-content-inverse">
              Papairs
            </h1>
          </div>
          <div class="flex items-center space-x-4">
            <router-link 
              to="/" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Home
            </router-link>
            <router-link 
              to="/docs" 
              class="text-content-secondary hover:text-content-primary dark:hover:text-content-inverse px-3 py-2 rounded-md"
            >
              Docs
            </router-link>
            
            <button 
              @click="toggleTheme"
              class="p-2 rounded-md text-content-primary dark:text-content-inverse hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
              :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
            >
              {{ isDark ? '☀️' : '🌙' }}
            </button>
            
            <router-link 
              to="/login" 
              class="bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded"
            >
              Login
            </router-link>
          </div>
        </div>
      </div>
    </nav>
  </div>
  <div class="text-center">
    <h1 class="text-4xl font-bold text-content-primary dark:text-content-inverse mb-4 transition-colors">
      Welcome to Papairs
    </h1>
    <p class="text-lg text-content-secondary mb-8 transition-colors">
      A simple Vue.js frontend with Spring Boot backends
    </p>
    
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-6xl mx-auto">

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

      <div class="bg-surface-light dark:bg-surface-dark-secondary p-6 rounded-lg shadow-md transition-colors">
        <h2 class="text-2xl font-semibold text-content-primary dark:text-content-inverse mb-4 transition-colors">
          Create New Page
        </h2>
        <p class="text-content-secondary mb-4 transition-colors">
          Create a new collaborative document page
        </p>
        
        <div class="mb-4">
          <label class="block text-content-primary dark:text-content-inverse text-sm font-bold mb-2">
            Page Name
          </label>
          <input 
            v-model="pageName"
            type="text" 
            name="page-name"
            autocomplete="off"
            placeholder="Enter page name..."
            class="w-full px-3 py-2 border border-border-light dark:border-border-dark rounded focus:outline-none focus:border-accent bg-surface-light-secondary dark:bg-surface-dark text-content-primary dark:text-content-inverse"
            :disabled="creatingPage"
          />
        </div>
        
        <button 
          @click="createPage" 
          :disabled="!pageName.trim() || creatingPage"
          class="w-full bg-accent hover:bg-[#E66900] disabled:bg-gray-400 disabled:cursor-not-allowed text-content-inverse font-bold py-2 px-4 rounded transition-colors mb-4"
        >
          {{ creatingPage ? 'Creating...' : 'Create Page' }}
        </button>
        
        <div 
          v-if="pageResult" 
          class="mt-4 p-3 bg-surface-light-secondary dark:bg-surface-dark rounded transition-colors"
        >
          <div class="text-content-primary dark:text-content-inverse text-sm">
            <div class="font-semibold mb-2">Page Created Successfully!</div>
            <div><strong>ID:</strong> {{ pageResult.pageId }}</div>
            <div><strong>Name:</strong> {{ pageResult.title }}</div>
            <div class="mt-2">
              <a 
                :href="`/docs/${pageResult.pageId}`" 
                class="text-accent hover:text-[#E66900] underline"
                @click.prevent="$router.push(`/docs/${pageResult.pageId}`)"
              >
                Open Page →
              </a>
            </div>
          </div>
        </div>
        
        <div 
          v-if="pageError" 
          class="mt-4 p-3 bg-red-100 dark:bg-red-900 border border-red-300 dark:border-red-700 rounded transition-colors"
        >
          <div class="text-red-700 dark:text-red-300 text-sm">
            <strong>Error:</strong> {{ pageError }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
