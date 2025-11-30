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
    </div>
  </div>
</template>

<script>
import TestCard from './TestCard.vue';
import { docsController } from '@/controllers/index.js';

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
    }
  }
}
</script>