import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DocsView from '../views/DocsView.vue'
import LoginView from '../views/LoginView.vue'
import AutocompleteView from '../views/AutocompleteView.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/docs',
    name: 'Docs',
    component: DocsView
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/autocomplete',
    name: 'Autocomplete',
    component: AutocompleteView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router