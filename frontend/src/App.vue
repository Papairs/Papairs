<template>
  <div class="min-h-screen bg-light-secondary">
    <nav class="bg-light-bg shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <h1 class="text-xl font-bold text-dark-text">Papairs</h1>
          </div>
          <div class="flex items-center space-x-4">
            <router-link to="/" class="text-light-text hover:text-dark-text">Home</router-link>
            <router-link to="/docs" class="text-light-text hover:text-dark-text">Docs</router-link>
            <button @click="login" class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
              Login
            </button>
          </div>
        </div>
      </div>
    </nav>
    
    <main class="max-w-7xl mx-auto py-6 px-4">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App',
  methods: {
    async login() {
      try {
        const response = await this.$http.post('http://localhost:8081/api/auth/login', {
          username: 'test',
          password: 'test'
        });
        console.log('Login response:', response.data);
      } catch (error) {
        console.error('Login error:', error);
      }
    }
  }
}
</script>