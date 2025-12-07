<script>
export default {
  name: 'CheckmarkIcon',
  props: {
    size: {
      type: [String, Number],
      default: '12'
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
    viewBox="0 0 12 12"
    :class="className"
  >
    <path 
      d="m4.01,10.754c-.225.007-.458-.111-.6-.3l-2.25-3c-.249-.332-.181-.802.15-1.05.333-.248.802-.181,1.05.15l1.652,2.203L9.642,1.294c.25-.331.72-.396,1.05-.147.331.25.396.72.147,1.05l-6.23,8.258c-.142.188-.363.298-.599.298Z" 
      stroke-width="0" 
      fill="currentColor"
    />
  </svg>
</template>
