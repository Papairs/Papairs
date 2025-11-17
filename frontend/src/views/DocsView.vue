<script>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { useRoute } from 'vue-router'
import StarterKit from '@tiptap/starter-kit'
import Collaboration from '@tiptap/extension-collaboration'
import TextAlign from '@tiptap/extension-text-align'
import Subscript from '@tiptap/extension-subscript'
import Superscript from '@tiptap/extension-superscript'
import { TextStyle } from '@tiptap/extension-text-style'
import { Color } from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import { HocuspocusProvider } from '@hocuspocus/provider'
import * as Y from 'yjs'
import SidebarBase from '@/components/SidebarBase.vue'
import { auth } from '@/utils/auth.js'

export default {
  name: 'DocsView',
  components: { 
    SidebarBase,
    EditorContent 
  },
  setup() {
    const route = useRoute()
    const provider = ref(null)
    const connectionStatus = ref('disconnected')
    const connectedUsers = ref(0)

    // Get document ID from route params or default to 'document-1'
    const documentId = computed(() => route.params.id || route.query.id || 'document-1')
    
    // Get current user ID for collaboration
    const userId = auth.getUserId() || 'anonymous-user'

    // Create Y.js document
    const ydoc = new Y.Doc()

    // Initialize editor with collaboration BEFORE onMounted
    const editor = useEditor({
      extensions: [
        StarterKit.configure({
          history: false, // Collaboration extension has its own history
        }),
        Collaboration.configure({
          document: ydoc,
        }),
        TextAlign.configure({
          types: ['heading', 'paragraph'],
        }),
        // Note: Strike, Bold, Italic, Code, Link, etc. are already in StarterKit
        Subscript,
        Superscript,
        TextStyle,
        Color,
        Highlight.configure({ multicolor: true }),
      ],
      editable: true,
      editorProps: {
        attributes: {
          class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl focus:outline-none p-8',
          style: 'min-height: 100vh;',
        },
      },
    })

    onMounted(() => {
      // Create Hocuspocus provider after component is mounted
      // Use collab-service hostname for Docker network, localhost for browser
      const wsUrl = window.location.hostname === 'localhost' 
        ? 'ws://localhost:3004'
        : `ws://${window.location.hostname}:3004`
      
      provider.value = new HocuspocusProvider({
        url: wsUrl,
        name: documentId.value, // Use actual document ID
        document: ydoc,
        token: userId, // Send user ID as token
        onStatus: ({ status }) => {
          connectionStatus.value = status
          
          // When connected, check if we need to load initial content
          if (status === 'connected') {
            loadInitialContentIfNeeded()
          }
        },
      })

      // Load initial content when provider is ready and document is empty
      const loadInitialContentIfNeeded = async () => {
        // Wait a bit for any existing content to sync
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // Check if editor is empty (only has empty paragraph)
        if (editor.value) {
          const content = editor.value.getHTML()
          console.log('🔍 Current editor content:', content)
          
          // If content is effectively empty, try to load from API
          if (!content || content === '<p></p>' || content.trim() === '') {
            console.log('📄 Editor is empty, attempting to load initial content')
            
            try {
              const response = await fetch(`http://localhost:3005/api/documents/${documentId.value}/initial-content`, {
                headers: {
                  'X-User-Id': userId
                }
              })
              
              if (response.ok) {
                const data = await response.json()
                if (data.content && data.content.trim() !== '' && data.content !== '<p></p>') {
                  console.log('✅ Loading initial content:', data.content)
                  
                  // Set the content in TipTap editor
                  editor.value.commands.setContent(data.content)
                  
                  console.log('🔄 Initial content loaded successfully')
                } else {
                  console.log('📄 No initial content available')
                }
              } else {
                console.warn('⚠️ Failed to load initial content:', response.status)
              }
            } catch (error) {
              console.error('❌ Error loading initial content:', error)
            }
          } else {
            console.log('✅ Editor already has content, skipping initial load')
          }
        }
      }

      // Update connected users count
      provider.value.on('awarenessUpdate', () => {
        const states = provider.value.awareness?.getStates()
        connectedUsers.value = states ? states.size : 0
      })
    })

    onBeforeUnmount(() => {
      if (editor.value) {
        editor.value.destroy()
      }
      if (provider.value) {
        provider.value.destroy()
      }
    })

    const setLink = () => {
      if (!editor.value) return
      
      const previousUrl = editor.value.getAttributes('link').href
      const url = window.prompt('URL', previousUrl)

      if (url === null) {
        return
      }

      if (url === '') {
        editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
        return
      }

      editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
    }

    return { 
      editor, 
      setLink,
      connectionStatus,
      connectedUsers
    }
  }
}
</script>

