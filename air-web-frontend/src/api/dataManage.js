/**
 * 数据管理API
 * 使用localStorage存储用户自定义数据
 * 数据格式与CSV文件保持一致
 */

const STORAGE_KEY = 'air_quality_custom_data'

// 站点代码映射
const STATION_CODE_MAP = {
  '中原区': '410101',
  '金水区': '410102',
  '二七区': '410103',
  '惠济区': '410108',
  '郑东新区': '410104'
}

/**
 * 获取所有自定义数据
 * @returns {Promise<Array>} 数据列表
 */
export async function getCustomData() {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    return data ? JSON.parse(data) : []
  } catch (error) {
    console.error('获取自定义数据失败:', error)
    return []
  }
}

/**
 * 添加单条数据
 * @param {Object} record - 数据记录
 * @returns {Promise<boolean>} 是否成功
 */
export async function addCustomData(record) {
  try {
    const data = await getCustomData()
    
    // 检查是否已存在相同站点和日期的数据
    const exists = data.some(d => 
      d.stationName === record.stationName && d.date === record.date
    )
    
    if (exists) {
      console.warn('数据已存在:', record.stationName, record.date)
      return false
    }
    
    // 确保站点代码正确
    if (!record.stationCode) {
      record.stationCode = STATION_CODE_MAP[record.stationName]
    }
    
    // 添加时间戳
    record.createdAt = new Date().toISOString()
    record.updatedAt = new Date().toISOString()
    record.isCustom = true
    
    data.push(record)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    
    console.log('[DataManage] 添加数据成功:', record.stationName, record.date)
    return true
  } catch (error) {
    console.error('添加数据失败:', error)
    return false
  }
}

/**
 * 批量添加数据
 * @param {Array} records - 数据记录数组
 * @returns {Promise<{success: number, failed: number}>} 添加结果
 */
export async function addCustomDataBatch(records) {
  const result = { success: 0, failed: 0 }
  
  for (const record of records) {
    const success = await addCustomData(record)
    if (success) {
      result.success++
    } else {
      result.failed++
    }
  }
  
  return result
}

/**
 * 更新数据
 * @param {Object} record - 数据记录
 * @returns {Promise<boolean>} 是否成功
 */
export async function updateCustomData(record) {
  try {
    const data = await getCustomData()
    
    const index = data.findIndex(d => 
      d.stationName === record.stationName && d.date === record.date
    )
    
    if (index === -1) {
      console.warn('数据不存在:', record.stationName, record.date)
      return false
    }
    
    // 更新时间戳
    record.updatedAt = new Date().toISOString()
    record.isCustom = true
    
    data[index] = { ...data[index], ...record }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    
    console.log('[DataManage] 更新数据成功:', record.stationName, record.date)
    return true
  } catch (error) {
    console.error('更新数据失败:', error)
    return false
  }
}

/**
 * 删除单条数据
 * @param {string} stationName - 站点名称
 * @param {string} date - 日期
 * @returns {Promise<boolean>} 是否成功
 */
export async function deleteCustomData(stationName, date) {
  try {
    const data = await getCustomData()
    
    const initialLength = data.length
    const filtered = data.filter(d => 
      !(d.stationName === stationName && d.date === date)
    )
    
    if (filtered.length === initialLength) {
      console.warn('数据不存在:', stationName, date)
      return false
    }
    
    localStorage.setItem(STORAGE_KEY, JSON.stringify(filtered))
    
    console.log('[DataManage] 删除数据成功:', stationName, date)
    return true
  } catch (error) {
    console.error('删除数据失败:', error)
    return false
  }
}

/**
 * 批量删除数据
 * @param {Array} conditions - 删除条件数组 [{stationName, date}]
 * @returns {Promise<number>} 删除的数量
 */
export async function deleteCustomDataBatch(conditions) {
  try {
    const data = await getCustomData()
    
    const filtered = data.filter(d => {
      return !conditions.some(c => 
        c.stationName === d.stationName && c.date === d.date
      )
    })
    
    const deletedCount = data.length - filtered.length
    localStorage.setItem(STORAGE_KEY, JSON.stringify(filtered))
    
    console.log('[DataManage] 批量删除成功:', deletedCount)
    return deletedCount
  } catch (error) {
    console.error('批量删除失败:', error)
    return 0
  }
}

/**
 * 清空所有自定义数据
 * @returns {Promise<boolean>} 是否成功
 */
export async function clearCustomData() {
  try {
    localStorage.removeItem(STORAGE_KEY)
    console.log('[DataManage] 清空所有数据')
    return true
  } catch (error) {
    console.error('清空数据失败:', error)
    return false
  }
}

/**
 * 根据站点和日期查询数据
 * @param {string} stationName - 站点名称
 * @param {string} date - 日期
 * @returns {Promise<Object|null>} 数据记录
 */
export async function queryCustomData(stationName, date) {
  try {
    const data = await getCustomData()
    return data.find(d => d.stationName === stationName && d.date === date) || null
  } catch (error) {
    console.error('查询数据失败:', error)
    return null
  }
}

/**
 * 根据站点和日期范围查询数据
 * @param {string} stationName - 站点名称
 * @param {string} startDate - 开始日期
 * @param {string} endDate - 结束日期
 * @returns {Promise<Array>} 数据列表
 */
export async function queryCustomDataByRange(stationName, startDate, endDate) {
  try {
    const data = await getCustomData()
    
    return data.filter(d => {
      if (d.stationName !== stationName) return false
      return d.date >= startDate && d.date <= endDate
    }).sort((a, b) => new Date(a.date) - new Date(b.date))
  } catch (error) {
    console.error('查询数据失败:', error)
    return []
  }
}

