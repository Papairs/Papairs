<template>
  <div class="min-h-screen bg-surface-light">
    <LoginHeader />
    
    <div class="flex flex-col items-center justify-center px-8 py-16">
      <h1 class="text-3xl font-bold text-content-primary mb-8">
        Autocomplete Test
      </h1>
      
      <div class="w-full max-w-3xl">
        <div class="relative mb-4">
          <div class="relative">
            <textarea
              ref="textareaRef"
              v-model="input"
              @keydown="handleKeyDown"
              @input="updateMirror"
              @scroll="syncScroll"
              class="w-full h-80 p-5 text-base font-mono bg-white border border-border-light-subtle rounded-lg resize-y outline-none transition-shadow focus:border-accent focus:shadow-md relative z-0 text-content-primary"
              :placeholder="isFocused ? '' : 'Start typing...'"
              @focus="isFocused = true"
              @blur="isFocused = false"
            ></textarea>
            
            <div 
              v-if="suggestion && input"
              ref="mirrorRef"
              class="absolute top-0 left-0 w-full h-80 p-5 text-base font-mono pointer-events-none whitespace-pre-wrap break-words overflow-hidden rounded-lg z-10"
              :style="{ lineHeight: '1.5' }"
            >
              <span class="invisible">{{ input }}</span><span class="text-accent opacity-50 italic">{{ displaySuggestion }}</span>
            </div>
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
import { ref, watch, computed } from 'vue'
import LoginHeader from '../components/LoginHeader.vue'

export default {
  name: 'AutocompleteView',
  components: {
    LoginHeader
  },
  setup() {
    const input = ref('')
    const suggestion = ref('')
    const isFocused = ref(false)
    const textareaRef = ref(null)
    const mirrorRef = ref(null)
    let timer = null

    const displaySuggestion = computed(() => {
      if (!suggestion.value) return ''
      
      // If suggestion already starts with space, use it as-is
      if (suggestion.value.startsWith(' ')) return suggestion.value
      
      // If input ends with space, don't add another space
      if (input.value.endsWith(' ')) return suggestion.value
      
      // Otherwise, add space before suggestion
      return ' ' + suggestion.value
    })

    watch(input, (newValue) => {
      suggestion.value = ''
      if (timer) clearTimeout(timer)
      
      if (newValue.trim()) {
        timer = setTimeout(() => fetchSuggestion(newValue), 1000)
      }
    })

    const fetchSuggestion = async (text) => {
      if (!text.trim()) {
        suggestion.value = ''
        return
      }

      try {
        const response = await fetch('http://localhost:3001/autocomplete', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userInput: text })
        })
        const data = await response.json()
        suggestion.value = (data.suggestion || '').trim()
      } catch (error) {
        console.error('Error fetching suggestion:', error)
        suggestion.value = ''
      }
    }

    const handleKeyDown = (e) => {
      if (e.key === 'Tab' && suggestion.value && input.value) {
        e.preventDefault()
        
        // Add suggestion, ensuring no double spaces
        const currentText = input.value
        const suggestionToAdd = displaySuggestion.value
        
        input.value = currentText + suggestionToAdd
        suggestion.value = ''
      }
    }

    const updateMirror = () => {
      if (mirrorRef.value && textareaRef.value) {
        mirrorRef.value.scrollTop = textareaRef.value.scrollTop
      }
    }

    const syncScroll = () => {
      if (mirrorRef.value && textareaRef.value) {
        mirrorRef.value.scrollTop = textareaRef.value.scrollTop
      }
    }

    return {
      input,
      suggestion,
      displaySuggestion,
      isFocused,
      textareaRef,
      mirrorRef,
      handleKeyDown,
      updateMirror,
      syncScroll
    }
  }
}
</script>

<style scoped>
textarea {
  caret-color: #111;
  line-height: 1.5;
  background-color: white;
}

textarea::placeholder {
  color: #999;
}
</style>