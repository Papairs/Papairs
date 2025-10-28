<template>
  <div class="min-h-screen bg-surface-light dark:bg-surface-dark transition-colors">
    <nav v-if="$route.name !== 'Login'" class="bg-surface-light dark:bg-surface-dark-secondary shadow-lg">
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
            
            <button 
              @click="login" 
              class="bg-accent hover:bg-[#E66900] text-content-inverse font-bold py-2 px-4 rounded"
            >
              Login
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main v-if="$route.name !== 'Login'" class="max-w-7xl mx-auto py-6 px-4">
      <router-view /> 
    </main>

    <div v-else>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useTheme } from '@/composables/useTheme';

const { isDark, toggleTheme, initTheme } = useTheme();

onMounted(() => {
  initTheme();
});

const login = async () => {
  try {
    const response = await this.$http.post('http://localhost:8081/api/auth/login', {
      username: 'test',
      password: 'test'
    });
    console.log('Login response:', response.data);
  } catch (error) {
    console.error('Login error:', error);
  }
};
</script>