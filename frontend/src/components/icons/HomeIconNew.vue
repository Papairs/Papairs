<template>
  <svg 
    :width="size" 
    :height="size" 
    viewBox="0 0 18 18"
    :class="className"
  >
    <line x1="9" y1="16.125" x2="9" y2="12" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></line>
    <path d="m2.5275,5.25L8.1525,1.401c.51-.3495,1.1835-.3495,1.6935,0l5.625,3.849c.408.279.6525.7425.6525,1.2375v6.6375c0,1.6575-1.3425,3-3,3H4.8775c-1.6575,0-3-1.3425-3-3v-6.6375c0-.495.2445-.9585.6525-1.2375Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path>
  </svg>
</template>

<script>
export default {
  name: 'HomeIcon',
  props: {
    size: {
      type: Number,
      default: 12
    },
    className: {
      type: String,
      default: ''
    }
  }
}
</script>
