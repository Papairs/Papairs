<template>
  <header class="h-24 bg-surface-light border-b border-border-light-subtle flex items-center justify-between px-8">
    <div class="flex items-center gap-3">
      <div class="w-8 h-8 bg-content-primary rounded flex items-center justify-center">
        <div class="w-4 h-4 bg-surface-light rounded-sm"></div>
      </div>
      <h1 class="text-2xl font-bold text-content-primary">
        pap<span class="text-accent">AI</span>rs
      </h1>
    </div>
    
    <div class="flex items-center gap-2 text-sm text-content-secondary">
      <span>🌐</span>
      <span>English</span>
      <span>▾</span>
    </div>
  </header>
</template>

<script>
export default {
  name: 'LoginHeader'
}
</script>