/**
 * 根据站点名称获取所有数据
 * @param {string} stationName - 站点名称
 * @returns {Promise<Array>} 数据列表
 */
export async function getCustomDataByStation(stationName) {
  try {
    const data = await getCustomData()
    return data
      .filter(d => d.stationName === stationName)
      .sort((a, b) => new Date(a.date) - new Date(b.date))
  } catch (error) {
    console.error('获取数据失败:', error)
    return []
  }
}

/**
 * 获取数据统计信息
 * @returns {Promise<Object>} 统计信息
 */
export async function getCustomDataStats() {
  try {
    const data = await getCustomData()
    
    const stats = {
      total: data.length,
      byStation: {},
      byLevel: {
        '优': 0,
        '良': 0,
        '轻度污染': 0,
        '中度污染': 0,
        '重度污染': 0,
        '严重污染': 0
      },
      dateRange: { min: null, max: null }
    }
    
    if (data.length > 0) {
      const dates = data.map(d => d.date).sort()
      stats.dateRange.min = dates[0]
      stats.dateRange.max = dates[dates.length - 1]
    }
    
    data.forEach(d => {
      // 按站点统计
      if (!stats.byStation[d.stationName]) {
        stats.byStation[d.stationName] = 0
      }
      stats.byStation[d.stationName]++
      
      // 按等级统计
      if (stats.byLevel[d.qualityLevel] !== undefined) {
        stats.byLevel[d.qualityLevel]++
      }
    })
    
    return stats
  } catch (error) {
    console.error('获取统计信息失败:', error)
    return { total: 0, byStation: {}, byLevel: {}, dateRange: { min: null, max: null } }
  }
}

/**
 * 导出数据为CSV格式
 * @returns {Promise<string>} CSV字符串
 */
export async function exportCustomDataToCSV() {
  try {
    const data = await getCustomData()
    
    if (data.length === 0) {
      return ''
    }
    
    const headers = [
      '站点编码', '站点名称', '日期', 'AQI', '等级',
      'PM2.5', 'PM10', 'O3', 'SO2', 'CO', 'NO2',
      '首要污染物', '沙尘天'
    ]
    
    const rows = data.map(d => [
      d.stationCode,
      d.stationName,
      d.date,
      d.aqi,
      d.qualityLevel,
      d.pm25,
      d.pm10,
      d.o3,
      d.so2,
      d.co,
      d.no2,
      d.primaryPollutant,
      d.isSandDustDay ? 'True' : 'False'
    ])
    
    return [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  } catch (error) {
    console.error('导出数据失败:', error)
    return ''
  }
}

/**
 * 导入CSV数据
 * @param {string} csvText - CSV文本
 * @returns {Promise<{success: number, failed: number, errors: Array}>} 导入结果
 */
export async function importCustomDataFromCSV(csvText) {
  const result = { success: 0, failed: 0, errors: [] }
  
  try {
    const lines = csvText.trim().split('\n')
    if (lines.length < 2) {
      result.errors.push('CSV文件格式错误')
      return result
    }
    
    // 跳过表头
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim()
      if (!line) continue
      
      const values = line.split(',')
      if (values.length < 13) {
        result.failed++
        result.errors.push(`第${i + 1}行数据格式错误`)
        continue
      }
      
      const record = {
        stationCode: values[0]?.trim(),
        stationName: values[1]?.trim(),
        date: values[2]?.trim(),
        aqi: parseInt(values[3]) || 0,
        qualityLevel: values[4]?.trim(),
        pm25: parseFloat(values[5]) || 0,
        pm10: parseFloat(values[6]) || 0,
        o3: parseFloat(values[7]) || 0,
        so2: parseFloat(values[8]) || 0,
        co: parseFloat(values[9]) || 0,
        no2: parseFloat(values[10]) || 0,
        primaryPollutant: values[11]?.trim() || '无',
        isSandDustDay: values[12]?.trim() === 'True'
      }
      
      const success = await addCustomData(record)
      if (success) {
        result.success++
      } else {
        result.failed++
        result.errors.push(`第${i + 1}行数据已存在`)
      }
    }
    
    return result
  } catch (error) {
    console.error('导入数据失败:', error)
    result.errors.push(error.message)
    return result
  }
}

// 将自定义数据合并到查询结果中
export async function mergeWithCustomData(csvData, stationName, date) {
  const customData = await getCustomData()
  const customRecord = customData.find(d => 
    d.stationName === stationName && d.date === date
  )
  
  if (customRecord) {
    // 自定义数据优先级更高
    return customRecord
  }
  
  return csvData.find(d => d.stationName === stationName && d.date === date) || null
}

export async function mergeWithCustomDataRange(csvData, stationName, startDate, endDate) {
  const customData = await getCustomData()
  
  // 获取自定义数据
  const customRecords = customData.filter(d => 
    d.stationName === stationName && 
    d.date >= startDate && 
    d.date <= endDate
  )
  
  // 获取CSV数据（排除自定义数据中已有的日期）
  const customDates = new Set(customRecords.map(d => d.date))
  const csvRecords = csvData.filter(d => 
    d.stationName === stationName && 
    d.date >= startDate && 
    d.date <= endDate &&
    !customDates.has(d.date)
  )
  
  // 合并并排序
  return [...customRecords, ...csvRecords].sort((a, b) => 
    new Date(a.date) - new Date(b.date)
  )
}
