<script setup>
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useTheme } from '@/composables/useTheme';
import SidebarBase from '@/components/SidebarBase.vue';

const { initTheme } = useTheme();
const route = useRoute();

const showSidebar = computed(() => {
  return !route.path.startsWith('/login') && !route.path.startsWith('/register');
});

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
