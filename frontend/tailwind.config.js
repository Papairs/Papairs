/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        accent: '#FF7700',
        'light-bg': '#FFFFFF',
        'light-secondary': '#F3F3F3',
        'dark-text': '#2A2A2A',
        'black': '#000000',
        'light-text': '#B8B8B8',
        'white-text': '#F0FEED',
        'dark-border': 'rgba(32, 32, 32, 0.75)',
        'light-border': 'rgba(25, 25, 25, 0.75)',
        'opa-border': 'rgba(25, 25, 25, 0.1)',
        'red': '#FF0000',
        'dark-bg': '#191919',
        'dark-secondary': '#202020',
        'dark-opa-border': 'rgba(243, 243, 243, 0.05)',
      },
    },
  },
  plugins: [],
}