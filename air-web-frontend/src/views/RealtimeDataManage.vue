<template>
  <div class="realtime-data-manage">
    <!-- 页面标题和操作按钮 -->
    <div class="page-header">
      <div class="header-stats">
        <div class="stat-card">
          <span class="stat-value">{{ stationList.length }}</span>
          <span class="stat-label">监测站点数</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ onlineCount }}</span>
          <span class="stat-label">在线站点</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ avgAQI }}</span>
          <span class="stat-label">平均AQI</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="success" size="large" @click="loadData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="primary" size="large" @click="handleBatchSave" :loading="saveLoading" v-if="hasChanges">
          <el-icon><Check /></el-icon>
          保存修改
        </el-button>
      </div>
    </div>

    <!-- 站点数据卡片 -->
    <div class="stations-grid">
      <div 
        v-for="station in realtimeData" 
        :key="station.stationName"
        class="station-card"
        :class="{ 'has-changes': hasStationChanges(station.stationName) }"
      >
        <div class="station-header">
          <div class="station-info">
            <h3>{{ station.stationName }}</h3>
            <span class="station-source">{{ station.actualStation }}</span>
          </div>
          <div class="station-aqi" :style="{ background: station.color }">
            <span class="aqi-value">{{ station.aqi }}</span>
            <span class="aqi-level">{{ station.level }}</span>
          </div>
        </div>

        <div class="station-body">
          <div class="indicators-row">
            <div class="indicator">
              <span class="label">PM2.5</span>
              <el-input-number 
                v-model="editData[station.stationName].pm25" 
                :min="0" 
                :precision="1"
                size="small"
                @change="onValueChange(station.stationName)"
              />
            </div>
            <div class="indicator">
              <span class="label">PM10</span>
              <el-input-number 
                v-model="editData[station.stationName].pm10" 
                :min="0" 
                :precision="1"
                size="small"
                @change="onValueChange(station.stationName)"
              />
            </div>
          </div>
          <div class="indicators-row">
            <div class="indicator">
              <span class="label">SO₂</span>
              <el-input-number 
                v-model="editData[station.stationName].so2" 
                :min="0" 
                :precision="1"
                size="small"
                @change="onValueChange(station.stationName)"
              />
            </div>
            <div class="indicator">
              <span class="label">NO₂</span>
              <el-input-number 
                v-model="editData[station.stationName].no2" 
                :min="0" 
                :precision="1"
                size="small"
                @change="onValueChange(station.stationName)"
              />
            </div>
          </div>
          <div class="indicators-row">
            <div class="indicator">
              <span class="label">CO</span>
              <el-input-number 
                v-model="editData[station.stationName].co" 
                :min="0" 
                :precision="2"
                size="small"
                @change="onValueChange(station.stationName)"
              />
            </div>
            <div class="indicator">
              <span class="label">O₃</span>
              <el-input-number 
                v-model="editData[station.stationName].o3" 
                :min="0" 
                :precision="1"
                size="small"
                @change="onValueChange(station.stationName)"
              />
            </div>
          </div>

          <div class="aqi-control">
            <div class="aqi-input-wrapper">
              <span class="label">AQI指数</span>
              <el-slider 
                v-model="editData[station.stationName].aqi" 
                :min="0" 
                :max="500"
                show-input
                @change="onAQIChange(station.stationName)"
              />
            </div>
            <div class="calculated-level" :class="getLevelClass(editData[station.stationName].aqi)">
              {{ getAQILevel(editData[station.stationName].aqi) }}
            </div>
          </div>

          <div class="pollutant-select">
            <span class="label">首要污染物</span>
            <el-select 
              v-model="editData[station.stationName].primaryPollutant" 
              size="small"
              style="width: 120px"
              @change="onValueChange(station.stationName)"
            >
              <el-option label="无" value="-" />
              <el-option label="PM2.5" value="PM2.5" />
              <el-option label="PM10" value="PM10" />
              <el-option label="O3" value="O3" />
              <el-option label="SO2" value="SO2" />
              <el-option label="CO" value="CO" />
              <el-option label="NO2" value="NO2" />
            </el-select>
          </div>
        </div>

        <div class="station-footer">
          <el-button 
            type="primary" 
            size="small" 
            @click="handleSaveStation(station.stationName)"
            :loading="savingStation === station.stationName"
            :disabled="!hasStationChanges(station.stationName)"
          >
            保存
          </el-button>
          <el-button 
            size="small" 
            @click="handleResetStation(station.stationName)"
            :disabled="!hasStationChanges(station.stationName)"
          >
            重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 修改历史记录 -->
    <div class="history-section" v-if="changeHistory.length > 0">
      <h3>修改记录</h3>
      <el-timeline>
        <el-timeline-item 
          v-for="(record, index) in changeHistory" 
          :key="index"
          :type="record.success ? 'success' : 'danger'"
          :timestamp="record.time"
        >
          {{ record.message }}
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import { getZhengzhouRealtimeData } from '../api/zhengzhou.js'

