<template>
  <div class="min-h-screen bg-light-secondary">
    <nav v-if="$route.name !== 'Login'" class="bg-light-bg shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <h1 class="text-xl font-bold text-dark-text">Papairs</h1>
          </div>
          <div class="flex items-center space-x-4">
            <router-link to="/" class="text-light-text hover:text-dark-text px-3 py-2 rounded-md">Home</router-link>
            <router-link to="/docs" class="text-light-text hover:text-dark-text px-3 py-2 rounded-md">Docs</router-link>
            <router-link to="/login" class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">Login</router-link>
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

<script>
export default {
  name: 'App',
  // App shell
}
</script>