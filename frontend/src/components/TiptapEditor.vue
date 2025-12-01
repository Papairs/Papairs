<template>
  <div v-if="editor" class="tiptap-editor-container flex flex-col h-full">
    <!-- Formatting Toolbar -->
    <EditorToolbar
      :activeHeading="activeHeading"
      :currentFontSize="currentFontSize"
      :currentTextColor="currentTextColor"
      :isActive="isActive"
      :canUndo="canUndo"
      :canRedo="canRedo"
      @set-heading="setHeading"
      @set-font-family="setFontFamily"
      @set-font-size="setFontSize"
      @increase-font-size="increaseFontSize"
      @decrease-font-size="decreaseFontSize"
      @toggle-bold="toggleBold"
      @toggle-italic="toggleItalic"
      @toggle-underline="toggleUnderline"
      @toggle-strike="toggleStrike"
      @open-text-color="openTextColorPicker"
      @set-alignment="setTextAlign"
      @toggle-bullet-list="toggleBulletList"
      @toggle-ordered-list="toggleOrderedList"
      @toggle-code="toggleCode"
      @toggle-code-block="toggleCodeBlock"
      @undo="undo"
      @redo="redo"
      class=""
    />

    <!-- Editor Content with centered max-width wrapper -->
    <div class="flex-1 flex justify-center w-full overflow-auto">
      <EditorContentArea
        :editor="editor"
        :currentTextColor="currentTextColor"
        @text-color-change="handleTextColorChange"
        ref="editorContentArea"
        class="w-full max-w-[800px] min-h-full h-fit"
      />
    </div>
  </div>
  <div v-else class="flex items-center justify-center h-full">
    <div class="text-gray-500">Loading editor...</div>
  </div>
</template>