// 站点列表
const stationList = [
  { value: '中原区', label: '中原区 (北区建设指挥部)' },
  { value: '金水区', label: '金水区 (北区建设指挥部)' },
  { value: '二七区', label: '二七区 (河医大)' },
  { value: '惠济区', label: '惠济区 (惠济区政府)' },
  { value: '郑东新区', label: '郑东新区 (经开区管委)' }
]

// 状态
const loading = ref(false)
const saveLoading = ref(false)
const savingStation = ref('')
const realtimeData = ref([])
const editData = reactive({})
const originalData = reactive({})
const changedStations = ref(new Set())
const changeHistory = ref([])

// 计算属性
const onlineCount = computed(() => realtimeData.value.length)
const avgAQI = computed(() => {
  if (realtimeData.value.length === 0) return 0
  const sum = realtimeData.value.reduce((acc, s) => acc + s.aqi, 0)
  return Math.round(sum / realtimeData.value.length)
})
const hasChanges = computed(() => changedStations.value.size > 0)

// AQI等级判断
function getAQILevel(aqi) {
  if (aqi <= 50) return '优'
  if (aqi <= 100) return '良'
  if (aqi <= 150) return '轻度污染'
  if (aqi <= 200) return '中度污染'
  if (aqi <= 300) return '重度污染'
  return '严重污染'
}

function getLevelClass(aqi) {
  if (aqi <= 50) return 'level-excellent'
  if (aqi <= 100) return 'level-good'
  if (aqi <= 150) return 'level-light'
  if (aqi <= 200) return 'level-moderate'
  if (aqi <= 300) return 'level-severe'
  return 'level-serious'
}

function getAQIColor(aqi) {
  if (aqi <= 50) return '#00e400'
  if (aqi <= 100) return '#ffff00'
  if (aqi <= 150) return '#ff7e00'
  if (aqi <= 200) return '#ff0000'
  if (aqi <= 300) return '#8f3f97'
  return '#7e0023'
}

function getAQITagType(aqi) {
  if (aqi <= 50) return 'success'
  if (aqi <= 100) return 'warning'
  if (aqi <= 150) return 'warning'
  if (aqi <= 200) return 'danger'
  if (aqi <= 300) return 'danger'
  return 'danger'
}

