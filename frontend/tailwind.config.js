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
        // Fixed colors (same in light and dark mode)
        accent: '#FF7700',
        red: '#FF0000',
        black: '#000000',
        white: '#FFFFFF',
        
        // Adaptive colors (change based on light/dark mode)
        background: 'rgb(var(--color-background) / <alpha-value>)',
        'background-secondary': 'rgb(var(--color-background-secondary) / <alpha-value>)',
        
        text: {
          DEFAULT: 'rgb(var(--color-text) / <alpha-value>)',
          secondary: 'rgb(var(--color-text-secondary) / <alpha-value>)',
        },
        
        border: {
          DEFAULT: 'rgb(var(--color-border) / <alpha-value>)',
          opa: 'rgb(var(--color-border-opa) / <alpha-value>)',
        },
        
        // Legacy colors (keeping for backwards compatibility)
        surface: {
          light: '#FFFFFF',
          'light-secondary': '#F3F3F3',
          dark: '#191919',
          'dark-secondary': '#202020',
        },
        
        content: {
          primary: '#2A2A2A',
          secondary: '#B8B8B8',
          inverse: '#F0FEED',
          black: '#000000',
        },
      },
    },
  },
  plugins: [],
}