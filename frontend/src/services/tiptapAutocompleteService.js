/**
 * Tiptap Autocomplete Service
 * Provides inline AI-powered autocomplete suggestions for Tiptap editor
 */

class TiptapAutocompleteService {
  constructor(apiUrl = 'http://localhost:3001/autocomplete') {
    this.apiUrl = apiUrl
    this.currentSuggestion = ''
    this.isLoading = false
    this.timer = null
    this.debounceDelay = 1000 // 1 second delay
    this.lastFetchedText = ''
    this.onSuggestionChangeCallback = null
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
      return this.currentSuggestion
    }

    this.isLoading = true
    this.lastFetchedText = text

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
      
      this.updateSuggestion(suggestion)
      return suggestion
    } catch (error) {
      console.error('Error fetching autocomplete suggestion:', error)
      this.clearSuggestion()
      return ''
    } finally {
      this.isLoading = false
    }
  }

  /**
   * Request suggestion with debouncing
   * @param {string} text - Current text content
   */
  requestSuggestion(text) {
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

    // Set new debounced request
    this.timer = setTimeout(() => {
      this.fetchSuggestion(text)
    }, this.debounceDelay)
  }

  /**
   * Update current suggestion and notify listeners
   * @param {string} suggestion - New suggestion text
   */
  updateSuggestion(suggestion) {
    this.currentSuggestion = suggestion
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
