<script setup>
import { onMounted } from 'vue';
import { useTheme } from '@/composables/useTheme';
import { useRoute } from 'vue-router';
import { computed } from 'vue';
import SidebarBase from '@/components/SidebarBase.vue';

const { initTheme } = useTheme();
const route = useRoute();

// Hide sidebar only on login page
const showSidebar = computed(() => route.path !== '/login');

onMounted(() => {
  initTheme();
});
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <SidebarBase v-if="showSidebar" />
    <main class="flex-1 overflow-auto">
      <router-view /> 
    </main>
  </div>
</template>
