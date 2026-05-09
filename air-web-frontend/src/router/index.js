import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import RealtimeDashboard from '../views/RealtimeDashboard.vue'
import History from '../views/History.vue'
import Prediction from '../views/Prediction.vue'
import RandomForestPrediction from '../views/RandomForestPrediction.vue'
import CorrelationAnalysis from '../views/CorrelationAnalysis.vue'
import DataQuery from '../views/DataQuery.vue'
import DataManage from '../views/DataManage.vue'
import RealtimeDataManage from '../views/RealtimeDataManage.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', name: 'Dashboard', component: Dashboard },
      { path: 'realtime', name: 'RealtimeDashboard', component: RealtimeDashboard },
      { path: 'history', name: 'History', component: History },
      { path: 'correlation', name: 'CorrelationAnalysis', component: CorrelationAnalysis },
      { path: 'prediction', name: 'Prediction', component: Prediction },
      { path: 'rf-prediction', name: 'RandomForestPrediction', component: RandomForestPrediction },
      { path: 'query', name: 'DataQuery', component: DataQuery },
      { path: 'manage', name: 'DataManage', component: DataManage },
      { path: 'realtime-manage', name: 'RealtimeDataManage', component: RealtimeDataManage }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