// 检查站点是否有修改
function hasStationChanges(stationName) {
  return changedStations.value.has(stationName)
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    // 1. 从 API 获取原始数据
    const data = await getZhengzhouRealtimeData()
    
    // 2. 从 localStorage 获取自定义数据
    const storageKey = 'air_quality_realtime_custom'
    const customData = JSON.parse(localStorage.getItem(storageKey) || '{}')
    
    // 3. 合并数据（自定义数据优先级更高）
    realtimeData.value = data.map(item => {
      const stationName = item.stationName
      const custom = customData[stationName]
      
      // 如果有自定义数据，使用自定义数据覆盖
      const mergedItem = custom ? { ...item, ...custom } : item
      const aqi = mergedItem.aqi || 0
      
      return {
        ...mergedItem,
        level: getAQILevel(aqi),
        color: getAQIColor(aqi),
        tagType: getAQITagType(aqi)
      }
    })

    // 4. 初始化编辑数据和原始数据
    realtimeData.value.forEach(item => {
      const stationName = item.stationName
      const stationData = {
        aqi: item.aqi || 0,
        pm25: item.pm25 || 0,
        pm10: item.pm10 || 0,
        so2: item.so2 || 0,
        no2: item.no2 || 0,
        co: item.co || 0,
        o3: item.o3 || 0,
        primaryPollutant: item.primaryPollutant || '-'
      }
      editData[stationName] = { ...stationData }
      originalData[stationName] = { ...stationData }
    })

    changedStations.value.clear()
    ElMessage.success('数据加载成功')
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// 值变化处理
function onValueChange(stationName) {
  changedStations.value.add(stationName)
}

// AQI变化处理
function onAQIChange(stationName) {
  changedStations.value.add(stationName)
}

// 保存单个站点
async function handleSaveStation(stationName) {
  savingStation.value = stationName
  try {
    // 这里应该调用API保存数据
    // 暂时保存到 localStorage 作为演示
    const storageKey = 'air_quality_realtime_custom'
    const customData = JSON.parse(localStorage.getItem(storageKey) || '{}')
    
    customData[stationName] = {
      ...editData[stationName],
      updateTime: new Date().toISOString(),
      isCustom: true
    }
    
    localStorage.setItem(storageKey, JSON.stringify(customData))
    
    // 更新原始数据
    originalData[stationName] = { ...editData[stationName] }
    changedStations.value.delete(stationName)
    
    // 添加历史记录
    changeHistory.value.unshift({
      time: new Date().toLocaleString('zh-CN'),
      message: `已保存 ${stationName} 的实时数据`,
      success: true
    })
    
    ElMessage.success(`${stationName} 数据保存成功`)
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
    changeHistory.value.unshift({
      time: new Date().toLocaleString('zh-CN'),
      message: `保存 ${stationName} 数据失败`,
      success: false
    })
  } finally {
    savingStation.value = ''
  }
}

// 批量保存
async function handleBatchSave() {
  try {
    await ElMessageBox.confirm(
      `确定要保存 ${changedStations.value.size} 个站点的修改吗？`,
      '确认保存',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    saveLoading.value = true
    
    const storageKey = 'air_quality_realtime_custom'
    const customData = JSON.parse(localStorage.getItem(storageKey) || '{}')
    
    for (const stationName of changedStations.value) {
      customData[stationName] = {
        ...editData[stationName],
        updateTime: new Date().toISOString(),
        isCustom: true
      }
      originalData[stationName] = { ...editData[stationName] }
    }
    
    localStorage.setItem(storageKey, JSON.stringify(customData))
    
    const savedCount = changedStations.value.size
    changedStations.value.clear()
    
    changeHistory.value.unshift({
      time: new Date().toLocaleString('zh-CN'),
      message: `批量保存成功，共 ${savedCount} 个站点`,
      success: true
    })
    
    ElMessage.success('批量保存成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量保存失败:', error)
      ElMessage.error('批量保存失败')
      changeHistory.value.unshift({
        time: new Date().toLocaleString('zh-CN'),
        message: '批量保存失败',
        success: false
      })
    }
  } finally {
    saveLoading.value = false
  }
}

// 重置单个站点
function handleResetStation(stationName) {
  editData[stationName] = { ...originalData[stationName] }
  changedStations.value.delete(stationName)
  ElMessage.info(`${stationName} 已重置`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.realtime-data-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border: 1px solid #2a3441;
  border-radius: 12px;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #00d4ff;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.station-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border: 1px solid #2a3441;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.station-card:hover {
  border-color: #00d4ff;
  box-shadow: 0 4px 20px rgba(0, 212, 255, 0.15);
}

.station-card.has-changes {
  border-color: #ff7e00;
  box-shadow: 0 0 15px rgba(255, 126, 0, 0.2);
}

.station-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid #2a3441;
}

.station-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: #e2e8f0;
}

.station-source {
  font-size: 12px;
  color: #94a3b8;
}

.station-aqi {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 16px;
  border-radius: 8px;
  min-width: 60px;
}

.aqi-value {
  font-size: 24px;
  font-weight: bold;
  color: #000;
}

.aqi-level {
  font-size: 12px;
  color: #000;
  font-weight: 500;
}

.station-body {
  padding: 20px;
}

.indicators-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.indicator {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.indicator .label {
  font-size: 12px;
  color: #94a3b8;
}

.aqi-control {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #2a3441;
}

.aqi-input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aqi-input-wrapper .label {
  font-size: 12px;
  color: #94a3b8;
}

.calculated-level {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #000;
  min-width: 80px;
  text-align: center;
}

.level-excellent { background: #00e400; }
.level-good { background: #ffff00; }
.level-light { background: #ff7e00; }
.level-moderate { background: #ff0000; color: #fff !important; }
.level-severe { background: #8f3f97; color: #fff !important; }
.level-serious { background: #7e0023; color: #fff !important; }

.pollutant-select {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.pollutant-select .label {
  font-size: 12px;
  color: #94a3b8;
}

.station-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  background: rgba(0, 0, 0, 0.1);
  border-top: 1px solid #2a3441;
}

.history-section {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border: 1px solid #2a3441;
  border-radius: 12px;
  padding: 20px;
}

.history-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #e2e8f0;
}

/* Element Plus 样式覆盖 */
:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-input-number .el-input__inner) {
  background: #0f1419;
  border-color: #2a3441;
  color: #e2e8f0;
}

:deep(.el-slider__runway) {
  background-color: #2a3441;
}

:deep(.el-slider__bar) {
  background: linear-gradient(90deg, #00e400, #ffff00, #ff7e00, #ff0000, #8f3f97, #7e0023);
}

:deep(.el-slider__button) {
  border-color: #00d4ff;
}

:deep(.el-select .el-input__inner) {
  background: #0f1419;
  border-color: #2a3441;
  color: #e2e8f0;
}

:deep(.el-timeline-item__content) {
  color: #e2e8f0;
}

:deep(.el-timeline-item__timestamp) {
  color: #94a3b8;
}
</style>
