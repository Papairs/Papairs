<template>
  <div class="relative" style="max-width: 500px; height: 48px; gap: 24px;">
    <svg class="absolute left-6 top-1/2 transform -translate-y-1/2 w-5 h-5 text-content-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
    </svg>
    <input 
      type="text" 
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('search', modelValue)"
      :placeholder="placeholder"
      class="w-full h-full bg-[#F3F3F3] dark:bg-surface-dark text-content-primary dark:text-content-inverse focus:outline-none focus:ring-2 focus:ring-accent transition-shadow"
      style="border: 2px solid #202020BF; border-radius: 100px; padding-left: 56px; padding-right: 24px;"
    />
  </div>
</template>

<script>
export default {
  name: 'SearchBar',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: 'Search in Papairs'
    }
  },
  emits: ['update:modelValue', 'search']
}
</script>
