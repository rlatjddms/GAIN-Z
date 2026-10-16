<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="name === 'more' ? 2.5 : 1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path :d="paths[name] || paths.file" />
  </svg>
</template>
<script setup>
defineProps({ name: String, size: { type: Number, default: 20 } })
const paths = {
  close: 'm6 6 12 12M6 18 18 6',
  more: 'M5 12h.01M12 12h.01M19 12h.01',
  edit: 'm16 3 5 5M4 20l4-1L21 6a2 2 0 0 0-5-3L3 16l-1 6z',
  trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
  image: 'M4 3h16a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1ZM3 16l6-6 5 5 3-3 4 4M16 7h.01',
  pin: 'm16 3 5 5-4 2-2 5-2 2-6-6 2-2 5-2zM9 15l-6 6',
  file: 'M14 2H5v20h14V7zM14 2v6h5M8 13h8M8 17h5',
  attach: 'm9 12 6-6a3 3 0 0 1 4 4l-8 8a5 5 0 0 1-7-7l9-9M7 14l7-7',
  comment: 'M21 11a9 9 0 0 1-9 9 10 10 0 0 1-4-1l-5 2 1-5a9 9 0 1 1 17-5Z',
  arrow: 'M5 12h14m-5-5 5 5-5 5',
  back: 'M19 12H5m5-5-5 5 5 5',
  plus: 'M12 5v14M5 12h14',
  download: 'M12 3v12m-4-4 4 4 4-4M4 16v5h16v-5',
  play: 'm9 5 11 7-11 7z',
}
</script>
