<script>
import SidebarBase from '@/components/SidebarBase.vue'


export default {
  name: 'DocsView',
  components: { SidebarBase }
  }
</script>


<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0"></div>
      <div class="flex flex-col flex-1 w-full justify-center items-center overflow-hidden">
        <div class="flex flex-row h-[50px] w-[1200px] border-b border-border-light-subtle flex-shrink-0"></div>
        <div class="flex flex-row flex-1 w-full overflow-y-auto overflow-x-hidden">
          <div class="flex flex-row w-full justify-center items-start">
            <textarea
              placeholder="Start writing your document..."
              class="w-[1000px] bg-white border-x border-border-light-subtle px-12 resize-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12 overflow-hidden min-h-[500px]"
              style="height: auto; min-height: 100%;"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>