<script>
import { Editor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import FontFamily from '@tiptap/extension-font-family'
import { Color } from '@tiptap/extension-color'
import { Extension } from '@tiptap/core'
import { onBeforeUnmount, ref, watch, nextTick, computed } from 'vue'
import EditorToolbar from './EditorToolbar.vue'
import EditorContentArea from './EditorContent.vue'
import './TiptapEditor.css'
import { createTiptapAutocompleteService } from '@/services/tiptapAutocompleteService'
import { TiptapAutocomplete } from './TiptapAutocomplete'

export default {
  name: 'TiptapEditor',
  components: {
    EditorToolbar,
    EditorContentArea,
  },
  props: {
    modelValue: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: 'Start writing your document...',
    }
  },
  emits: ['update:modelValue', 'ready', 'content-change'],
  setup(props, { emit }) {
    // Constants
    const DEFAULT_TEXT_COLOR = '#000000'
    const DEFAULT_FONT_SIZE = '12pt'
    const FONT_SIZES = ['8pt', '9pt', '10pt', '11pt', '12pt', '14pt', '18pt', '24pt', '30pt', '36pt', '48pt', '60pt', '72pt', '96pt']
    
    // State
    const editor = ref(null)
    const isInitialized = ref(false)
    const lastContent = ref('')
    const currentTextColor = ref(DEFAULT_TEXT_COLOR)
    const currentFontSize = ref(DEFAULT_FONT_SIZE)
    
    // Initialize autocomplete service
    const autocompleteService = createTiptapAutocompleteService()

    // Custom font size extension
    const FontSize = Extension.create({
      name: 'fontSize',
      
      addOptions() {
        return {
          types: ['textStyle'],
        }
      },
      
      addGlobalAttributes() {
        return [
          {
            types: this.options.types,
            attributes: {
              fontSize: {
                default: null,
                parseHTML: element => element.style.fontSize || null,
                renderHTML: attributes => {
                  if (!attributes.fontSize) {
                    return {}
                  }
                  return {
                    style: `font-size: ${attributes.fontSize}`,
                  }
                },
              },
            },
          },
        ]
      },
      
      addCommands() {
        return {
          setFontSize: fontSize => ({ chain }) => {
            return chain()
              .setMark('textStyle', { fontSize })
              .run()
          },
          unsetFontSize: () => ({ chain }) => {
            return chain()
              .setMark('textStyle', { fontSize: null })
              .removeEmptyTextStyle()
              .run()
          },
        }
      },
    })

    // Initialize Tiptap editor
    const initEditor = () => {
      editor.value = new Editor({
        extensions: [
          StarterKit.configure({
            history: {
              depth: 100,
            },
          }),
          Underline,
          TextAlign.configure({
            types: ['heading', 'paragraph'],
          }),
          TextStyle,
          FontFamily,
          FontSize,
          Color,
          TiptapAutocomplete(autocompleteService),
        ],
        content: props.modelValue,
        onUpdate: ({ editor }) => {
          const html = editor.getHTML()
          
          // Only emit update if content actually changed and editor is initialized
          if (isInitialized.value && html !== lastContent.value) {
            lastContent.value = html
            emit('update:modelValue', html)
            emit('content-change', html)
          }
        },
        onCreate: ({ editor }) => {
          nextTick(() => {
            lastContent.value = editor.getHTML()
            isInitialized.value = true
            emit('ready', editor)
          })
        },
        onSelectionUpdate: ({ editor }) => {
          // Update current font size based on selection
          const fontSize = editor.getAttributes('textStyle').fontSize
          
          // Check if we're in a heading
          for (let level = 1; level <= 6; level++) {
            if (editor.isActive('heading', { level })) {
              // Map heading levels to approximate font sizes
              const headingSizes = {
                1: '36pt',
                2: '30pt',
                3: '24pt',
                4: '18pt',
                5: '14pt',
                6: '12pt'
              }
              currentFontSize.value = headingSizes[level]
              return
            }
          }
          
          // If not in heading, use textStyle fontSize or default
          currentFontSize.value = fontSize || DEFAULT_FONT_SIZE
        },
        editorProps: {
          attributes: {
            class: 'prose prose-lg max-w-none focus:outline-none min-h-full',
            spellcheck: 'false',
          },
        },
      })
    }

    // Generic command executor with guard
    const executeEditorCommand = (commandFn) => {
      if (!editor.value) return false
      commandFn(editor.value.chain().focus())
      return true
    }

    // Handle heading selection
    const setHeading = (event) => {
      const level = event.target.value
      executeEditorCommand((chain) => {
        if (level) {
          chain.toggleHeading({ level: parseInt(level) }).run()
        } else {
          chain.setParagraph().run()
        }
      })
    }

    // Get active heading level
    const getActiveHeading = () => {
      if (!editor.value) return ''
      
      for (let level = 1; level <= 6; level++) {
        if (editor.value.isActive('heading', { level })) {
          return level.toString()
        }
      }
      return ''
    }

    // Set font family
    const setFontFamily = (event) => {
      const font = event.target.value
      executeEditorCommand((chain) => {
        font ? chain.setFontFamily(font).run() : chain.unsetFontFamily().run()
      })
    }

    // Set font size
    const setFontSize = (event) => {
      const size = event.target.value
      currentFontSize.value = size
      executeEditorCommand((chain) => {
        size ? chain.setFontSize(size).run() : chain.unsetFontSize().run()
      })
    }

    // Font size utilities
    const changeFontSizeByStep = (step) => {
      if (!editor.value) return
      const currentIndex = FONT_SIZES.indexOf(currentFontSize.value)
      const newIndex = currentIndex + step
      
      if (newIndex >= 0 && newIndex < FONT_SIZES.length) {
        const newSize = FONT_SIZES[newIndex]
        currentFontSize.value = newSize
        editor.value.chain().focus().setFontSize(newSize).run()
      }
    }

    // Increase font size
    const increaseFontSize = () => changeFontSizeByStep(1)

    // Decrease font size
    const decreaseFontSize = () => changeFontSizeByStep(-1)

    // Set text color
    const setTextColor = (event) => {
      if (!editor.value) return
      const color = event.target.value
      currentTextColor.value = color
      editor.value.chain().focus().setColor(color).run()
    }

    watch(() => props.modelValue, (newValue) => {
      if (!editor.value || !isInitialized.value || newValue === lastContent.value) return
      if (newValue === editor.value.getHTML()) return
      
      isInitialized.value = false
      editor.value.commands.setContent(newValue, false)
      lastContent.value = newValue
      nextTick(() => { isInitialized.value = true })
    })

    // Initialize editor on mount
    initEditor()

    onBeforeUnmount(() => {
      autocompleteService.destroy()
      editor.value?.destroy()
    })

    // Computed properties for toolbar
    const activeHeading = computed(() => getActiveHeading())
    const isActive = computed(() => ({
      bold: editor.value?.isActive('bold'),
      italic: editor.value?.isActive('italic'),
      underline: editor.value?.isActive('underline'),
      strike: editor.value?.isActive('strike'),
      code: editor.value?.isActive('code'),
      codeBlock: editor.value?.isActive('codeBlock'),
      bulletList: editor.value?.isActive('bulletList'),
      orderedList: editor.value?.isActive('orderedList'),
      textAlignLeft: editor.value?.isActive({ textAlign: 'left' }),
      textAlignCenter: editor.value?.isActive({ textAlign: 'center' }),
      textAlignRight: editor.value?.isActive({ textAlign: 'right' }),
      textAlignJustify: editor.value?.isActive({ textAlign: 'justify' }),
    }))
    const canUndo = computed(() => editor.value?.can().undo())
    const canRedo = computed(() => editor.value?.can().redo())
    const editorContentArea = ref(null)

    // Create toggle command
    const createToggleCommand = (command) => () => {
      executeEditorCommand((chain) => chain[command]().run())
    }

    // Toolbar action methods
    const toggleBold = createToggleCommand('toggleBold')
    const toggleItalic = createToggleCommand('toggleItalic')
    const toggleUnderline = createToggleCommand('toggleUnderline')
    const toggleStrike = createToggleCommand('toggleStrike')
    const toggleCode = createToggleCommand('toggleCode')
    const toggleCodeBlock = createToggleCommand('toggleCodeBlock')
    const toggleBulletList = createToggleCommand('toggleBulletList')
    const toggleOrderedList = createToggleCommand('toggleOrderedList')
    const undo = createToggleCommand('undo')
    const redo = createToggleCommand('redo')
    
    const setTextAlign = (alignment) => {
      executeEditorCommand((chain) => chain.setTextAlign(alignment).run())
    }
    
    const openTextColorPicker = () => {
      editorContentArea.value?.openColorPicker()
    }

    const handleTextColorChange = (event) => {
      setTextColor(event)
    }

    return {
      editor,
      currentTextColor,
      currentFontSize,
      activeHeading,
      isActive,
      canUndo,
      canRedo,
      editorContentArea,
      setHeading,
      setFontFamily,
      setFontSize,
      increaseFontSize,
      decreaseFontSize,
      setTextColor,
      toggleBold,
      toggleItalic,
      toggleUnderline,
      toggleStrike,
      toggleCode,
      toggleCodeBlock,
      toggleBulletList,
      toggleOrderedList,
      undo,
      redo,
      setTextAlign,
      openTextColorPicker,
      handleTextColorChange,
    }
  },
}
</script>