<style scoped>
/* Basic editor styles */
:deep(.ProseMirror) {
  padding: 3rem;
  min-height: calc(100vh - 100px);
  outline: none;
  cursor: text;
}

:deep(.ProseMirror):focus {
  outline: none;
}

:deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: 'Start typing here...';
  color: #adb5bd;
  pointer-events: none;
  height: 0;
}

:deep(.tiptap > * + *) {
  margin-top: 0.75em;
}

:deep(.tiptap ul),
:deep(.tiptap ol) {
  padding: 0 1rem;
}

:deep(.tiptap h1),
:deep(.tiptap h2),
:deep(.tiptap h3),
:deep(.tiptap h4),
:deep(.tiptap h5),
:deep(.tiptap h6) {
  line-height: 1.1;
  font-weight: bold;
}

:deep(.tiptap h1) {
  font-size: 2em;
}

:deep(.tiptap h2) {
  font-size: 1.5em;
}

:deep(.tiptap h3) {
  font-size: 1.25em;
}

:deep(.tiptap code) {
  background-color: rgba(97, 97, 97, 0.1);
  color: #616161;
  padding: 0.2em 0.4em;
  border-radius: 0.25em;
  font-size: 0.9em;
}

:deep(.tiptap pre) {
  background: #0d0d0d;
  color: #fff;
  font-family: 'JetBrainsMono', monospace;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
}

:deep(.tiptap pre code) {
  color: inherit;
  padding: 0;
  background: none;
  font-size: 0.8rem;
}

:deep(.tiptap blockquote) {
  padding-left: 1rem;
  border-left: 2px solid rgba(13, 13, 13, 0.1);
}

:deep(.tiptap hr) {
  border: none;
  border-top: 2px solid rgba(13, 13, 13, 0.1);
  margin: 2rem 0;
}

/* Toolbar styles */
.toolbar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
  overflow-x: auto;
}

.toolbar button,
.toolbar select {
  padding: 0.375rem 0.75rem;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 0.375rem;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s;
  white-space: nowrap;
}

.toolbar button:hover,
.toolbar select:hover {
  background: #f3f4f6;
}

.toolbar button.is-active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.toolbar button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #e5e7eb;
  margin: 0 0.25rem;
}

/* Collaboration cursor styles */
:deep(.collaboration-cursor__caret) {
  border-left: 2px solid;
  border-right: 2px solid;
  margin-left: -1px;
  margin-right: -1px;
  pointer-events: none;
  position: relative;
  word-break: normal;
}

