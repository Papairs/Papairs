import { ref } from 'vue';

const isDark = ref(false);

export function useTheme() {
  const setTheme = (dark) => {
    isDark.value = dark;
    
    if (dark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  };

  const toggleTheme = () => {
    setTheme(!isDark.value);
  };

  const initTheme = () => {
    const savedTheme = localStorage.getItem('theme');
    
    if (savedTheme) {
      setTheme(savedTheme === 'dark');
    } else {

      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setTheme(prefersDark);
    }
  };

  return {
    isDark,
    setTheme,
    toggleTheme,
    initTheme,
  };
}