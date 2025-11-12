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
      <div class="bg-surface-light dark:bg-surface-dark-secondary p-6 rounded-lg shadow-md transition-colors">
        <h2 class="text-2xl font-semibold text-content-primary dark:text-content-inverse mb-4 transition-colors">          
          Autocomplete test
        </h2>
        <p class="text-content-secondary mb-4 transition-colors">
          Test page, showing autocomplete functionality.
        </p>
        <router-link 
          to="/autocomplete" 
          class="bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded transition-colors inline-block"
        >
          Autocomplete Test
        </router-link>
      </div>
    </div>
  </div>
</template>
