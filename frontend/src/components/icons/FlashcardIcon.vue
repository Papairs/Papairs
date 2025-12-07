<script>
export default {
  name: 'FlashcardIcon',
  props: {
    size: {
      type: [String, Number],
      default: '18'
    },
    className: {
      type: String,
      default: ''
    }
  }
}
</script>

<template>
  <svg 
    xmlns="http://www.w3.org/2000/svg" 
    :width="size" 
    :height="size" 
    viewBox="0 0 18 18"
    :class="className"
  >
    <line 
      x1="1.75" 
      y1="7.25" 
      x2="16.25" 
      y2="7.25" 
      fill="none" 
      stroke="currentColor" 
      stroke-linecap="round" 
      stroke-linejoin="round" 
      stroke-width="1.5"
    />
    <rect 
      x="1.75" 
      y="3.75" 
      width="14.5" 
      height="10.5" 
      rx="2" 
      ry="2" 
      transform="translate(18 18) rotate(180)" 
      fill="none" 
      stroke="currentColor" 
      stroke-linecap="round" 
      stroke-linejoin="round" 
      stroke-width="1.5"
    />
    <line 
      x1="4.25" 
      y1="11.25" 
      x2="7.25" 
      y2="11.25" 
      fill="none" 
      stroke="currentColor" 
      stroke-linecap="round" 
      stroke-linejoin="round" 
      stroke-width="1.5"
    />
    <line 
      x1="12.75" 
      y1="11.25" 
      x2="13.75" 
      y2="11.25" 
      fill="none" 
      stroke="currentColor" 
      stroke-linecap="round" 
      stroke-linejoin="round" 
      stroke-width="1.5"
    />
  </svg>
</template>
