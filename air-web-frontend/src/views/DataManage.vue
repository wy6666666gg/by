<template>
  <div class="data-manage">
    <!-- 页面标题和操作按钮 -->
    <div class="page-header">
      <div class="header-stats">
        <div class="stat-card">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">总数据条数</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ stationStats.length }}</span>
          <span class="stat-label">监测站点数</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ todayCount }}</span>
          <span class="stat-label">今日新增</span>
        </div>
      </div>
      <el-button type="primary" size="large" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加数据
      </el-button>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="search-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="监测站点">
          <el-select v-model="searchForm.station" placeholder="全部站点" clearable style="width: 150px">
            <el-option
              v-for="station in stationList"
              :key="station.value"
              :label="station.label"
              :value="station.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item label="污染等级">
          <el-select v-model="searchForm.level" placeholder="全部等级" clearable style="width: 120px">
            <el-option label="优" value="优" />
            <el-option label="良" value="良" />
            <el-option label="轻度污染" value="轻度污染" />
            <el-option label="中度污染" value="中度污染" />
            <el-option label="重度污染" value="重度污染" />
            <el-option label="严重污染" value="严重污染" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table
        :data="paginatedData"
        stripe
        border
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="stationName" label="监测站点" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStationType(row.stationName)">{{ row.stationName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="日期" width="120" align="center" />
        <el-table-column prop="aqi" label="AQI" width="90" align="center">
          <template #default="{ row }">
            <span :class="getAQIClass(row.aqi)">{{ row.aqi }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="qualityLevel" label="等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.qualityLevel)" size="small">
              {{ row.qualityLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pm25" label="PM2.5" width="90" align="center">
          <template #default="{ row }">
            <span class="pollutant-value">{{ row.pm25 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pm10" label="PM10" width="90" align="center">
          <template #default="{ row }">
            <span class="pollutant-value">{{ row.pm10 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="o3" label="O₃" width="80" align="center" />
        <el-table-column prop="so2" label="SO₂" width="80" align="center" />
        <el-table-column prop="co" label="CO" width="80" align="center" />
        <el-table-column prop="no2" label="NO₂" width="80" align="center" />
        <el-table-column prop="primaryPollutant" label="首要污染物" width="110" align="center">
          <template #default="{ row }">
            <span :class="{ 'no-pollutant': row.primaryPollutant === '无' }">
              {{ row.primaryPollutant }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="isSandDustDay" label="沙尘天" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSandDustDay" type="danger" size="small">是</el-tag>
            <span v-else class="text-muted">否</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row, $index }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row, $index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredData.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 添加/编辑数据对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="isEdit ? '编辑数据' : '添加数据'"
      width="700px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="data-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="监测站点" prop="stationName">
              <el-select v-model="formData.stationName" placeholder="请选择站点" style="width: 100%">
                <el-option
                  v-for="station in stationList"
                  :key="station.value"
                  :label="station.label"
                  :value="station.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期" prop="date">
              <el-date-picker
                v-model="formData.date"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="AQI指数" prop="aqi">
              <el-input-number
                v-model="formData.aqi"
                :min="0"
                :max="500"
                style="width: 100%"
                @change="onAQIChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="污染等级">
              <el-tag :type="getLevelType(calculatedLevel)" size="large" style="width: 100%; text-align: center;">
                {{ calculatedLevel }}
              </el-tag>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider>污染物浓度 (μg/m³)</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="PM2.5" prop="pm25">
              <el-input-number v-model="formData.pm25" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="PM10" prop="pm10">
              <el-input-number v-model="formData.pm10" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="O₃" prop="o3">
              <el-input-number v-model="formData.o3" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SO₂" prop="so2">
              <el-input-number v-model="formData.so2" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="CO" prop="co">
              <el-input-number v-model="formData.co" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="NO₂" prop="no2">
              <el-input-number v-model="formData.no2" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="首要污染物" prop="primaryPollutant">
              <el-select v-model="formData.primaryPollutant" placeholder="请选择" clearable style="width: 100%">
                <el-option label="无" value="无" />
                <el-option label="PM2.5" value="PM2.5" />
                <el-option label="PM10" value="PM10" />
                <el-option label="O3" value="O3" />
                <el-option label="SO2" value="SO2" />
                <el-option label="CO" value="CO" />
                <el-option label="NO2" value="NO2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="沙尘天气">
              <el-switch
                v-model="formData.isSandDustDay"
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ isEdit ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { addCustomData, getCustomData, deleteCustomData, updateCustomData } from '../api/dataManage'

// 站点列表
const stationList = [
  { value: '中原区', label: '中原区 (北区建设指挥部)' },
  { value: '金水区', label: '金水区 (北区建设指挥部)' },
  { value: '二七区', label: '二七区 (河医大)' },
  { value: '惠济区', label: '惠济区 (惠济区政府)' },
  { value: '郑东新区', label: '郑东新区 (经开区管委)' }
]

// 搜索表单
const searchForm = ref({
  station: '',
  dateRange: [],
  level: ''
})

// 表格数据
const allData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)

// 对话框相关
const showAddDialog = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const editIndex = ref(-1)

// 表单数据
const formData = ref({
  stationName: '',
  stationCode: '',
  date: '',
  aqi: 50,
  qualityLevel: '优',
  pm25: 0,
  pm10: 0,
  o3: 0,
  so2: 0,
  co: 0,
  no2: 0,
  primaryPollutant: '无',
  isSandDustDay: false
})

// 表单验证规则
const formRules = {
  stationName: [{ required: true, message: '请选择监测站点', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  aqi: [{ required: true, message: '请输入AQI值', trigger: 'blur' }],
  pm25: [{ required: true, message: '请输入PM2.5值', trigger: 'blur' }],
  pm10: [{ required: true, message: '请输入PM10值', trigger: 'blur' }]
}

// 站点代码映射
const stationCodeMap = {
  '中原区': '410101',
  '金水区': '410102',
  '二七区': '410103',
  '惠济区': '410108',
  '郑东新区': '410104'
}

// 计算属性：根据AQI计算等级
const calculatedLevel = computed(() => {
  return getAQILevel(formData.value.aqi)
})

// 统计数据
const totalCount = computed(() => allData.value.length)
const todayCount = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return allData.value.filter(d => d.date === today).length
})
const stationStats = computed(() => {
  const stats = {}
  allData.value.forEach(d => {
    if (!stats[d.stationName]) {
      stats[d.stationName] = { name: d.stationName, count: 0 }
    }
    stats[d.stationName].count++
  })
  return Object.values(stats)
})

// 筛选后的数据
const filteredData = computed(() => {
  let result = [...allData.value]

  // 按站点筛选
  if (searchForm.value.station) {
    result = result.filter(d => d.stationName === searchForm.value.station)
  }

  // 按日期范围筛选
  if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
    const [start, end] = searchForm.value.dateRange
    result = result.filter(d => d.date >= start && d.date <= end)
  }

  // 按污染等级筛选
  if (searchForm.value.level) {
    result = result.filter(d => d.qualityLevel === searchForm.value.level)
  }

  // 按日期降序排序
  result.sort((a, b) => new Date(b.date) - new Date(a.date))

  return result
})

// 分页后的数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredData.value.slice(start, end)
})

// 获取AQI等级
function getAQILevel(aqi) {
  if (aqi <= 50) return '优'
  if (aqi <= 100) return '良'
  if (aqi <= 150) return '轻度污染'
  if (aqi <= 200) return '中度污染'
  if (aqi <= 300) return '重度污染'
  return '严重污染'
}

// AQI变化时更新等级
function onAQIChange(val) {
  formData.value.qualityLevel = getAQILevel(val)
}

// 获取AQI样式类
function getAQIClass(aqi) {
  if (aqi <= 50) return 'aqi-excellent'
  if (aqi <= 100) return 'aqi-good'
  if (aqi <= 150) return 'aqi-light'
  if (aqi <= 200) return 'aqi-moderate'
  if (aqi <= 300) return 'aqi-severe'
  return 'aqi-serious'
}

// 获取等级标签类型
function getLevelType(level) {
  const map = {
    '优': 'success',
    '良': '',
    '轻度污染': 'warning',
    '中度污染': 'warning',
    '重度污染': 'danger',
    '严重污染': 'danger'
  }
  return map[level] || ''
}

// 获取站点标签类型
function getStationType(station) {
  const map = {
    '中原区': '',
    '金水区': 'success',
    '二七区': 'warning',
    '惠济区': 'info',
    '郑东新区': 'danger'
  }
  return map[station] || ''
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    allData.value = await getCustomData()
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
}

// 重置
function handleReset() {
  searchForm.value = {
    station: '',
    dateRange: [],
    level: ''
  }
  currentPage.value = 1
}

// 编辑
function handleEdit(row) {
  isEdit.value = true
  editIndex.value = allData.value.findIndex(d => 
    d.stationName === row.stationName && d.date === row.date
  )
  formData.value = { ...row }
  showAddDialog.value = true
}

// 删除
async function handleDelete(row, index) {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${row.stationName} ${row.date} 的数据吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const success = await deleteCustomData(row.stationName, row.date)
    if (success) {
      ElMessage.success('删除成功')
      await loadData()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 提交表单
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 设置站点代码
    formData.value.stationCode = stationCodeMap[formData.value.stationName]
    // 更新等级
    formData.value.qualityLevel = calculatedLevel.value

    let success
    if (isEdit.value) {
      success = await updateCustomData(formData.value)
    } else {
      success = await addCustomData(formData.value)
    }

    if (success) {
      ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
      showAddDialog.value = false
      await loadData()
      resetForm()
    } else {
      ElMessage.error(isEdit.value ? '修改失败' : '添加失败，该日期数据可能已存在')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
function resetForm() {
  isEdit.value = false
  editIndex.value = -1
  formData.value = {
    stationName: '',
    stationCode: '',
    date: '',
    aqi: 50,
    qualityLevel: '优',
    pm25: 0,
    pm10: 0,
    o3: 0,
    so2: 0,
    co: 0,
    no2: 0,
    primaryPollutant: '无',
    isSandDustDay: false
  }
}

// 分页变化
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
}

function handleCurrentChange(val) {
  currentPage.value = val
}

// 监听对话框关闭
watch(showAddDialog, (val) => {
  if (!val) {
    resetForm()
  }
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.data-manage {
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

.search-section {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border: 1px solid #2a3441;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.table-section {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border: 1px solid #2a3441;
  border-radius: 12px;
  padding: 20px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* AQI颜色 */
.aqi-excellent { color: #00e400; font-weight: bold; }
.aqi-good { color: #ffff00; font-weight: bold; }
.aqi-light { color: #ff7e00; font-weight: bold; }
.aqi-moderate { color: #ff0000; font-weight: bold; }
.aqi-severe { color: #8f3f97; font-weight: bold; }
.aqi-serious { color: #7e0023; font-weight: bold; }

.pollutant-value {
  color: #00d4ff;
  font-weight: 500;
}

.no-pollutant {
  color: #94a3b8;
}

.text-muted {
  color: #64748b;
}

/* 表格样式 */
:deep(.el-table) {
  background: transparent;
}

:deep(.el-table th) {
  background: #1a2332;
  color: #00d4ff;
  font-weight: 600;
  border-bottom: 2px solid #00d4ff;
}

:deep(.el-table td) {
  background: transparent;
  color: #e2e8f0;
  font-weight: 500;
}

:deep(.el-table__body tr:nth-child(odd) td) {
  background: rgba(36, 52, 71, 0.6);
}

:deep(.el-table__body tr:nth-child(even) td) {
  background: rgba(26, 35, 50, 0.8);
}

:deep(.el-table__body tr:hover td) {
  background: rgba(0, 212, 255, 0.2) !important;
  color: #fff;
}

/* 表单样式 */
.data-form :deep(.el-form-item__label) {
  color: #94a3b8;
}

.data-form :deep(.el-input__inner),
.data-form :deep(.el-input-number) {
  background: #0f1419;
  border-color: #2a3441;
  color: #fff;
}

.data-form :deep(.el-divider__text) {
  background: #1a2332;
  color: #94a3b8;
}
</style>
