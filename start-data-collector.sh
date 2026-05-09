#!/bin/bash
# ============================================================
# 空气质量数据采集脚本
# 功能：从公开API采集空气质量监测数据并写入MySQL
# 使用方式：./start-data-collector.sh [city]
# 默认城市：郑州
# ============================================================

set -e

CITY=${1:-"郑州"}
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="air_quality_db"
DB_USER="root"
DB_PASS="${DB_PASSWORD:-123456}"
LOG_DIR="./logs"
LOG_FILE="${LOG_DIR}/collector_$(date +%Y%m%d).log"

mkdir -p "$LOG_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

check_dependencies() {
    for cmd in curl mysql jq; do
        if ! command -v $cmd &> /dev/null; then
            log "错误: 缺少依赖 $cmd，请先安装"
            exit 1
        fi
    done
}

collect_data() {
    log "开始采集 ${CITY} 空气质量数据..."

    API_URL="https://api.waqi.info/feed/${CITY}/?token=demo"
    RESPONSE=$(curl -s --connect-timeout 10 --max-time 30 "$API_URL" 2>/dev/null || echo "")

    if [ -z "$RESPONSE" ]; then
        log "警告: API请求失败，尝试备用数据源"
        return 1
    fi

    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null || echo "error")
    if [ "$STATUS" != "ok" ]; then
        log "警告: API返回异常状态: $STATUS"
        return 1
    fi

    AQI=$(echo "$RESPONSE" | jq -r '.data.aqi // 0')
    PM25=$(echo "$RESPONSE" | jq -r '.data.iaqi.pm25.v // 0')
    PM10=$(echo "$RESPONSE" | jq -r '.data.iaqi.pm10.v // 0')
    SO2=$(echo "$RESPONSE" | jq -r '.data.iaqi.so2.v // 0')
    NO2=$(echo "$RESPONSE" | jq -r '.data.iaqi.no2.v // 0')
    CO=$(echo "$RESPONSE" | jq -r '.data.iaqi.co.v // 0')
    O3=$(echo "$RESPONSE" | jq -r '.data.iaqi.o3.v // 0')
    STATION=$(echo "$RESPONSE" | jq -r '.data.city.name // "unknown"')
    UPDATE_TIME=$(echo "$RESPONSE" | jq -r '.data.time.s // ""')

    log "采集成功: AQI=$AQI, PM2.5=$PM25, PM10=$PM10, SO2=$SO2, NO2=$NO2, CO=$CO, O3=$O3"

    INSERT_SQL="INSERT INTO air_quality_data (city, station_code, aqi, pm25, pm10, so2, no2, co, o3, quality_level, monitor_time, create_time) VALUES ('${CITY}', '${STATION}', ${AQI}, ${PM25}, ${PM10}, ${SO2}, ${NO2}, ${CO}, ${O3}, '$(get_quality_level $AQI)', '${UPDATE_TIME}', NOW()) ON DUPLICATE KEY UPDATE aqi=${AQI}, pm25=${PM25}, pm10=${PM10}, update_time=NOW();"

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "$INSERT_SQL" 2>/dev/null

    if [ $? -eq 0 ]; then
        log "数据已写入MySQL"
    else
        log "警告: 数据库写入失败"
        return 1
    fi
}

get_quality_level() {
    local aqi=$1
    if [ "$aqi" -le 50 ]; then echo "优"
    elif [ "$aqi" -le 100 ]; then echo "良"
    elif [ "$aqi" -le 150 ]; then echo "轻度污染"
    elif [ "$aqi" -le 200 ]; then echo "中度污染"
    elif [ "$aqi" -le 300 ]; then echo "重度污染"
    else echo "严重污染"
    fi
}

main() {
    log "========================================="
    log "空气质量数据采集任务启动"
    log "目标城市: ${CITY}"
    log "========================================="

    check_dependencies
    collect_data

    if [ $? -eq 0 ]; then
        log "采集任务执行成功"
    else
        log "采集任务执行失败"
        exit 1
    fi
}

main
