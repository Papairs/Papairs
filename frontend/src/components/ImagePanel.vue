<template>
  <div class="flex-1 flex items-center justify-center">
    <div class="w-full h-full flex items-center justify-center">
      <div class="w-full max-w-4xl aspect-video bg-white rounded-lg shadow-2xl overflow-hidden">
        <img 
          :src="imageSrc" 
          :alt="imageAlt"
          class="w-full h-full object-cover"
        />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ImagePanel',
  props: {
    imageSrc: {
      type: String,
      required: true
    },
    imageAlt: {
      type: String,
      default: 'Preview image'
    }
  }
}
</script>