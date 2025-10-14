<template>
  <div class="max-w-md mx-auto bg-white p-6 rounded-lg shadow-md">
    <h1 class="text-2xl font-bold mb-4 text-gray-900">Login</h1>

    <form @submit.prevent="submit">
      <div class="mb-4">
        <label class="block text-gray-700 text-sm font-bold mb-2">Email</label>
        <input v-model="email" type="email" required
               class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"/>
      </div>

      <div class="mb-4">
        <label class="block text-gray-700 text-sm font-bold mb-2">Password</label>
        <input v-model="password" type="password" required
               class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"/>
      </div>

      <div class="flex items-center justify-between">
        <button :disabled="loading" type="submit" class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
          <span v-if="!loading">Sign in</span>
          <span v-else>Signing in...</span>
        </button>
      </div>

      <div v-if="error" class="mt-4 text-red-600">
        {{ error }}
      </div>
      <div v-if="success" class="mt-4 text-green-600">
        Logged in successfully (token saved)
      </div>
    </form>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'LoginView',
  data() {
    return {
      email: '',
      password: '',
      loading: false,
      error: null,
      success: false
    }
  },
  methods: {
    async submit() {
      this.error = null
      this.success = false
      this.loading = true
      try {
        const resp = await axios.post('http://localhost:8081/api/auth/login', {
          email: this.email,
          password: this.password
        })

        // expect token in resp.data.token or resp.data.accessToken depending on backend
        const token = resp.data?.sessionToken || null
        if (token) {
          localStorage.setItem('papairs_token', token)
          this.success = true
          // redirect to home after short delay
          setTimeout(() => this.$router.push({ name: 'Home' }), 700)
        } else {
          // show raw response if no token
          this.error = 'Login succeeded but no token returned.'
          console.warn('Login response:', resp.data)
        }
      } catch (err) {
        this.error = err.response?.data?.message || err.message || 'Login failed'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
