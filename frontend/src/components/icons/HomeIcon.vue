<script>
export default {
  name: 'HomeIcon',
  props: {
    size: {
      type: [String, Number],
      default: '24'
    },
    className: {
      type: String,
      default: ''
    }
  }
}
</script>

<template>
    <svg width="24" height="22" viewBox="0 0 24 22" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M11.5382 20.6992V15.1936M3.07451 5.71559L10.7252 1.21885C11.2205 0.92705 11.8573 0.92705 12.3541 1.21885L20.0048 5.71559C21.298 6.47536 22.0793 7.78433 22.0793 9.18965V16.57C22.0793 18.8507 20.0575 20.6992 17.563 20.6992H5.51635C3.02182 20.6992 1 18.8507 1 16.57V9.18965C1 7.78433 1.78133 6.47536 3.07451 5.71559Z" stroke="#2A2A2A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
</template>
