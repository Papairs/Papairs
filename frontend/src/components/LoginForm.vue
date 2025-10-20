<template>
  <div class="w-[500px] p-2.5 flex-shrink-0">
    <div class="pt-12">
      <h1 class="text-6xl lg:text-7xl font-extrabold text-gray-900 mb-6">
        Sign In<span class="text-orange-500">.</span>
      </h1>

      <form @submit.prevent="handleSubmit" class="max-w-sm">
        <div class="mb-10">
          <input 
            placeholder="Email*"
            v-model="formData.email" 
            type="email" 
            required
            class="w-full bg-transparent border-0 border-b-2 border-gray-300 focus:border-black py-2 px-1 text-xl outline-none"
          />
        </div>

        <div class="mb-6">
          <input 
            placeholder="Password*"
            v-model="formData.password" 
            type="password" 
            required
            class="w-full bg-transparent border-0 border-b-2 border-gray-300 focus:border-black py-2 px-1 text-xl s outline-none"
          />
        </div>

        <div class="flex items-center mt-6 mb-6">
          <label class="inline-flex items-center">
            <input 
              type="checkbox" 
              v-model="formData.remember" 
              class="h-5 w-5 rounded-sm border border-slate-300"
            />
            <span class="ml-3 text-gray-700">Remember me</span>
          </label>
        </div>

        <div class="mb-6">
          <a href="#" class="text-sm text-gray-700 font-medium hover:underline">Password Help?</a>
        </div>

        <div class="mb-8">
          <button 
            :disabled="loading" 
            type="submit"
            class="bg-black text-white rounded-md py-3 px-6 w-40 text-center font-semibold hover:opacity-90 disabled:opacity-60"
          >
            <span v-if="!loading">Sign In</span>
            <span v-else>Signing in...</span>
          </button>
        </div>

        <div class="text-sm text-gray-700">
          Don't have an account? <br />
          <a @click.prevent="$emit('create-account')" class="font-semibold underline cursor-pointer">
            Create One Now
          </a>
        </div>

        <p v-if="error" class="mt-6 text-sm text-red-600">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginForm',
  emits: ['login', 'create-account'],
  data() {
    return {
      formData: {
        email: '',
        password: '',
        remember: false
      },
      loading: false,
      error: null
    }
  },
  methods: {
    async handleSubmit() {
      this.error = null
      this.loading = true
      
      try {
        await this.$emit('login', {
          email: this.formData.email,
          password: this.formData.password,
          remember: this.formData.remember
        })
      } catch (err) {
        this.error = err.message || 'Login failed'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
input[type="checkbox"] {
  accent-color: #000;
}
</style>