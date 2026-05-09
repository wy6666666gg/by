import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import * as echarts from 'echarts'
import VueEcharts from 'vue-echarts'
import 'element-plus/dist/index.css'
import userRouter from './user-router/index.js'
import UserApp from './UserApp.vue'

const app = createApp(UserApp)

app.use(ElementPlus)
app.use(userRouter)
app.component('v-chart', VueEcharts)

app.config.globalProperties.$echarts = echarts

app.mount('#user-app')
