<script>
export default {
  name: 'SearchIcon',
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
    <svg width="24" height="23" viewBox="0 0 24 23" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M21.9535 20.7625L15.3081 14.4948M17.7628 8.905C17.7628 13.2708 14.0103 16.81 9.3814 16.81C4.75248 16.81 1 13.2708 1 8.905C1 4.53919 4.75248 1 9.3814 1C14.0103 1 17.7628 4.53919 17.7628 8.905Z" stroke="#2A2A2A" stroke-width="2" stroke-miterlimit="10" stroke-linecap="square"/>
    </svg>
</template>