:deep(.collaboration-cursor__label) {
  border-radius: 3px 3px 3px 0;
  color: #fff;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  left: -1px;
  line-height: normal;
  padding: 0.1rem 0.3rem;
  position: absolute;
  top: -1.4em;
  user-select: none;
  white-space: nowrap;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-connected {
  background-color: #10b981;
}

.status-connecting {
  background-color: #f59e0b;
}

.status-disconnected {
  background-color: #ef4444;
}
</style>

<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0 items-center justify-end px-4">
        <div class="connection-status">
          <span 
            class="status-indicator"
            :class="{
              'status-connected': connectionStatus === 'connected',
              'status-connecting': connectionStatus === 'connecting',
              'status-disconnected': connectionStatus === 'disconnected'
            }"
          ></span>
          <span>{{ connectedUsers }} {{ connectedUsers === 1 ? 'user' : 'users' }} online</span>
        </div>
      </div>
      <div class="flex flex-col flex-1 w-full overflow-hidden">
        <div v-if="editor" class="flex flex-row min-h-[50px] w-full border-b border-border-light-subtle flex-shrink-0 toolbar bg-gray-50">
          <!-- Text Formatting -->
          <button 
            @click="editor?.chain().focus().toggleBold().run()" 
            :disabled="!editor?.can().chain().focus().toggleBold().run()"
            :class="{ 'is-active': editor?.isActive('bold') }"
            title="Bold"
          >
            <strong>B</strong>
          </button>
          <button 
            @click="editor?.chain().focus().toggleItalic().run()" 
            :disabled="!editor?.can().chain().focus().toggleItalic().run()"
            :class="{ 'is-active': editor?.isActive('italic') }"
            title="Italic"
          >
            <em>I</em>
          </button>
          <button 
            @click="editor?.chain().focus().toggleUnderline().run()" 
            :class="{ 'is-active': editor?.isActive('underline') }"
            title="Underline"
          >
            <u>U</u>
          </button>
          <button 
            @click="editor?.chain().focus().toggleStrike().run()" 
            :disabled="!editor?.can().chain().focus().toggleStrike().run()"
            :class="{ 'is-active': editor?.isActive('strike') }"
            title="Strikethrough"
          >
            <s>S</s>
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Headings -->
          <button 
            @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()" 
            :class="{ 'is-active': editor?.isActive('heading', { level: 1 }) }"
            title="Heading 1"
          >
            H1
          </button>
          <button 
            @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()" 
            :class="{ 'is-active': editor?.isActive('heading', { level: 2 }) }"
            title="Heading 2"
          >
            H2
          </button>
          <button 
            @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()" 
            :class="{ 'is-active': editor?.isActive('heading', { level: 3 }) }"
            title="Heading 3"
          >
            H3
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Text Align -->
          <button 
            @click="editor?.chain().focus().setTextAlign('left').run()" 
            :class="{ 'is-active': editor?.isActive({ textAlign: 'left' }) }"
            title="Align Left"
          >
            ≡
          </button>
          <button 
            @click="editor?.chain().focus().setTextAlign('center').run()" 
            :class="{ 'is-active': editor?.isActive({ textAlign: 'center' }) }"
            title="Align Center"
          >
            ≣
          </button>
          <button 
            @click="editor?.chain().focus().setTextAlign('right').run()" 
            :class="{ 'is-active': editor?.isActive({ textAlign: 'right' }) }"
            title="Align Right"
          >
            ≡
          </button>
          <button 
            @click="editor?.chain().focus().setTextAlign('justify').run()" 
            :class="{ 'is-active': editor?.isActive({ textAlign: 'justify' }) }"
            title="Justify"
          >
            ≣
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Lists -->
          <button 
            @click="editor?.chain().focus().toggleBulletList().run()" 
            :class="{ 'is-active': editor?.isActive('bulletList') }"
            title="Bullet List"
          >
            • List
          </button>
          <button 
            @click="editor?.chain().focus().toggleOrderedList().run()" 
            :class="{ 'is-active': editor?.isActive('orderedList') }"
            title="Numbered List"
          >
            1. List
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Special formatting -->
          <button 
            @click="editor?.chain().focus().toggleCode().run()" 
            :disabled="!editor?.can().chain().focus().toggleCode().run()"
            :class="{ 'is-active': editor?.isActive('code') }"
            title="Inline Code"
          >
            &lt;/&gt;
          </button>
          <button 
            @click="editor?.chain().focus().toggleCodeBlock().run()" 
            :class="{ 'is-active': editor?.isActive('codeBlock') }"
            title="Code Block"
          >
            { }
          </button>
          <button 
            @click="editor?.chain().focus().toggleBlockquote().run()" 
            :class="{ 'is-active': editor?.isActive('blockquote') }"
            title="Quote"
          >
            " "
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Subscript / Superscript -->
          <button 
            @click="editor?.chain().focus().toggleSubscript().run()" 
            :class="{ 'is-active': editor?.isActive('subscript') }"
            title="Subscript"
          >
            X₂
          </button>
          <button 
            @click="editor?.chain().focus().toggleSuperscript().run()" 
            :class="{ 'is-active': editor?.isActive('superscript') }"
            title="Superscript"
          >
            X²
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Link -->
          <button 
            @click="setLink" 
            :class="{ 'is-active': editor?.isActive('link') }"
            title="Insert/Edit Link"
          >
            🔗
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Other -->
          <button @click="editor?.chain().focus().setHorizontalRule().run()" title="Horizontal Rule">
            ―
          </button>
          
          <div class="toolbar-divider"></div>
          
          <!-- Undo/Redo -->
          <button @click="editor?.chain().focus().undo().run()" :disabled="!editor?.can().chain().focus().undo().run()" title="Undo">
            ↶
          </button>
          <button @click="editor?.chain().focus().redo().run()" :disabled="!editor?.can().chain().focus().redo().run()" title="Redo">
            ↷
          </button>
        </div>
        <div class="flex flex-row flex-1 w-full overflow-y-auto overflow-x-hidden bg-gray-100">
          <div class="flex flex-row w-full justify-center items-start py-8">
            <editor-content 
              v-if="editor"
              :editor="editor" 
              class="w-full max-w-[1000px] min-h-screen bg-white shadow-lg text-gray-800 text-base"
            />
            <div v-else class="w-full max-w-[1000px] min-h-screen bg-white shadow-lg p-8 text-gray-500">
              Loading editor...
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>