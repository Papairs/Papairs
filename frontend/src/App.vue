<template>
  <div>
    <main>
      <router-view /> 
    </main>
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