<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-content-primary dark:text-content-inverse mb-4">
      Documentation Service Tests
    </h2>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Health Check -->
      <TestCard
        title="Health Check"
        description="GET /actuator/health/services/docsService"
        button-text="Test Health"
        :is-loading="testManager.isTestLoading('docs-health')"
        :result="formatResult('docs-health')"
        @test="testDocsHealth"
        @clear-result="clearResult('docs-health')"
      />

      <!-- File Upload for AI Context -->
      <div class="bg-white dark:bg-surface-dark-secondary rounded-lg shadow p-6">
        <h3 class="text-lg font-semibold text-content-primary dark:text-content-inverse mb-4">
          AI Context Files
        </h3>
        
        <!-- Upload Section -->
        <div class="mb-4">
          <label class="block mb-2 text-sm font-medium text-content-primary dark:text-content-inverse">
            Upload File
          </label>
          <input
            type="file"
            @change="uploadFile"
            accept=".txt,.md,.java,.js,.py,.html,.css,.pdf"
            class="block w-full text-sm text-content-secondary file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-accent file:text-white hover:file:bg-accent-hover cursor-pointer"
            :disabled="isUploading"
          />
          <p v-if="uploadError" class="mt-2 text-sm text-red-600">{{ uploadError }}</p>
          <p v-if="uploadSuccess" class="mt-2 text-sm text-green-600">{{ uploadSuccess }}</p>
          <p v-if="isUploading" class="mt-2 text-sm text-content-secondary">Uploading...</p>
        </div>

        <!-- Files List -->
        <div class="space-y-2 max-h-96 overflow-y-auto">
          <h4 class="text-sm font-medium text-content-primary dark:text-content-inverse mb-2">
            Your Files ({{ userFiles.length }})
          </h4>
          <div v-if="userFiles.length === 0" class="text-sm text-content-secondary italic">
            No files uploaded yet
          </div>
          <div
            v-for="file in userFiles"
            :key="file.fileId"
            class="flex items-center justify-between p-2 border border-border-light dark:border-border-dark rounded hover:bg-surface-light dark:hover:bg-surface-dark transition-colors"
          >
            <label class="flex items-center flex-1 cursor-pointer">
              <input
                type="checkbox"
                :checked="isFileSelected(file.fileId)"
                @change="toggleFileSelection(file)"
                class="mr-2 w-4 h-4 text-accent bg-gray-100 border-gray-300 rounded focus:ring-accent"
              />
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-content-primary dark:text-content-inverse truncate">
                  {{ file.filename }}
                </p>
                <p class="text-xs text-content-secondary">{{ (file.fileSize / 1024).toFixed(1) }} KB</p>
              </div>
            </label>
            <button
              @click="deleteFile(file.fileId)"
              class="ml-2 text-red-600 hover:text-red-800 text-lg font-bold"
              title="Delete file"
            >
              ×
            </button>
          </div>
        </div>

        <div v-if="selectedFiles.length > 0" class="mt-4 p-3 bg-blue-50 dark:bg-blue-900 rounded text-sm">
          <p class="font-medium text-blue-900 dark:text-blue-100">
            {{ selectedFiles.length }} file(s) selected for AI context
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import TestCard from './TestCard.vue';
import { docsController } from '@/controllers/index.js';
import axios from 'axios';
import auth from '@/utils/auth';

export default {
  name: 'DocsSection',
  components: {
    TestCard
  },
  props: {
    testManager: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      userFiles: [],
      selectedFiles: [],
      uploadError: null,
      uploadSuccess: null,
      isUploading: false
    };
  },
  mounted() {
    this.loadUserFiles();
  },
  methods: {
    formatResult(testKey) {
      const result = this.testManager.getResult(testKey);
      return result ? this.testManager.formatResult(result) : null;
    },
    
    clearResult(testKey) {
      this.testManager.clearResult(testKey);
    },

    async testDocsHealth() {
      await this.testManager.executeTest('docs-health', () => docsController.checkHealth());
    },

    async loadUserFiles() {
      const token = auth.getToken();
      if (!token) return;

      try {
        const response = await axios.get(
            `http://localhost:8080/api/docs/files`,
            {
              headers: {
                'Authorization': `Bearer ${token}`
              }
            }
        )
        this.userFiles = response.data;
      } catch (error) {
        console.error('Error loading files:', error);
      }
    },

    async uploadFile(event) {
      const file = event.target.files[0];
      if (!file) return;

      this.uploadError = null;
      this.uploadSuccess = null;
      this.isUploading = true;

      const token = auth.getToken();
      if (!token) {
        this.uploadError = 'Token not found. Please log in.';
        this.isUploading = false;
        return;
      }

      const formData = new FormData();
      formData.append('file', file);

      try {
        await axios.post('http://localhost:8080/api/docs/files', formData,
          {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          }
        )

        this.uploadSuccess = `File "${file.name}" uploaded successfully!`;
        await this.loadUserFiles();
        event.target.value = '';
      } catch (error) {
        if (error.response?.status === 409) {
          this.uploadError = `File "${file.name}" already exists`;
        } else {
          this.uploadError = error.response?.data?.message || 'Error uploading file';
        }
      } finally {
        this.isUploading = false;
      }
    },

    async deleteFile(fileId) {
      try {
        await axios.delete(
            `http://localhost:8080/api/docs/files/${fileId}`,
            {
              headers: {
                'Authorization': `Bearer ${auth.getToken()}`
              }
            }
        );
        this.uploadSuccess = 'File deleted successfully';
        await this.loadUserFiles();
        this.selectedFiles = this.selectedFiles.filter(f => f.fileId !== fileId);
      } catch (error) {
        this.uploadError = error.response?.data || 'Error deleting file';
      }
    },

    toggleFileSelection(file) {
      const index = this.selectedFiles.findIndex(f => f.fileId === file.fileId);
      if (index === -1) {
        this.selectedFiles.push(file);
      } else {
        this.selectedFiles.splice(index, 1);
      }
    },

    isFileSelected(fileId) {
      return this.selectedFiles.some(f => f.fileId === fileId);
    }
  }
}
</script>

