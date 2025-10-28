<template>
  <div class="min-h-screen bg-surface-light">
    <LoginHeader />
    
    <div class="flex flex-col items-center justify-center px-8 py-16">
      <h1 class="text-3xl font-bold text-content-primary mb-8">
        Autocomplete Test
      </h1>
      
      <div class="w-full max-w-3xl">
        <div class="relative mb-4">
          <textarea
            v-model="input"
            @keydown="handleKeyDown"
            class="w-full h-80 p-5 text-base font-mono bg-white border border-border-light-subtle rounded-lg resize-y outline-none transition-shadow focus:border-accent focus:shadow-md"
            placeholder="Start typing..."
          ></textarea>
          
          <div 
            v-if="suggestion" 
            class="absolute top-5 left-5 text-base font-mono pointer-events-none whitespace-pre-wrap break-words text-transparent leading-relaxed"
          >
            {{ input }} <span class="text-accent opacity-50 italic">{{ suggestion }}</span>
          </div>
        </div>
        
        <p class="text-center text-content-secondary">
          Press <strong class="text-content-primary bg-border-light-subtle px-2 py-1 rounded">Tab</strong> to accept suggestion
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import LoginHeader from '../components/LoginHeader.vue'

export default {
  name: 'AutocompleteView',
  components: {
    LoginHeader
  },
  setup() {
    const input = ref('')
    const suggestion = ref('')
    let timer = null

    // Watch input changes with debounce
    watch(input, (newValue) => {
      // Clear suggestion immediately when user types
      suggestion.value = ''
      
      // Clear previous timer
      if (timer) clearTimeout(timer)
      
      // Set new timer for API call
      if (newValue.trim()) {
        timer = setTimeout(() => {
          fetchSuggestion(newValue)
        }, 1000)
      }
    })

    const fetchSuggestion = async (text) => {
      try {
        const response = await fetch('http://localhost:3001/autocomplete', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userInput: text })
        })
        const data = await response.json()
        suggestion.value = data.suggestion || ''
      } catch (error) {
        console.error('Error fetching suggestion:', error)
        suggestion.value = ''
      }
    }

    const handleKeyDown = (e) => {
      if (e.key === 'Tab' && suggestion.value) {
        e.preventDefault()
        input.value = input.value + suggestion.value
        suggestion.value = ''
      }
    }

    return {
      input,
      suggestion,
      handleKeyDown
    }
  }
}
</script>

<style scoped>
textarea::placeholder {
  color: #999;
}
</style>