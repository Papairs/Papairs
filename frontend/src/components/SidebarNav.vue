<template>
  <aside class="w-64 bg-white dark:bg-surface-dark-secondary border-r border-border-light dark:border-border-dark flex flex-col">
    <!-- User Profile -->
    <div class="p-4 border-border-light dark:border-border-dark">
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 rounded-full bg-accent flex items-center justify-center text-white font-semibold">
          {{ userInitial }}
        </div>
        <div class="flex-1">
          <p class="text-sm font-medium text-content-primary dark:text-content-inverse">{{ userName }}</p>
          <p class="text-xs text-content-secondary">{{ userRole }}</p>
        </div>
        <button 
          @click="$emit('toggleTheme')"
          class="p-1 rounded hover:bg-surface-light-secondary dark:hover:bg-surface-dark"
        >
          {{ isDark ? '☀️' : '🌙' }}
        </button>
      </div>
    </div>

    <!-- New Button -->
    <div class="p-4">
      <button 
        @click="$emit('createDocument')"
        class="w-full flex items-center justify-center space-x-2 px-4 py-2 bg-accent hover:bg-[#E66900] text-white rounded-lg transition-colors font-medium"
      >
        <span class="text-xl">+</span>
        <span>New</span>
      </button>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto px-2">
      <div class="space-y-1">
        <button 
          @click="$emit('navigate', 'search')"
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <span class="text-sm">Search</span>
        </button>
        
        <button 
          @click="$emit('navigate', 'home')"
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span class="text-sm">Home</span>
        </button>

        <button 
          @click="$emit('navigate', 'invite')"
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <span class="text-sm">Invite Friends</span>
        </button>
      </div>

      <!-- My Papairs Section -->
      <div class="mt-6">
        <p class="px-3 text-xs font-semibold text-content-secondary uppercase tracking-wider mb-2">
          My papairs
        </p>
        <button 
          @click="$emit('navigate', 'notebook')"
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md bg-accent/10 text-accent transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <span class="text-sm font-medium">My notebook</span>
        </button>

        <button 
          @click="$emit('navigate', 'shared')"
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors mt-1"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
          </svg>
          <span class="text-sm">Shared with me</span>
        </button>

        <button 
          @click="$emit('navigate', 'favorites')"
          class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors mt-1"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
          </svg>
          <span class="text-sm">Favorites</span>
        </button>
      </div>
    </nav>

    <!-- Settings at Bottom -->
    <div class="p-4 border-t border-border-light dark:border-border-dark">
      <button 
        @click="$emit('navigate', 'settings')"
        class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        <span class="text-sm">Settings</span>
      </button>
      <button 
        @click="$emit('navigate', 'trash')"
        class="w-full flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-surface-light-secondary dark:hover:bg-surface-dark text-content-primary dark:text-content-inverse transition-colors mt-1"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        <span class="text-sm">Trash</span>
      </button>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'SidebarNav',
  props: {
    userName: {
      type: String,
      default: 'Gustaw Juul'
    },
    userRole: {
      type: String,
      default: 'Student User'
    },
    isDark: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    userInitial() {
      return this.userName ? this.userName.charAt(0).toUpperCase() : 'G'
    }
  },
  emits: ['navigate', 'createDocument', 'toggleTheme']
}
</script>
