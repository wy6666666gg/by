import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '../user-views/UserLayout.vue'
import UserHome from '../user-views/UserHome.vue'
import UserQuery from '../user-views/UserQuery.vue'
import UserPrediction from '../user-views/UserPrediction.vue'
import UserCompare from '../user-views/UserCompare.vue'

const routes = [
  {
    path: '/',
    component: UserLayout,
    children: [
      { path: '', name: 'UserHome', component: UserHome },
      { path: 'query', name: 'UserQuery', component: UserQuery },
      { path: 'prediction', name: 'UserPrediction', component: UserPrediction },
      { path: 'compare', name: 'UserCompare', component: UserCompare }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/user.html'),
  routes
})

export default router
