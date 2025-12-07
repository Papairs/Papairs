<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-4xl mx-auto px-4">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-3xl font-bold text-gray-900">Study Mode</h1>
          <button
            @click="exitStudyMode"
            class="px-4 py-2 text-gray-600 hover:text-gray-900 transition-colors"
          >
            ← Back to Flashcards
          </button>
        </div>

        <!-- Progress Bar -->
        <div class="bg-white rounded-lg shadow-sm p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium text-gray-700">
              Progress: {{ completedCount }} / {{ totalCards }}
            </span>
            <span class="text-sm text-gray-500">
              {{ remainingCount }} cards remaining
            </span>
          </div>
          <div class="w-full bg-gray-200 rounded-full h-2">
            <div
              class="bg-accent h-2 rounded-full transition-all duration-300"
              :style="{ width: progressPercentage + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Study Complete Message -->
      <div v-if="studyComplete" class="bg-white rounded-lg shadow-lg p-8 text-center">
        <!-- Circular Progress -->
        <div class="flex justify-center mb-6">
          <CircularProgress :current="learnedCount" :total="totalFlashcards" :size="160" :stroke-width="12" />
        </div>
        
        <h2 class="text-2xl font-bold text-gray-900 mb-2">Study Session Complete!</h2>
        <p class="text-gray-600 mb-6">
          You've reviewed all {{ totalCards }} flashcard{{ totalCards !== 1 ? 's' : '' }}!
        </p>
        <div class="flex gap-4 justify-center flex-wrap">
          <button
            @click="restartStudy"
            class="px-6 py-3 bg-accent text-white rounded-lg hover:opacity-90 transition-opacity"
          >
            Study Unlearned Cards
          </button>
          <button
            @click="resetAllAndRestart"
            class="px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Study Again
          </button>
          <button
            @click="exitStudyMode"
            class="px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Back to Flashcards
          </button>
        </div>
      </div>

      <!-- Flashcard -->
      <div v-else-if="currentCard" class="perspective-1000">
        <div
          class="relative w-full h-96 cursor-pointer"
          @click="flipCard"
          :class="{ 'rotate-y-180': isFlipped }"
          style="transform-style: preserve-3d; transition: transform 0.6s"
        >
          <!-- Front of Card (Question) -->
          <div
            class="absolute inset-0 bg-white rounded-lg shadow-xl p-8 flex flex-col justify-center items-center backface-hidden"
            style="backface-visibility: hidden"
          >
            <div class="text-sm text-gray-500 mb-4">Question</div>
            <div class="text-2xl font-semibold text-gray-900 text-center">
              {{ currentCard.question }}
            </div>
            <div class="mt-6 text-sm text-gray-400">Click to flip</div>
          </div>

          <!-- Back of Card (Answer) -->
          <div
            class="absolute inset-0 bg-blue-50 rounded-lg shadow-xl p-8 flex flex-col justify-center items-center backface-hidden rotate-y-180"
            style="backface-visibility: hidden; transform: rotateY(180deg)"
          >
            <div class="text-sm text-blue-600 mb-4">Answer</div>
            <div class="text-2xl font-semibold text-gray-900 text-center">
              {{ currentCard.answer }}
            </div>
            <div class="mt-6 text-sm text-gray-400">Click to flip back</div>
          </div>
        </div>

        <!-- Tags -->
        <div v-if="currentCard.tags && currentCard.tags.length > 0" class="flex flex-wrap gap-2 mt-4 justify-center">
          <span
            v-for="tag in currentCard.tags"
            :key="tag"
            class="px-3 py-1 bg-gray-100 text-gray-700 text-sm rounded-full"
          >
            #{{ tag }}
          </span>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4 mt-8 justify-center">
          <button
            @click="markAsNotLearned"
            class="px-8 py-4 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition-colors font-medium flex items-center gap-2"
          >
            <XmarkIcon :size="20" />
            Not Learned
          </button>
          <button
            @click="markAsLearned"
            class="px-8 py-4 bg-green-100 text-green-700 rounded-lg hover:bg-green-200 transition-colors font-medium flex items-center gap-2"
          >
            <CheckmarkIcon :size="20" class="text-green-700" />
            Learned
          </button>
        </div>

        <!-- Skip Button -->
        <div class="flex justify-center mt-4">
          <button
            @click="skipCard"
            class="px-4 py-2 text-gray-500 hover:text-gray-700 transition-colors text-sm"
          >
            Skip for now →
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div v-else-if="loading" class="bg-white rounded-lg shadow-lg p-8 text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p class="mt-4 text-gray-600">Loading flashcards...</p>
      </div>

      <!-- No Cards Message -->
      <div v-else class="bg-white rounded-lg shadow-lg p-8 text-center">
        <div class="text-6xl mb-4">📚</div>
        <h2 class="text-2xl font-bold text-gray-900 mb-2">No Cards to Study</h2>
        <p class="text-gray-600 mb-6">
          All your flashcards are marked as learned, or you don't have any flashcards yet.
        </p>
        <button
          @click="exitStudyMode"
          class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          Back to Flashcards
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import auth from '../utils/auth'
import CheckmarkIcon from '@/components/icons/CheckmarkIcon.vue'
import XmarkIcon from '@/components/icons/XmarkIcon.vue'
import CircularProgress from '@/components/CircularProgress.vue'

