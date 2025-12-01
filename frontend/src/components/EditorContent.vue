<script>
import { EditorContent } from '@tiptap/vue-3'
import { ref, watch } from 'vue'

export default {
  name: 'EditorContentArea',
  components: {
    EditorContent,
  },
  props: {
    editor: Object,
    currentTextColor: String,
  },
  emits: ['text-color-change'],
  setup(props) {
    const textColorPicker = ref(null)
    const colorValue = ref(props.currentTextColor)

    const openColorPicker = () => textColorPicker.value?.click()

    watch(() => props.currentTextColor, (newColor) => {
      colorValue.value = newColor
    })

    return {
      textColorPicker,
      colorValue,
      openColorPicker,
    }
  },
}

</script>
<template>
  <div class="flex border-x border-t border-border-light-subtle min-h-full w-full">
    <editor-content 
      :editor="editor" 
      class="px-12 py-12 prose prose-lg min-w-full min-h-full focus:outline-none border-x border-t border-border-light-subtle"
    />
    
    <!-- Hidden color picker -->
    <input 
      ref="textColorPicker"
      type="color" 
      @change="$emit('text-color-change', $event)"
      v-model="colorValue"
      class="absolute opacity-0 w-0 h-full pointer-events-none"
    />
  </div>
</template>

