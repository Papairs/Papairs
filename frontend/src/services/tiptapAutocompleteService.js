/**
 * Tiptap Autocomplete Service
 * Provides inline AI-powered autocomplete suggestions for Tiptap editor
 */

// Generate unique ID for each service instance
let instanceCounter = 0

class TiptapAutocompleteService {
  constructor(apiUrl = 'http://localhost:3001/autocomplete') {
    this.instanceId = ++instanceCounter
    this.apiUrl = apiUrl
    this.currentSuggestion = ''
    this.isLoading = false
    this.timer = null
    this.debounceDelay = 3000 // 3 second delay
    this.lastFetchedText = ''
    this.lastInputTime = 0
    this.onSuggestionChangeCallback = null
    console.log(`[Autocomplete Service #${this.instanceId}] Created`)
  }

  /**
   * Set callback for when suggestion changes
   * @param {Function} callback - Called with new suggestion text
   */
  onSuggestionChange(callback) {
    this.onSuggestionChangeCallback = callback
  }

  /**
   * Fetch autocomplete suggestion from AI service
   * @param {string} text - Current text content
   * @returns {Promise<string>} Suggestion text
   */
  async fetchSuggestion(text) {
    if (!text.trim()) {
      this.clearSuggestion()
      return ''
    }

    // Avoid duplicate requests for same text
    if (text === this.lastFetchedText) {
      console.log(`[Autocomplete Service #${this.instanceId}] Using cached suggestion`)
      return this.currentSuggestion
    }

    this.isLoading = true
    this.lastFetchedText = text

    console.log(`[Autocomplete Service #${this.instanceId}] Fetching suggestion for: "${text.substring(text.length - 50)}"`)

    try {
      const response = await fetch(this.apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userInput: text })
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()
      const suggestion = (data.suggestion || '').trim()
      
      console.log(`[Autocomplete Service #${this.instanceId}] Received suggestion: "${suggestion}"`)
      
      this.updateSuggestion(suggestion)
      return suggestion
    } catch (error) {
      console.error(`[Autocomplete Service #${this.instanceId}] Error:`, error)
      this.clearSuggestion()
      return ''
    } finally {
      this.isLoading = false
    }
  }

  /**
   * Request suggestion with debouncing
   * @param {string} text - Current text content
   * @param {boolean} isUserInput - True if triggered by user typing
   */
  requestSuggestion(text, isUserInput = true) {
    // Update last input time
    if (isUserInput) {
      this.lastInputTime = Date.now()
    }

    // Clear any pending request
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }

    // Clear suggestion immediately if text is empty
    if (!text.trim()) {
      this.clearSuggestion()
      return
    }

    // Don't request if text hasn't changed
    if (text === this.lastFetchedText) {
      return
    }

    // Set new debounced request (3 seconds after user stops typing)
    this.timer = setTimeout(() => {
      // Only fetch if enough time has passed since last input
      const timeSinceInput = Date.now() - this.lastInputTime
      if (timeSinceInput >= this.debounceDelay - 100) {
        console.log(`[Autocomplete Service #${this.instanceId}] Debounce complete, fetching suggestion`)
        this.fetchSuggestion(text)
      }
    }, this.debounceDelay)
  }

  /**
   * Update current suggestion and notify listeners
   * @param {string} suggestion - New suggestion text
   */
  updateSuggestion(suggestion) {
    this.currentSuggestion = suggestion
    console.log(`[Autocomplete Service #${this.instanceId}] Updating suggestion: "${suggestion}"`)
    if (this.onSuggestionChangeCallback) {
      this.onSuggestionChangeCallback(suggestion)
    }
  }

  /**
   * Clear current suggestion
   */
  clearSuggestion() {
    this.currentSuggestion = ''
    this.lastFetchedText = ''
    console.log(`[Autocomplete Service #${this.instanceId}] Clearing suggestion`)
    if (this.onSuggestionChangeCallback) {
      this.onSuggestionChangeCallback('')
    }
  }

  /**
   * Accept the current suggestion
   * @returns {string} The accepted suggestion text
   */
  acceptSuggestion() {
    const suggestion = this.currentSuggestion
    this.clearSuggestion()
    return suggestion
  }

  /**
   * Check if there's a suggestion available
   * @returns {boolean}
   */
  hasSuggestion() {
    return this.currentSuggestion.length > 0
  }

  /**
   * Get current suggestion text
   * @returns {string}
   */
  getSuggestion() {
    return this.currentSuggestion
  }

  /**
   * Cleanup resources
   */
  destroy() {
    console.log(`[Autocomplete Service #${this.instanceId}] Destroyed`)
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }
    this.clearSuggestion()
    this.onSuggestionChangeCallback = null
  }
}

/**
 * Create a new autocomplete service instance
 * @param {string} apiUrl - API endpoint URL
 * @returns {TiptapAutocompleteService}
 */
export function createTiptapAutocompleteService(apiUrl) {
  return new TiptapAutocompleteService(apiUrl)
}

export default TiptapAutocompleteService
