/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        accent: '#FF7700',
        red: '#FF0000',
        
        surface: {
          light: '#FFFFFF',
          'light-secondary': '#F3F3F3',
          dark: '#191919',
          'dark-secondary': '#202020',
        },
        
        content: {
          primary: '#2A2A2A',
          secondary: '#B8B8B8',
          inverse: '#F0EFED',
          black: '#000000',
        },
        
        border: {
          light: 'rgba(25, 25, 25, 0.75)',
          dark: 'rgba(32, 32, 32, 0.75)',
          'light-subtle': 'rgba(25, 25, 25, 0.1)',
          'dark-subtle': 'rgba(243, 243, 243, 0.05)',
        },
      },
    },
  },
  plugins: [],
}