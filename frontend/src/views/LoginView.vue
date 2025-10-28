<template>
  <div class="min-h-screen bg-surface-light dark:bg-surface-dark" :style="gridBg">
    <!-- Header with height 100px -->
    <LoginHeader />
    
    <!-- Main content area with specified padding -->
    <div class="px-32 py-16">
      <div class="flex min-h-[calc(100vh-364px)]">
        <!-- Login Form Component - width 500px + 10px padding -->
        <LoginForm 
          ref="loginForm"
          @login="handleLogin"
          @create-account="handleCreateAccount"
        />
        
        <!-- Image Panel Component - fills remaining space with 16:9 ratio -->
        <ImagePanel 
          :image-src="previewImg"
          image-alt="Papairs preview"
        />
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import preview from '../../Images/7 Semester Recording Oct 13 2025.gif'
import LoginHeader from '../components/LoginHeader.vue'
import LoginForm from '../components/LoginForm.vue'
import ImagePanel from '../components/ImagePanel.vue'

export default {
  name: 'LoginView',
  components: {
    LoginHeader,
    LoginForm,
    ImagePanel
  },
  data() {
    return {
      // subtle orange grid using theme accent color
      gridBg: {
        backgroundImage:
          'linear-gradient(to right, rgba(255,119,0,0.08) 2px, transparent 2px),' +
          'linear-gradient(to bottom, rgba(255,119,0,0.08) 2px, transparent 2px)',
        backgroundSize: '80px 80px, 80px 80px',
        backgroundPosition: '0 0, 0 0'
      },
      previewImg: preview
    }
  },
  methods: {
    async handleLogin(loginData) {
      try {
        const resp = await axios.post('http://localhost:8081/api/auth/login', {
          email: loginData.email,
          password: loginData.password
        })
        
        const token = resp.data?.sessionToken || null
        if (token) {
          localStorage.setItem('papairs_token', token)
          setTimeout(() => this.$router.push({ name: 'Home' }), 700)
        } else {
          this.$refs.loginForm.error = 'Login succeeded but no token returned.'
        }
      } catch (err) {
        this.$refs.loginForm.error = err.response?.data?.message || err.message || 'Login failed'
      } finally {
        this.$refs.loginForm.loading = false
      }
    },
    
    handleCreateAccount() {
      this.$router.push('/register')
    }
  }
}
</script>

<style scoped>
/* keep checkbox looking neat in all browsers */
input[type="checkbox"] {
  accent-color: #000; /* modern browsers will render black check */
}
</style>
