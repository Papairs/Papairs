<script>
export default {
  name: 'CircularProgress',
  props: {
    current: {
      type: Number,
      required: true
    },
    total: {
      type: Number,
      required: true
    },
    size: {
      type: Number,
      default: 160
    },
    strokeWidth: {
      type: Number,
      default: 12
    }
  },
  data() {
    return {
      isAnimating: true
    }
  },
  mounted() {
    setTimeout(() => {
      this.isAnimating = false
    }, 100)
  },
  computed: {
    radius() {
      return (this.size - this.strokeWidth) / 2
    },
    circumference() {
      return 2 * Math.PI * this.radius
    },
    progressPercentage() {
      if (this.total === 0) return 0
      return (this.current / this.total) * 100
    },
    strokeDashoffset() {
      if (this.isAnimating) return this.circumference
      return this.circumference - (this.progressPercentage / 100) * this.circumference
    },
    center() {
      return this.size / 2
    }
  }
}
</script>

<template>
  <div class="relative" :style="{ width: size + 'px', height: size + 'px' }">
    <!-- SVG Circle -->
    <svg :width="size" :height="size" class="transform -rotate-90">
      <!-- Background circle (empty gray) -->
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        stroke="#e5e7eb"
        :stroke-width="strokeWidth"
        fill="none"
      />
      <!-- Progress circle (green) -->
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        stroke="#22c55e"
        :stroke-width="strokeWidth"
        fill="none"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="strokeDashoffset"
        class="transition-all duration-1000 ease-out"
        stroke-linecap="round"
      />
    </svg>
    <!-- Center text -->
    <div 
      class="absolute inset-0 flex items-center justify-center transition-all duration-700 ease-out"
      :class="isAnimating ? 'opacity-0 scale-50' : 'opacity-100 scale-100'"
    >
      <div class="text-3xl font-bold text-gray-900">
        {{ current }}<span class="text-gray-400 mx-1">/</span>{{ total }}
      </div>
    </div>
  </div>
</template>
