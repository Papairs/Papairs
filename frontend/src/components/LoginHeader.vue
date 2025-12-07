<script>
import GlobeIcon from '@/components/icons/GlobeIcon.vue';

export default {
  name: 'LoginHeader',
  components: {
    GlobeIcon
  }
}
</script>

<template>
  <div>
    <header class="h-16 bg-surface-light flex items-center dark:bg-surface-dark px-8">
      <div class="flex items-center gap-3">
        <img src="@/assets/icons/papairslogo.svg" alt="Papairs Logo" class="h-8" />
        <h1 class="text-xl font-bold text-content-primary dark:text-content-inverse">
          pap<span class="text-accent">AI</span>rs
        </h1>
      </div>
      
      <div class="flex items-center gap-2 text-sm text-content-secondary ml-8">
        <GlobeIcon :size="16" class-name="text-content-secondary dark:text-content-inverse opacity-60" />
        <span class="text-content-secondary dark:text-content-inverse opacity-60">English</span>
        <span class="text-content-secondary dark:text-content-inverse opacity-60">▾</span>
      </div>
    </header>
    <div class="h-0.5 bg-accent"></div>
  </div>
</template>