const router = useRouter()
const route = useRoute()

const flashcards = ref([])
const allFlashcards = ref([])
const currentIndex = ref(0)
const isFlipped = ref(false)
const loading = ref(true)
const studyComplete = ref(false)

const currentCard = computed(() => {
  if (currentIndex.value < flashcards.value.length) {
    return flashcards.value[currentIndex.value]
  }
  return null
})

const totalCards = computed(() => flashcards.value.length)
const completedCount = computed(() => currentIndex.value)
const remainingCount = computed(() => totalCards.value - completedCount.value)
const progressPercentage = computed(() => {
  if (totalCards.value === 0) return 0
  return (completedCount.value / totalCards.value) * 100
})

// Total flashcards and learned count for completion screen
const totalFlashcards = computed(() => allFlashcards.value.length)
const learnedCount = computed(() => allFlashcards.value.filter(card => card.learned).length)

onMounted(async () => {
  await loadFlashcards()
})

async function loadFlashcards() {
  try {
    loading.value = true
    const headers = await auth.getAuthHeaders(router)
    
    // Get filter from query params (passed from FlashcardsView)
    const pageId = route.query.pageId
    
    let response
    if (pageId && pageId !== 'all') {
      // Load flashcards for specific page
      response = await axios.get(
        `http://localhost:8082/api/docs/flashcards/page/${pageId}`,
        { headers }
      )
    } else {
      // Load all user flashcards
      response = await axios.get(
        'http://localhost:8082/api/docs/flashcards',
        { headers }
      )
    }
    
    // Filter to only unlearned cards
    const allCards = response.data.data || []
    allFlashcards.value = allCards
    console.log('All cards:', allCards.length, 'Learned cards:', allCards.filter(card => card.learned).length)
    flashcards.value = allCards.filter(card => !card.learned)
    console.log('Unlearned cards to study:', flashcards.value.length)
    
    if (flashcards.value.length === 0) {
      studyComplete.value = true
    }
  } catch (error) {
    console.error('Failed to load flashcards:', error)
    alert('Failed to load flashcards. Please try again.')
  } finally {
    loading.value = false
  }
}

function flipCard() {
  isFlipped.value = !isFlipped.value
}

async function markAsLearned() {
  await updateLearnedStatus(true)
  nextCard()
}

async function markAsNotLearned() {
  await updateLearnedStatus(false)
  nextCard()
}

async function updateLearnedStatus(learned) {
  try {
    const headers = await auth.getAuthHeaders(router)
    const response = await axios.put(
      `http://localhost:8082/api/docs/flashcards/${currentCard.value.flashcardId}/learned`,
      learned,
      { 
        headers: {
          ...headers,
          'Content-Type': 'application/json'
        }
      }
    )
    // Update the local card state as well
    if (currentCard.value) {
      currentCard.value.learned = learned
    }
    console.log('Updated learned status:', response.data)
  } catch (error) {
    console.error('Failed to update learned status:', error)
    console.error('Error details:', error.response?.data)
  }
}

function skipCard() {
  nextCard()
}

function nextCard() {
  isFlipped.value = false
  currentIndex.value++
  
  if (currentIndex.value >= totalCards.value) {
    studyComplete.value = true
  }
}

async function restartStudy() {
  currentIndex.value = 0
  studyComplete.value = false
  isFlipped.value = false
  await loadFlashcards()
}

async function resetAllAndRestart() {
  try {
    loading.value = true
    const headers = await auth.getAuthHeaders(router)
    
    // Reset all flashcards to unlearned
    await axios.put(
      'http://localhost:8082/api/docs/flashcards/reset',
      null,
      { headers }
    )
    
    console.log('All flashcards reset to unlearned')
    
    // Reload and restart
    currentIndex.value = 0
    studyComplete.value = false
    isFlipped.value = false
    await loadFlashcards()
  } catch (error) {
    console.error('Failed to reset flashcards:', error)
    alert('Failed to reset flashcards. Please try again.')
  }
}

function exitStudyMode() {
  router.push('/flashcards')
}
</script>

<style scoped>
.perspective-1000 {
  perspective: 1000px;
}

.rotate-y-180 {
  transform: rotateY(180deg);
}

.backface-hidden {
  backface-visibility: hidden;
}
</style>
