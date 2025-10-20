<template>
  <div class="min-h-screen bg-gray-50 grid" :style="gridBg">
  <div class="max-w-screen-2xl mx-auto px-12 py-16">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
        <!-- Left column: Sign in form -->
        <div>
          <h1 class="text-6xl font-extrabold text-gray-900 mb-6">Sign In<span class="text-orange-500">.</span></h1>

          <form @submit.prevent="submit" class="max-w-sm">
            <div class="mb-6">
              <label class="block text-gray-500 mb-2">Email <span class="text-gray-400">*</span></label>
              <input v-model="email" type="email" required
                     class="w-full bg-transparent border-0 border-b-2 border-gray-300 focus:border-black py-2 px-1 text-lg outline-none"/>
            </div>

            <div class="mb-4">
              <label class="block text-gray-500 mb-2">Password <span class="text-gray-400">*</span></label>
              <input v-model="password" type="password" required
                     class="w-full bg-transparent border-0 border-b-2 border-gray-300 focus:border-black py-2 px-1 text-lg outline-none"/>
            </div>

            <div class="flex items-center mt-6 mb-6">
              <label class="inline-flex items-center">
                <input type="checkbox" v-model="remember" class="form-checkbox h-5 w-5 text-black" />
                <span class="ml-3 text-gray-700">Remember me</span>
              </label>
            </div>

            <div class="mb-6">
              <a href="#" class="text-sm text-gray-700 font-medium">Password Help?</a>
            </div>

            <div class="mb-6">
              <button :disabled="loading" type="submit"
                      class="bg-black text-white rounded-md py-3 px-6 w-36 text-center font-semibold hover:opacity-90">
                <span v-if="!loading">Sign In</span>
                <span v-else>Signing in...</span>
              </button>
            </div>

            <div class="text-sm text-gray-700">
              Don't have an account? <br />
              <a @click.prevent="$router.push('/register')" class="font-semibold underline">Create One Now</a>
            </div>
          </form>
        </div>

        <div class="flex justify-center lg:justify-end">
        <div class="bg-white rounded-xl shadow-lg overflow-hidden w-full max-w-5xl transform translate-y-12 lg:translate-y-20">
              <div class="p-8 bg-white">
                <img :src="previewImg" alt="Papairs preview" class="w-full h-auto rounded-md"/>
              </div>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import preview from '../../Images/Screenshot 2025-10-15 at 16.17.48.png'

export default {
  name: 'LoginView',
  data() {
    return {
      email: '',
      password: '',
      remember: false,
      loading: false,
      error: null,
      success: false,
      // inline style for subtle grid background similar to screenshot
      gridBg: {
        backgroundImage: 'linear-gradient(to right, rgba(234,179,8,0.04) 1px, transparent 1px), linear-gradient(to bottom, rgba(234,179,8,0.04) 1px, transparent 1px)',
        backgroundSize: '80px 80px, 80px 80px'
      }
      ,
      previewImg: preview
    }
  },
  methods: {
    async submit() {
      this.error = null
      this.success = false
      this.loading = true
      try {
        const resp = await axios.post('http://localhost:8081/api/auth/login', {
          username: this.email,
          password: this.password
        })

        const token = resp.data?.token || resp.data?.accessToken || resp.data?.sessionToken || null
        if (token) {
          localStorage.setItem('papairs_token', token)
          this.success = true
          setTimeout(() => this.$router.push({ name: 'Home' }), 700)
        } else {
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

<style scoped>
/* minor tweaks to better match screenshot spacing */
.form-checkbox { border: 1px solid #cbd5e1; }
</style>
