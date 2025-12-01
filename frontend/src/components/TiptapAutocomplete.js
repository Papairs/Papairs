/**
 * Tiptap Autocomplete Extension
 * Displays inline AI-powered suggestions in the editor
 */

import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

export const AutocompletePluginKey = new PluginKey('autocomplete')

/**
 * Create Tiptap Autocomplete Extension
 * @param {Object} autocompleteService - The autocomplete service instance
 * @returns {Extension} Tiptap extension
 */
export const TiptapAutocomplete = (autocompleteService) => {
  return Extension.create({
    name: 'autocomplete',

    addOptions() {
      return {
        autocompleteService,
        suggestionClass: 'tiptap-suggestion',
      }
    },

    addProseMirrorPlugins() {
      const { autocompleteService } = this.options

      return [
        new Plugin({
          key: AutocompletePluginKey,
          
          state: {
            init() {
              return {
                suggestion: '',
                decorations: DecorationSet.empty,
              }
            },
            
            apply(tr, value, oldState, newState) {
              const meta = tr.getMeta(AutocompletePluginKey)
              
              // Update suggestion from meta
              if (meta?.suggestion !== undefined) {
                const suggestion = meta.suggestion
                
                if (!suggestion) {
                  return {
                    suggestion: '',
                    decorations: DecorationSet.empty,
                  }
                }

                // Get cursor position
                const { from, to } = newState.selection
                
                // Only show suggestion at cursor if it's a cursor (not selection)
                if (from !== to) {
                  return {
                    suggestion: '',
                    decorations: DecorationSet.empty,
                  }
                }

                // Create decoration at cursor position
                const decoration = Decoration.widget(to, () => {
                  const span = document.createElement('span')
                  span.className = 'tiptap-autocomplete-suggestion'
                  span.style.cssText = `
                    color: #6366f1;
                    opacity: 0.5;
                    font-style: italic;
                    pointer-events: none;
                    user-select: none;
                  `
                  span.textContent = suggestion
                  return span
                }, {
                  side: 1,
                })

                return {
                  suggestion,
                  decorations: DecorationSet.create(newState.doc, [decoration]),
                }
              }

              // Map decorations through document changes
              return {
                suggestion: value.suggestion,
                decorations: value.decorations.map(tr.mapping, tr.doc),
              }
            },
          },

          props: {
            decorations(state) {
              return this.getState(state).decorations
            },

            handleKeyDown(view, event) {
              const pluginState = this.getState(view.state)
              
              // Accept suggestion on Tab
              if (event.key === 'Tab' && pluginState.suggestion) {
                event.preventDefault()
                
                const suggestion = pluginState.suggestion
                const { from } = view.state.selection
                
                // Insert suggestion text
                const tr = view.state.tr.insertText(suggestion, from)
                
                // Clear suggestion
                tr.setMeta(AutocompletePluginKey, { suggestion: '' })
                
                view.dispatch(tr)
                
                // Clear in service
                autocompleteService.clearSuggestion()
                
                return true
              }

              // Clear suggestion on Escape
              if (event.key === 'Escape' && pluginState.suggestion) {
                event.preventDefault()
                
                const tr = view.state.tr.setMeta(AutocompletePluginKey, { suggestion: '' })
                view.dispatch(tr)
                
                autocompleteService.clearSuggestion()
                
                return true
              }

              return false
            },
          },

          view(editorView) {
            let isUpdating = false

            // Setup suggestion change listener
            autocompleteService.onSuggestionChange((suggestion) => {
              if (isUpdating) return
              
              isUpdating = true
              const tr = editorView.state.tr.setMeta(AutocompletePluginKey, { 
                suggestion: suggestion 
              })
              editorView.dispatch(tr)
              isUpdating = false
            })

            return {
              update(view, prevState) {
                if (isUpdating) return
                
                const { state } = view
                const { selection, doc } = state
                
                // Check if this update was triggered by our plugin
                const meta = view.state.tr.getMeta(AutocompletePluginKey)
                if (meta) {
                  return
                }
                
                // Don't fetch suggestions if selection is not a cursor
                if (selection.from !== selection.to) {
                  if (autocompleteService.hasSuggestion()) {
                    isUpdating = true
                    autocompleteService.clearSuggestion()
                    isUpdating = false
                  }
                  return
                }

                // Don't fetch if document hasn't changed
                if (prevState && prevState.doc.eq(doc) && prevState.selection.eq(selection)) {
                  return
                }

                // Extract text content from document and limit to last 50 words
                const fullText = doc.textBetween(0, doc.content.size, ' ', ' ')
                const words = fullText.trim().split(/\s+/)
                const last50Words = words.slice(-100).join(' ')
                
                // Request suggestion with debouncing (won't trigger callback immediately)
                autocompleteService.requestSuggestion(last50Words)
              },

              destroy() {
                autocompleteService.destroy()
              },
            }
          },
        }),
      ]
    },
  })
}

export default TiptapAutocomplete
