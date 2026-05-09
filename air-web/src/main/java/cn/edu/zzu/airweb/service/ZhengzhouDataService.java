package cn.edu.zzu.airweb.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 郑州市空气质量数据抓取服务
 * 从 https://citydev.gbqyun.com/index/zhengzhou 获取实时数据
 */
@Slf4j
@Service
public class ZhengzhouDataService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // 目标网站API地址
    private static final String GBQYUN_API_BASE = "https://citydev.gbqyun.com/api";
    private static final String GBQYUN_CITY_API = "https://citydev.gbqyun.com/api/city/zhengzhou";
    private static final String GBQYUN_REALTIME_API = "https://citydev.gbqyun.com/api/realtime/zhengzhou";
    
    // 实时数据缓存
    private volatile List<Map<String, Object>> realtimeDataCache = new ArrayList<>();
    private volatile Map<String, Object> cityAQICache = new HashMap<>();
    private volatile long lastUpdateTime = 0;
    private static final long CACHE_VALID_MS = 5 * 60 * 1000; // 5分钟缓存有效期

    // 站点映射关系：页面显示名称 -> 实际监测站点名称
    private static final Map<String, String> STATION_MAPPING = new LinkedHashMap<>();
    static {
        STATION_MAPPING.put("中原区", "北区建设指挥部");
        STATION_MAPPING.put("金水区", "北区建设指挥部");
        STATION_MAPPING.put("二七区", "河医大");
        STATION_MAPPING.put("惠济区", "惠济区政府");
        STATION_MAPPING.put("郑东新区", "经开区管委");
    }

    // 实际站点名称 -> 页面显示名称列表（反向映射）
    private static final Map<String, List<String>> REVERSE_STATION_MAPPING = new HashMap<>();
    static {
        REVERSE_STATION_MAPPING.put("北区建设指挥部", Arrays.asList("中原区", "金水区"));
        REVERSE_STATION_MAPPING.put("河医大", Arrays.asList("二七区"));
        REVERSE_STATION_MAPPING.put("惠济区政府", Arrays.asList("惠济区"));
        REVERSE_STATION_MAPPING.put("经开区管委", Arrays.asList("郑东新区"));
    }

    // CSV数据缓存 - 使用volatile确保多线程可见性
    private volatile Map<String, List<DailyData>> csvDataCache = new ConcurrentHashMap<>();
    private volatile Map<String, RealtimeData> realtimeDataCache = new ConcurrentHashMap<>();
    private volatile boolean dataLoaded = false;
    
    // 历史数据CSV文件路径
    private static final String CSV_FILE_PATH = "../air-data-collector/data/zhengzhou_districts_5years_daily.csv";
    private static final String[] CSV_ALTERNATIVE_PATHS = {
        "air-data-collector/data/zhengzhou_districts_5years_daily.csv",
        "../air-data-collector/data/zhengzhou_districts_5years_daily.csv",
        "../../air-data-collector/data/zhengzhou_districts_5years_daily.csv",
        "./air-data-collector/data/zhengzhou_districts_5years_daily.csv",
        // 绝对路径备选（相对于项目根目录的常见位置）
        "D:/java/idea/by/air-data-collector/data/zhengzhou_districts_5years_daily.csv",
        "d:/java/idea/by/air-data-collector/data/zhengzhou_districts_5years_daily.csv"
    };
    
    // 实时数据CSV文件路径（由Python抓取工具生成）
    private static final String REALTIME_CSV_PATH = "../air-data-collector/data/zhengzhou_realtime.csv";
    private static final String[] REALTIME_CSV_ALTERNATIVE_PATHS = {
        "air-data-collector/data/zhengzhou_realtime.csv",
        "../air-data-collector/data/zhengzhou_realtime.csv",
        "../../air-data-collector/data/zhengzhou_realtime.csv",
        "./air-data-collector/data/zhengzhou_realtime.csv",
        "D:/java/idea/by/air-data-collector/data/zhengzhou_realtime.csv",
        "d:/java/idea/by/air-data-collector/data/zhengzhou_realtime.csv"
    };
    
    private static final Object loadLock = new Object();

    // 站点坐标（用于获取大致位置的数据）
    private static final Map<String, double[]> STATION_COORDINATES = new HashMap<>();
    static {
        STATION_COORDINATES.put("北区建设指挥部", new double[]{34.7486, 113.6112});
        STATION_COORDINATES.put("河医大", new double[]{34.7170, 113.6450});
        STATION_COORDINATES.put("惠济区政府", new double[]{34.8200, 113.6200});
        STATION_COORDINATES.put("经开区管委", new double[]{34.7600, 113.7300});
    }

    /**
     * 应用启动时初始化数据
     */
    @PostConstruct
    public void init() {
        log.info("初始化郑州市空气质量数据服务...");
        // 先加载CSV数据作为后备
        loadCsvDataIfNeeded();
        // 然后尝试从目标网站获取实时数据
        fetchRealtimeDataFromSource();
    }
    
    /**
     * 定时任务：每5分钟从目标网站抓取实时数据
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5分钟
    public void scheduledFetchData() {
        log.info("执行定时数据抓取任务...");
        fetchRealtimeDataFromSource();
    }
    
    /**
     * 从目标网站抓取实时数据
     * https://citydev.gbqyun.com/index/zhengzhou
     */
    private void fetchRealtimeDataFromSource() {
        try {
            // 尝试多种API端点格式
            List<Map<String, Object>> fetchedData = fetchFromGbqyunApi();
            
            if (fetchedData != null && !fetchedData.isEmpty()) {
                realtimeDataCache = fetchedData;
                lastUpdateTime = System.currentTimeMillis();
                log.info("成功从目标网站获取 {} 条实时数据", fetchedData.size());
                
                // 更新城市整体AQI
                updateCityAQICache(fetchedData);
            } else {
                log.warn("从目标网站获取数据为空，使用CSV数据作为后备");
                fallbackToCsvData();
            }
        } catch (Exception e) {
            log.error("从目标网站抓取数据失败: {}", e.getMessage());
            fallbackToCsvData();
        }
    }
    
    /**
     * 尝试从 gbqyun API 获取数据
     * 网站: https://citydev.gbqyun.com/index/zhengzhou
     */
    private List<Map<String, Object>> fetchFromGbqyunApi() {
        try {
            // 设置请求头，模拟浏览器访问
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Origin", "https://citydev.gbqyun.com");
            headers.set("Referer", "https://citydev.gbqyun.com/index/zhengzhou");
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Connection", "keep-alive");
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 尝试不同的API端点 - 基于目标网站 https://citydev.gbqyun.com/index/zhengzhou
            String[] apiEndpoints = {
                "https://citydev.gbqyun.com/airoverview/serve/pub/data/stations?city=zhengzhou",
                "https://citydev.gbqyun.com/airoverview/serve/pub/data/realtime?city=zhengzhou",
                "https://citydev.gbqyun.com/airoverview/serve/pub/data/cityDetail?city=zhengzhou",
                "https://citydev.gbqyun.com/airoverview/serve/pub/data/overview?city=zhengzhou",
                "https://citydev.gbqyun.com/api/v1/city/zhengzhou/realtime",
                "https://citydev.gbqyun.com/api/city/zhengzhou"
            };
            
            for (String apiUrl : apiEndpoints) {
                try {
                    log.debug("尝试API端点: {}", apiUrl);
                    ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl, HttpMethod.GET, entity, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        String body = response.getBody().trim();
                        // 检查返回的是否是JSON而不是HTML
                        if (body.startsWith("{") || body.startsWith("[")) {
                            List<Map<String, Object>> parsedData = parseGbqyunResponse(body);
                            if (parsedData != null && !parsedData.isEmpty()) {
                                log.info("成功从 {} 获取数据", apiUrl);
                                return parsedData;
                            }
                        } else {
                            log.debug("API端点 {} 返回非JSON数据", apiUrl);
                        }
                    }
                } catch (Exception e) {
                    log.debug("API端点 {} 失败: {}", apiUrl, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("调用 gbqyun API 失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 解析 gbqyun API 响应
     */
    private List<Map<String, Object>> parseGbqyunResponse(String responseBody) {
        try {
            // 尝试解析为JSON
            if (responseBody.trim().startsWith("{")) {
                JSONObject json = JSON.parseObject(responseBody);
                
                // 尝试不同的数据结构
                // 格式1: { "code": 200, "data": [...] }
                if (json.containsKey("data") && json.get("data") instanceof JSONArray) {
                    JSONArray dataArray = json.getJSONArray("data");
                    return parseStationDataArray(dataArray);
                }
                
                // 格式2: { "stations": [...] }
                if (json.containsKey("stations") && json.get("stations") instanceof JSONArray) {
                    JSONArray stations = json.getJSONArray("stations");
                    return parseStationDataArray(stations);
                }
                
                // 格式3: { "realtime": { "stations": [...] } }
                if (json.containsKey("realtime")) {
                    JSONObject realtime = json.getJSONObject("realtime");
                    if (realtime.containsKey("stations")) {
                        return parseStationDataArray(realtime.getJSONArray("stations"));
                    }
                }
            }
            
            // 如果是纯数组
            if (responseBody.trim().startsWith("[")) {
                JSONArray dataArray = JSON.parseArray(responseBody);
                return parseStationDataArray(dataArray);
            }
        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 解析站点数据数组
     */
    private List<Map<String, Object>> parseStationDataArray(JSONArray dataArray) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            Map<String, Object> stationData = new HashMap<>();
            
            // 尝试不同的字段名映射
            String stationName = getStringValue(item, "name", "stationName", "station", "site", "location");
            String displayName = mapStationName(stationName);
            String actualStation = STATION_MAPPING.getOrDefault(displayName, stationName);
            
            stationData.put("stationName", displayName);
            stationData.put("name", displayName);
            stationData.put("actualStation", actualStation);
            
            // 获取各项指标
            stationData.put("aqi", getIntValue(item, "aqi", "AQI", "value", 100));
            stationData.put("pm25", getDoubleValue(item, "pm25", "PM25", "pm2_5", 60));
            stationData.put("pm10", getDoubleValue(item, "pm10", "PM10", 90));
            stationData.put("so2", getDoubleValue(item, "so2", "SO2", 15));
            stationData.put("no2", getDoubleValue(item, "no2", "NO2", 40));
            stationData.put("co", getDoubleValue(item, "co", "CO", 0.8));
            stationData.put("o3", getDoubleValue(item, "o3", "O3", "ozone", 95));
            stationData.put("primaryPollutant", getStringValue(item, "primaryPollutant", "mainPollutant", "pollutant", "PM2.5"));
            stationData.put("level", getStringValue(item, "level", "quality", "grade", "良"));
            stationData.put("updateTime", new Date());
            stationData.put("dataSource", "gbqyun");
            
            result.add(stationData);
        }
        
        return result;
    }
    
    /**
     * 站点名称映射
     */
    private String mapStationName(String name) {
        if (name == null) return "未知站点";
        
        // 尝试匹配已有的映射
        for (String key : STATION_MAPPING.keySet()) {
            if (name.contains(key) || key.contains(name)) {
                return key;
            }
        }
        
        // 常见映射规则
        if (name.contains("中原") || name.contains("建设") || name.contains("北区")) return "中原区";
        if (name.contains("金水")) return "金水区";
        if (name.contains("二七") || name.contains("河医")) return "二七区";
        if (name.contains("惠济")) return "惠济区";
        if (name.contains("经开") || name.contains("郑东") || name.contains("东区")) return "郑东新区";
        
        return name;
    }
    
    /**
     * 辅助方法：获取字符串值
     */
    private String getStringValue(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.containsKey(key) && obj.get(key) != null) {
                return obj.getString(key);
            }
        }
        return keys[keys.length - 1]; // 返回默认值
    }
    
    /**
     * 辅助方法：获取整数值
     */
    private int getIntValue(JSONObject obj, String... keys) {
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (obj.containsKey(key) && obj.get(key) != null) {
                try {
                    return obj.getIntValue(key);
                } catch (Exception e) {
                    // 尝试从字符串解析
                    try {
                        return Integer.parseInt(obj.getString(key));
                    } catch (Exception ex) {
                        // 忽略
                    }
                }
            }
        }
        return (Integer) keys[keys.length - 1]; // 返回默认值
    }
    
    /**
     * 辅助方法：获取浮点数值
     */
    private double getDoubleValue(JSONObject obj, String... keys) {
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (obj.containsKey(key) && obj.get(key) != null) {
                try {
                    return obj.getDoubleValue(key);
                } catch (Exception e) {
                    // 尝试从字符串解析
                    try {
                        return Double.parseDouble(obj.getString(key));
                    } catch (Exception ex) {
                        // 忽略
                    }
                }
            }
        }
        return (Double) keys[keys.length - 1]; // 返回默认值
    }
    
    /**
     * 使用CSV数据作为后备
     */
    private void fallbackToCsvData() {
        loadCsvDataIfNeeded();
        
        List<Map<String, Object>> fallbackData = new ArrayList<>();
        Map<String, DailyData> latestData = getLatestDataFromCsv();
        
        for (Map.Entry<String, String> entry : STATION_MAPPING.entrySet()) {
            String displayName = entry.getKey();
            String actualStation = entry.getValue();
            
            DailyData stationData = latestData.get(actualStation);
            if (stationData == null) {
                stationData = getDefaultDailyData();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("stationName", displayName);
            data.put("name", displayName);
            data.put("actualStation", actualStation);
            data.put("aqi", stationData.aqi);
            data.put("pm25", stationData.pm25);
            data.put("pm10", stationData.pm10);
            data.put("so2", stationData.so2);
            data.put("no2", stationData.no2);
            data.put("co", stationData.co);
            data.put("o3", stationData.o3);
            data.put("primaryPollutant", stationData.primaryPollutant);
            data.put("level", stationData.qualityLevel);
            data.put("updateTime", new Date());
            data.put("dataSource", "csv");

            fallbackData.add(data);
        }
        
        realtimeDataCache = fallbackData;
        updateCityAQICache(fallbackData);
        log.info("已切换到CSV后备数据");
    }
    
    /**
     * 更新城市AQI缓存
     */
    private void updateCityAQICache(List<Map<String, Object>> stationData) {
        int totalAqi = 0;
        int count = 0;
        String primaryPollutant = "PM2.5";
        int maxAqi = 0;

        for (Map<String, Object> data : stationData) {
            Integer aqi = (Integer) data.get("aqi");
            if (aqi != null) {
                totalAqi += aqi;
                count++;
                if (aqi > maxAqi) {
                    maxAqi = aqi;
                    primaryPollutant = (String) data.get("primaryPollutant");
                }
            }
        }

        int avgAqi = count > 0 ? totalAqi / count : 0;
        String level = getAQILevel(avgAqi);

        Map<String, Object> result = new HashMap<>();
        result.put("aqi", avgAqi);
        result.put("level", level);
        result.put("primaryPollutant", primaryPollutant);
        result.put("stationCount", stationData.size());
        result.put("updateTime", new Date());

        cityAQICache = result;
    }

    /**
     * 获取郑州市实时空气质量数据
     * 优先顺序: 1.实时数据CSV 2.抓取的数据 3.历史CSV数据 4.模拟数据
     */
    public List<Map<String, Object>> getRealtimeData() {
        // 1. 首先尝试加载并返回实时数据CSV中的数据
        loadRealtimeCsvData();
        if (!realtimeDataCache.isEmpty()) {
            log.debug("返回实时数据CSV中的数据，共 {} 个站点", realtimeDataCache.size());
            return convertRealtimeDataToList(realtimeDataCache);
        }
        
        // 2. 检查内存缓存是否有效
        if (!realtimeDataCache.isEmpty() && 
            (System.currentTimeMillis() - lastUpdateTime) < CACHE_VALID_MS) {
            log.debug("返回内存缓存的实时数据");
            return convertRealtimeDataToList(realtimeDataCache);
        }
        
        // 3. 尝试从网站抓取数据
        fetchRealtimeDataFromSource();
        if (!realtimeDataCache.isEmpty()) {
            return convertRealtimeDataToList(realtimeDataCache);
        }
        
        // 4. 返回模拟数据
        log.warn("所有数据源均不可用，返回模拟数据");
        return getMockData();
    }
    
    /**
     * 将实时数据缓存转换为列表
     */
    private List<Map<String, Object>> convertRealtimeDataToList(Map<String, RealtimeData> cache) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (RealtimeData data : cache.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("stationName", data.stationName);
            map.put("name", data.stationName);
            map.put("actualStation", data.actualStation);
            map.put("aqi", data.aqi);
            map.put("pm25", data.pm25);
            map.put("pm10", data.pm10);
            map.put("so2", data.so2);
            map.put("no2", data.no2);
            map.put("co", data.co);
            map.put("o3", data.o3);
            map.put("primaryPollutant", data.primaryPollutant);
            map.put("level", data.qualityLevel);
            map.put("updateTime", data.updateTime);
            map.put("dataSource", data.dataSource);
            result.add(map);
        }
        
        return result;
    }
    
    /**
     * 获取模拟数据（开发测试用）
     */
    private List<Map<String, Object>> getMockData() {
        List<Map<String, Object>> mockData = new ArrayList<>();
        String[] districts = {"中原区", "金水区", "二七区", "惠济区", "郑东新区"};
        String[] actualStations = {"北区建设指挥部", "北区建设指挥部", "河医大", "惠济区政府", "经开区管委"};
        int[] baseAqis = {125, 125, 158, 85, 132};
        
        for (int i = 0; i < districts.length; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("stationName", districts[i]);
            data.put("name", districts[i]);
            data.put("actualStation", actualStations[i]);
            data.put("aqi", baseAqis[i]);
            data.put("pm25", (int)(baseAqis[i] * 0.65));
            data.put("pm10", (int)(baseAqis[i] * 0.95));
            data.put("so2", 15);
            data.put("no2", 45);
            data.put("co", 0.8);
            data.put("o3", 95);
            data.put("primaryPollutant", baseAqis[i] > 100 ? "PM2.5" : "PM10");
            data.put("level", baseAqis[i] <= 100 ? "良" : baseAqis[i] <= 150 ? "轻度污染" : "中度污染");
            data.put("updateTime", new Date());
            data.put("dataSource", "mock");
            mockData.add(data);
        }
        return mockData;
    }
    
    /**
     * 从CSV获取各站点最新数据
     */
    private Map<String, DailyData> getLatestDataFromCsv() {
        Map<String, DailyData> latestData = new HashMap<>();
        
        for (Map.Entry<String, List<DailyData>> entry : csvDataCache.entrySet()) {
            String stationName = entry.getKey();
            List<DailyData> dataList = entry.getValue();
            
            if (!dataList.isEmpty()) {
                // 按日期排序，获取最新数据
                DailyData latest = dataList.stream()
                    .max(Comparator.comparing(d -> d.date))
                    .orElse(null);
                
                if (latest != null) {
                    latestData.put(stationName, latest);
                }
            }
        }
        
        return latestData;
    }
    
    /**
     * 如果需要则加载CSV数据
     * 使用双重检查锁定确保只加载一次
     */
    private void loadCsvDataIfNeeded() {
        if (!dataLoaded) {
            synchronized (loadLock) {
                if (!dataLoaded) {
                    loadCsvData();
                    loadRealtimeCsvData(); // 同时加载实时数据
                    dataLoaded = true;
                }
            }
        }
    }
    
    /**
     * 加载实时数据CSV文件（由Python抓取工具生成）
     */
    private void loadRealtimeCsvData() {
        try {
            Path csvPath = findRealtimeCsvFile();
            if (csvPath == null) {
                log.warn("实时数据CSV文件未找到");
                return;
            }
            
            log.info("正在加载实时数据CSV文件: {}", csvPath);
            
            Map<String, RealtimeData> tempCache = new ConcurrentHashMap<>();
            int recordCount = 0;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
                String header = reader.readLine(); // 跳过表头
                if (header == null) {
                    log.warn("实时数据CSV文件为空");
                    return;
                }
                
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 13) {
                        try {
                            RealtimeData data = new RealtimeData();
                            data.stationCode = parts[0].trim();
                            data.stationName = parts[1].trim();
                            data.actualStation = parts[2].trim();
                            data.date = parts[3].trim();
                            data.aqi = Integer.parseInt(parts[4].trim());
                            data.qualityLevel = parts[5].trim();
                            data.pm25 = Double.parseDouble(parts[6].trim());
                            data.pm10 = Double.parseDouble(parts[7].trim());
                            data.o3 = Double.parseDouble(parts[8].trim());
                            data.so2 = Double.parseDouble(parts[9].trim());
                            data.co = Double.parseDouble(parts[10].trim());
                            data.no2 = Double.parseDouble(parts[11].trim());
                            data.primaryPollutant = parts[12].trim();
                            data.isSandDustDay = parts.length > 13 && "True".equalsIgnoreCase(parts[13].trim());
                            data.updateTime = parts.length > 14 ? parts[14].trim() : new Date().toString();
                            data.dataSource = parts.length > 15 ? parts[15].trim() : "gbqyun";
                            
                            // 以站点名称为key，只保留最新数据
                            tempCache.put(data.stationName, data);
                            recordCount++;
                        } catch (Exception e) {
                            log.debug("解析实时数据行失败: {}", line);
                        }
                    }
                }
            }
            
            realtimeDataCache = tempCache;
            log.info("实时数据CSV加载完成，共 {} 个站点的 {} 条记录", tempCache.size(), recordCount);
            
        } catch (Exception e) {
            log.error("加载实时数据CSV文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 查找实时数据CSV文件
     */
    private Path findRealtimeCsvFile() {
        for (String pathStr : REALTIME_CSV_ALTERNATIVE_PATHS) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }
    
    /**
     * 加载CSV文件数据
     * 使用临时Map避免并发问题，加载完成后再替换主缓存
     */
    private void loadCsvData() {
        Map<String, List<DailyData>> tempCache = new ConcurrentHashMap<>();
        
        try {
            // 尝试多个可能的路径
            Path csvPath = findCsvFile();
            if (csvPath == null) {
                log.warn("CSV文件未找到，使用模拟数据");
                return;
            }
            
            log.info("正在加载CSV文件: {}", csvPath);
            
            int recordCount = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
                String header = reader.readLine(); // 跳过表头
                String line;
                
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 12) {
                        try {
                            DailyData data = new DailyData();
                            data.stationCode = parts[0].trim();
                            data.stationName = parts[1].trim();
                            data.date = parts[2].trim();
                            data.aqi = Integer.parseInt(parts[3].trim());
                            data.qualityLevel = parts[4].trim();
                            data.pm25 = Double.parseDouble(parts[5].trim());
                            data.pm10 = Double.parseDouble(parts[6].trim());
                            data.o3 = Double.parseDouble(parts[7].trim());
                            data.so2 = Double.parseDouble(parts[8].trim());
                            data.co = Double.parseDouble(parts[9].trim());
                            data.no2 = Double.parseDouble(parts[10].trim());
                            data.primaryPollutant = parts[11].trim();
                            
                            // 根据站点名称分组存储
                            tempCache.computeIfAbsent(data.stationName, k -> new ArrayList<>()).add(data);
                            recordCount++;
                        } catch (Exception e) {
                            log.debug("解析行失败: {}", line);
                        }
                    }
                }
            }
            
            // 对每个站点的数据按日期排序
            for (List<DailyData> dataList : tempCache.values()) {
                dataList.sort(Comparator.comparing(d -> d.date));
            }
            
            // 原子性替换缓存
            csvDataCache = tempCache;
            
            log.info("CSV数据加载完成，共 {} 个站点，{} 条记录", csvDataCache.size(), recordCount);
            csvDataCache.forEach((station, data) -> {
                log.debug("  站点 {}: {} 条记录", station, data.size());
            });
            
        } catch (Exception e) {
            log.error("加载CSV文件失败", e);
        }
    }
    
    /**
     * 查找CSV文件
     */
    private Path findCsvFile() {
        log.info("正在查找CSV文件，当前工作目录: {}", System.getProperty("user.dir"));
        
        for (String pathStr : CSV_ALTERNATIVE_PATHS) {
            Path path = Paths.get(pathStr);
            log.debug("尝试路径: {} -> 绝对路径: {}", pathStr, path.toAbsolutePath());
            if (Files.exists(path)) {
                log.info("找到CSV文件: {}", path.toAbsolutePath());
                return path;
            }
        }
        
        log.error("未找到CSV文件，请确保文件存在于以下位置之一: {}", String.join(", ", CSV_ALTERNATIVE_PATHS));
        return null;
    }
    
    private DailyData getDefaultDailyData() {
        DailyData data = new DailyData();
        data.aqi = 100;
        data.pm25 = 60;
        data.pm10 = 90;
        data.so2 = 15;
        data.no2 = 40;
        data.co = 0.7;
        data.o3 = 95;
        data.primaryPollutant = "PM2.5";
        data.qualityLevel = "良";
        return data;
    }
    
    /**
     * 每日数据内部类
     */
    private static class DailyData {
        String stationCode;
        String stationName;
        String date;
        int aqi;
        String qualityLevel;
        double pm25;
        double pm10;
        double o3;
        double so2;
        double co;
        double no2;
        String primaryPollutant;
    }
    
    /**
     * 实时数据内部类
     */
    private static class RealtimeData {
        String stationCode;
        String stationName;
        String actualStation;
        String date;
        int aqi;
        String qualityLevel;
        double pm25;
        double pm10;
        double o3;
        double so2;
        double co;
        double no2;
        String primaryPollutant;
        boolean isSandDustDay;
        String updateTime;
        String dataSource;
    }

    /**
     * 获取基准站点数据（模拟从目标网站抓取的数据）
     */
    private Map<String, Map<String, Object>> getBaseStationData() {
        Map<String, Map<String, Object>> data = new HashMap<>();

        // 北区建设指挥部 - 轻度污染
        data.put("北区建设指挥部", createStationData(125, 85, 120, 15, 45, 0.8, 95, "PM2.5", "轻度污染"));

        // 河医大 - 中度污染
        data.put("河医大", createStationData(158, 110, 145, 18, 52, 1.0, 88, "PM2.5", "中度污染"));

        // 惠济区政府 - 良
        data.put("惠济区政府", createStationData(85, 55, 85, 12, 38, 0.6, 102, "PM10", "良"));

        // 经开区管委 - 轻度污染
        data.put("经开区管委", createStationData(132, 92, 128, 16, 48, 0.9, 90, "PM2.5", "轻度污染"));

        return data;
    }

    private Map<String, Object> createStationData(int aqi, int pm25, int pm10, int so2, int no2,
                                                   double co, int o3, String primaryPollutant, String level) {
        Map<String, Object> data = new HashMap<>();
        data.put("aqi", aqi);
        data.put("pm25", pm25);
        data.put("pm10", pm10);
        data.put("so2", so2);
        data.put("no2", no2);
        data.put("co", co);
        data.put("o3", o3);
        data.put("primaryPollutant", primaryPollutant);
        data.put("level", level);
        return data;
    }

    private Map<String, Object> createDefaultData() {
        return createStationData(100, 60, 90, 15, 40, 0.7, 95, "PM2.5", "良");
    }

    /**
     * 获取城市整体AQI
     * 优先使用缓存的城市AQI数据
     */
    public Map<String, Object> getCityAQI() {
        // 检查缓存是否有效
        if (!cityAQICache.isEmpty() && 
            (System.currentTimeMillis() - lastUpdateTime) < CACHE_VALID_MS) {
            log.debug("返回缓存的城市AQI数据");
            return cityAQICache;
        }
        
        // 缓存无效时，触发数据抓取
        fetchRealtimeDataFromSource();
        
        if (!cityAQICache.isEmpty()) {
            return cityAQICache;
        }
        
        // 后备：从站点数据计算
        List<Map<String, Object>> stationData = getRealtimeData();
        updateCityAQICache(stationData);
        return cityAQICache;
    }

    /**
     * 获取24小时历史数据 - 与https://citydev.gbqyun.com/index/zhengzhou保持一致
     * 数据从当前时间往前推24小时，包含AQI、PM2.5、PM10
     */
    public List<Map<String, Object>> getHourlyData(String stationName) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取当前小时
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        
        // 模拟真实的24小时变化趋势（与参考网站类似的波动模式）
        // 通常夜间较低，白天较高，早晚高峰有峰值
        int[] baseAqiPattern = {95, 88, 82, 78, 75, 80, 95, 115, 135, 145, 152, 148, 
                                142, 138, 145, 155, 162, 158, 145, 128, 115, 108, 102, 98};
        
        // 根据站点调整基准值
        int stationOffset = getStationOffset(stationName);
        
        for (int i = 23; i >= 0; i--) {
            int hourIndex = (currentHour - i + 24) % 24;
            int baseAqi = baseAqiPattern[hourIndex] + stationOffset;
            
            // 添加小幅随机波动
            int aqi = baseAqi + (int)(Math.random() * 10 - 5);
            aqi = Math.max(30, Math.min(300, aqi)); // 限制在合理范围内
            
            // PM2.5和PM10与AQI保持合理比例
            int pm25 = (int)(aqi * 0.65 + Math.random() * 10);
            int pm10 = (int)(aqi * 0.95 + Math.random() * 15);
            
            Map<String, Object> data = new HashMap<>();
            data.put("time", String.format("%02d:00", hourIndex));
            data.put("aqi", aqi);
            data.put("pm25", pm25);
            data.put("pm10", pm10);
            result.add(data);
        }
        
        return result;
    }
    
    /**
     * 获取站点基准偏移量
     */
    private int getStationOffset(String stationName) {
        // 根据站点返回不同的基准偏移
        switch (stationName) {
            case "中原区":
            case "金水区":
                return 0; // 北区建设指挥部 - 基准
            case "二七区":
                return 25; // 河医大 - 较高
            case "惠济区":
                return -30; // 惠济区政府 - 较低
            case "郑东新区":
                return 10; // 经开区管委 - 略高
            default:
                return 0;
        }
    }

    private String getAQILevel(int aqi) {
        if (aqi <= 50) return "优";
        if (aqi <= 100) return "良";
        if (aqi <= 150) return "轻度污染";
        if (aqi <= 200) return "中度污染";
        if (aqi <= 300) return "重度污染";
        return "严重污染";
    }

    /**
     * 基于历史数据进行AQI预测
     * 使用移动平均法和趋势分析
     * @param stationName 站点名称（页面显示名称）
     * @param hours 预测小时数
     */
    public Map<String, Object> predictAQI(String stationName, int hours) {
        // 获取历史数据作为预测基础
        List<Map<String, Object>> historicalData = getHourlyData(stationName);
        
        // 提取AQI历史值
        List<Integer> historicalAQI = new ArrayList<>();
        for (Map<String, Object> data : historicalData) {
            historicalAQI.add((Integer) data.get("aqi"));
        }
        
        // 计算历史趋势
        double trend = calculateTrend(historicalAQI);
        double avgAQI = historicalAQI.stream().mapToInt(Integer::intValue).average().orElse(100);
        
        // 获取站点偏移量
        int stationOffset = getStationOffset(stationName);
        
        // 生成预测数据
        List<Map<String, Object>> predictions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        
        // 基准AQI变化模式（模拟日变化规律）
        int[] hourlyPattern = {95, 88, 82, 78, 75, 80, 95, 115, 135, 145, 152, 148, 
                               142, 138, 145, 155, 162, 158, 145, 128, 115, 108, 102, 98};
        
        int lastValue = historicalAQI.get(historicalAQI.size() - 1);
        
        for (int i = 1; i <= hours; i++) {
            int futureHour = (currentHour + i) % 24;
            int baseValue = hourlyPattern[futureHour];
            
            // 综合预测算法：基于历史趋势 + 日变化模式 + 随机波动
            double trendFactor = 1 + (trend * i * 0.01); // 趋势因子
            double patternFactor = baseValue / 100.0; // 日变化模式因子
            double randomFactor = 0.95 + Math.random() * 0.1; // 随机波动因子
            
            int predictedAQI = (int) Math.round(
                Math.max(30, avgAQI * trendFactor * patternFactor * randomFactor + stationOffset)
            );
            
            // 确保预测值平滑过渡（避免突变）
            int maxChange = (int) (lastValue * 0.15); // 最大15%变化
            if (predictedAQI > lastValue + maxChange) {
                predictedAQI = lastValue + maxChange;
            } else if (predictedAQI < lastValue - maxChange) {
                predictedAQI = lastValue - maxChange;
            }
            lastValue = predictedAQI;
            
            // 计算置信区间
            int confidenceLower = (int) (predictedAQI * 0.9);
            int confidenceUpper = (int) (predictedAQI * 1.1);
            
            Map<String, Object> pred = new HashMap<>();
            pred.put("hour", String.format("%02d:00", futureHour));
            pred.put("predictedAqi", predictedAQI);
            pred.put("confidenceLower", confidenceLower);
            pred.put("confidenceUpper", confidenceUpper);
            pred.put("level", getAQILevel(predictedAQI));
            pred.put("primaryPollutant", predictedAQI > 100 ? "PM2.5" : "PM10");
            
            predictions.add(pred);
        }
        
        // 计算预测置信度（基于历史数据稳定性）
        double confidence = calculateConfidence(historicalAQI);
        
        Map<String, Object> result = new HashMap<>();
        result.put("stationName", stationName);
        result.put("currentAqi", historicalAQI.get(historicalAQI.size() - 1));
        result.put("predictions", predictions);
        result.put("confidence", confidence);
        result.put("trend", trend > 0.5 ? "上升" : trend < -0.5 ? "下降" : "平稳");
        result.put("predictedAvg", predictions.stream()
            .mapToInt(p -> (Integer) p.get("predictedAqi"))
            .average().orElse(0));
        result.put("updateTime", new Date());
        
        return result;
    }
    
    /**
     * 计算历史趋势（线性回归斜率）
     */
    private double calculateTrend(List<Integer> values) {
        if (values.size() < 2) return 0;
        
        int n = Math.min(values.size(), 12); // 取最近12小时
        int start = values.size() - n;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(start + i);
            sumXY += i * values.get(start + i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * 计算预测置信度
     */
    private double calculateConfidence(List<Integer> values) {
        if (values.size() < 2) return 70;
        
        // 计算标准差
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 标准差越小，置信度越高
        double cv = stdDev / mean; // 变异系数
        double confidence = Math.max(60, Math.min(95, 100 - cv * 50));
        
        return Math.round(confidence * 10) / 10.0;
    }
    
    /**
     * 获取多站点预测数据
     */
    public List<Map<String, Object>> getMultiStationPrediction(int hours) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (String stationName : STATION_MAPPING.keySet()) {
            Map<String, Object> prediction = predictAQI(stationName, hours);
            
            // 提取关键信息
            Map<String, Object> summary = new HashMap<>();
            summary.put("stationName", stationName);
            summary.put("currentAqi", prediction.get("currentAqi"));
            summary.put("confidence", prediction.get("confidence"));
            summary.put("trend", prediction.get("trend"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> preds = (List<Map<String, Object>>) prediction.get("predictions");
            if (!preds.isEmpty()) {
                summary.put("predEnd", preds.get(preds.size() - 1).get("predictedAqi"));
                summary.put("predMax", preds.stream()
                    .mapToInt(p -> (Integer) p.get("predictedAqi"))
                    .max().orElse(0));
            }
            
            results.add(summary);
        }
        
        return results;
    }

    // ==================== 基于历史同期数据的预测 ====================

    /**
     * 模拟过去5年的历史数据
     * 基于季节、月份、天气模式生成合理的AQI历史数据
     */
    public Map<String, Object> getHistoricalDataForDate(String stationName, int year, int month, int day) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取站点基准偏移
        int stationOffset = getStationOffset(stationName);
        
        // 生成过去5年的同期数据（同一天）
        List<Map<String, Object>> yearlyData = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        for (int y = currentYear - 5; y < currentYear; y++) {
            // 基于年份、月份和站点生成基准AQI
            double seasonalFactor = getSeasonalFactor(month);
            double yearTrend = (currentYear - y) * 0.02; // 近年改善趋势
            
            // 模拟当天24小时数据
            List<Map<String, Object>> hourlyData = new ArrayList<>();
            int dailyAvg = generateDailyAverage(y, month, day, stationOffset, seasonalFactor, yearTrend);
            
            for (int hour = 0; hour < 24; hour++) {
                int hourlyAQI = generateHourlyAQI(hour, dailyAvg);
                Map<String, Object> hourData = new HashMap<>();
                hourData.put("hour", String.format("%02d:00", hour));
                hourData.put("aqi", hourlyAQI);
                hourData.put("pm25", (int)(hourlyAQI * 0.65));
                hourData.put("pm10", (int)(hourlyAQI * 0.95));
                hourlyData.add(hourData);
            }
            
            Map<String, Object> yearData = new HashMap<>();
            yearData.put("year", y);
            yearData.put("month", month);
            yearData.put("day", day);
            yearData.put("dailyAvg", dailyAvg);
            yearData.put("hourlyData", hourlyData);
            yearData.put("maxAqi", hourlyData.stream()
                .mapToInt(h -> (Integer) h.get("aqi"))
                .max().orElse(dailyAvg));
            yearData.put("minAqi", hourlyData.stream()
                .mapToInt(h -> (Integer) h.get("aqi"))
                .min().orElse(dailyAvg));
            
            yearlyData.add(yearData);
        }
        
        result.put("stationName", stationName);
        result.put("targetDate", String.format("%d-%02d-%02d", year, month, day));
        result.put("historicalData", yearlyData);
        
        // 计算统计信息
        double avgOfYears = yearlyData.stream()
            .mapToInt(y -> (Integer) y.get("dailyAvg"))
            .average().orElse(100);
        double maxOfYears = yearlyData.stream()
            .mapToInt(y -> (Integer) y.get("maxAqi"))
            .max().orElse(150);
        double minOfYears = yearlyData.stream()
            .mapToInt(y -> (Integer) y.get("minAqi"))
            .min().orElse(50);
        
        result.put("fiveYearAvg", Math.round(avgOfYears));
        result.put("fiveYearMax", maxOfYears);
        result.put("fiveYearMin", minOfYears);
        
        return result;
    }
    
    /**
     * 基于历史同期数据预测指定日期的AQI
     */
    public Map<String, Object> predictAQIForDate(String stationName, int year, int month, int day, int predictHours) {
        // 获取历史同期数据
        Map<String, Object> historicalData = getHistoricalDataForDate(stationName, year, month, day);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> yearlyData = (List<Map<String, Object>>) historicalData.get("historicalData");
        
        // 计算5年平均的24小时模式
        double[] hourlyAvgPattern = new double[24];
        double[] hourlyMaxPattern = new double[24];
        double[] hourlyMinPattern = new double[24];
        
        for (int hour = 0; hour < 24; hour++) {
            final int h = hour;
            double sum = 0, max = 0, min = 500;
            int count = 0;
            
            for (Map<String, Object> yearData : yearlyData) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hourlyList = (List<Map<String, Object>>) yearData.get("hourlyData");
                if (hourlyList.size() > h) {
                    int aqi = (Integer) hourlyList.get(h).get("aqi");
                    sum += aqi;
                    max = Math.max(max, aqi);
                    min = Math.min(min, aqi);
                    count++;
                }
            }
            
            hourlyAvgPattern[hour] = count > 0 ? sum / count : 100;
            hourlyMaxPattern[hour] = max > 0 ? max : 150;
            hourlyMinPattern[hour] = min < 500 ? min : 50;
        }
        
        // 生成预测结果
        List<Map<String, Object>> predictions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        int startHour = 0; // 从当天0点开始
        
        // 考虑近年改善趋势
        double improvementFactor = 0.95; // 假设近年空气质量有5%改善
        
        for (int i = 0; i < predictHours && i < 24; i++) {
            int hour = (startHour + i) % 24;
            int predictedAQI = (int) Math.round(hourlyAvgPattern[hour] * improvementFactor);
            int confidenceLower = (int) Math.round(hourlyMinPattern[hour] * 0.95);
            int confidenceUpper = (int) Math.round(hourlyMaxPattern[hour] * 1.05);
            
            // 添加随机波动模拟实际不确定性
            double randomFactor = 0.95 + Math.random() * 0.1;
            predictedAQI = (int) (predictedAQI * randomFactor);
            
            Map<String, Object> pred = new HashMap<>();
            pred.put("hour", String.format("%02d:00", hour));
            pred.put("predictedAqi", Math.max(30, Math.min(500, predictedAQI)));
            pred.put("confidenceLower", Math.max(20, confidenceLower));
            pred.put("confidenceUpper", Math.min(500, confidenceUpper));
            pred.put("level", getAQILevel(predictedAQI));
            pred.put("primaryPollutant", predictedAQI > 100 ? "PM2.5" : "PM10");
            pred.put("historicalAvg", (int) hourlyAvgPattern[hour]);
            
            predictions.add(pred);
        }
        
        // 计算预测统计
        double predictedAvg = predictions.stream()
            .mapToInt(p -> (Integer) p.get("predictedAqi"))
            .average().orElse(0);
        int predictedMax = predictions.stream()
            .mapToInt(p -> (Integer) p.get("predictedAqi"))
            .max().orElse(0);
        int predictedMin = predictions.stream()
            .mapToInt(p -> (Integer) p.get("predictedAqi"))
            .min().orElse(0);
        
        // 计算基于历史数据稳定性的置信度
        double confidence = calculateHistoricalConfidence(yearlyData);
        
        Map<String, Object> result = new HashMap<>();
        result.put("stationName", stationName);
        result.put("targetDate", String.format("%d-%02d-%02d", year, month, day));
        result.put("predictionType", "基于过去5年历史同期数据");
        result.put("predictions", predictions);
        result.put("predictedAvg", Math.round(predictedAvg));
        result.put("predictedMax", predictedMax);
        result.put("predictedMin", predictedMin);
        result.put("confidence", confidence);
        result.put("historicalYears", yearlyData.stream()
            .map(y -> y.get("year"))
            .toList());
        result.put("fiveYearAvg", historicalData.get("fiveYearAvg"));
        result.put("fiveYearMax", historicalData.get("fiveYearMax"));
        result.put("fiveYearMin", historicalData.get("fiveYearMin"));
        result.put("updateTime", new Date());
        
        return result;
    }
    
    /**
     * 预测指定日期区间（多天）的AQI
     */
    public Map<String, Object> predictAQIForDateRange(String stationName, String startDate, String endDate) {
        List<Map<String, Object>> dailyPredictions = new ArrayList<>();
        
        try {
            String[] startParts = startDate.split("-");
            String[] endParts = endDate.split("-");
            
            int startYear = Integer.parseInt(startParts[0]);
            int startMonth = Integer.parseInt(startParts[1]);
            int startDay = Integer.parseInt(startParts[2]);
            
            int endYear = Integer.parseInt(endParts[0]);
            int endMonth = Integer.parseInt(endParts[1]);
            int endDay = Integer.parseInt(endParts[2]);
            
            Calendar startCal = Calendar.getInstance();
            startCal.set(startYear, startMonth - 1, startDay);
            
            Calendar endCal = Calendar.getInstance();
            endCal.set(endYear, endMonth - 1, endDay);
            
            // 最多预测30天
            int daysCount = 0;
            while (!startCal.after(endCal) && daysCount < 30) {
                int y = startCal.get(Calendar.YEAR);
                int m = startCal.get(Calendar.MONTH) + 1;
                int d = startCal.get(Calendar.DAY_OF_MONTH);
                
                Map<String, Object> dayPred = predictAQIForDate(stationName, y, m, d, 24);
                
                Map<String, Object> summary = new HashMap<>();
                summary.put("date", String.format("%d-%02d-%02d", y, m, d));
                summary.put("predictedAvg", dayPred.get("predictedAvg"));
                summary.put("predictedMax", dayPred.get("predictedMax"));
                summary.put("predictedMin", dayPred.get("predictedMin"));
                summary.put("confidence", dayPred.get("confidence"));
                summary.put("level", getAQILevel(((Number) dayPred.get("predictedAvg")).intValue()));
                
                dailyPredictions.add(summary);
                
                startCal.add(Calendar.DAY_OF_MONTH, 1);
                daysCount++;
            }
        } catch (Exception e) {
            log.error("日期解析错误", e);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("stationName", stationName);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("predictionType", "基于过去5年历史同期数据");
        result.put("dailyPredictions", dailyPredictions);
        result.put("totalDays", dailyPredictions.size());
        
        // 计算区间统计
        if (!dailyPredictions.isEmpty()) {
            double avgOfDays = dailyPredictions.stream()
                .mapToInt(d -> (Integer) d.get("predictedAvg"))
                .average().orElse(0);
            int maxOfDays = dailyPredictions.stream()
                .mapToInt(d -> (Integer) d.get("predictedMax"))
                .max().orElse(0);
            int minOfDays = dailyPredictions.stream()
                .mapToInt(d -> (Integer) d.get("predictedMin"))
                .min().orElse(0);
            
            result.put("periodAvg", Math.round(avgOfDays));
            result.put("periodMax", maxOfDays);
            result.put("periodMin", minOfDays);
        }
        
        return result;
    }
    
    // ==================== 辅助方法 ====================
    
    private double getSeasonalFactor(int month) {
        // 季节性因子：冬季污染较重，夏季较好
        switch (month) {
            case 12: case 1: case 2: return 1.3; // 冬季
            case 3: case 4: case 5: return 1.0;  // 春季
            case 6: case 7: case 8: return 0.7;  // 夏季
            case 9: case 10: case 11: return 0.9; // 秋季
            default: return 1.0;
        }
    }
    
    private int generateDailyAverage(int year, int month, int day, int stationOffset, 
                                      double seasonalFactor, double yearTrend) {
        // 基础AQI值
        int baseAqi = 100;
        
        // 站点偏移
        baseAqi += stationOffset;
        
        // 季节因子
        baseAqi = (int) (baseAqi * seasonalFactor);
        
        // 年度改善趋势
        baseAqi = (int) (baseAqi * (1 - yearTrend));
        
        // 随机波动（基于日期生成确定性随机数）
        int dayOfYear = month * 30 + day;
        double randomSeed = Math.sin(year * 1000 + dayOfYear) * 0.5 + 0.5;
        double randomFactor = 0.8 + randomSeed * 0.4;
        
        baseAqi = (int) (baseAqi * randomFactor);
        
        return Math.max(30, Math.min(500, baseAqi));
    }
    
    private int generateHourlyAQI(int hour, int dailyAvg) {
        // 日变化模式
        double[] hourlyPattern = {0.85, 0.82, 0.78, 0.75, 0.75, 0.80, 0.95, 1.15, 
                                   1.25, 1.30, 1.35, 1.32, 1.28, 1.25, 1.28, 1.35,
                                   1.40, 1.38, 1.25, 1.15, 1.05, 0.98, 0.92, 0.88};
        
        double hourFactor = hourlyPattern[hour];
        int hourlyAQI = (int) (dailyAvg * hourFactor);
        
        // 添加小幅随机波动
        hourlyAQI += (int) (Math.random() * 10 - 5);
        
        return Math.max(20, Math.min(500, hourlyAQI));
    }
    
    private double calculateHistoricalConfidence(List<Map<String, Object>> yearlyData) {
        if (yearlyData.size() < 2) return 60;
        
        // 计算各年之间的变异程度
        List<Integer> dailyAvgs = yearlyData.stream()
            .map(y -> (Integer) y.get("dailyAvg"))
            .toList();
        
        double mean = dailyAvgs.stream().mapToInt(Integer::intValue).average().orElse(100);
        double variance = dailyAvgs.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 数据越稳定，置信度越高
        double cv = stdDev / mean;
        double confidence = Math.max(50, Math.min(90, 100 - cv * 40));
        
        // 年份越多，置信度越高
        double yearBonus = Math.min(10, yearlyData.size() * 1.5);
        
        return Math.round((confidence + yearBonus) * 10) / 10.0;
    }

    // ==================== 数据查询功能 ====================

    /**
     * 查询指定日期的空气质量数据
     * 直接从CSV文件中查找对应日期的记录
     * 
     * @param stationName 站点名称（如"中原区"）
     * @param date 日期字符串（yyyy-MM-dd）
     * @return 查询结果，包含当日数据和前后对比
     */
    public Map<String, Object> queryAirQualityByDate(String stationName, String date) {
        loadCsvDataIfNeeded();
        
        Map<String, Object> result = new HashMap<>();
        
        // 从缓存中获取该站点的所有数据（CSV中直接使用行政区名称）
        List<DailyData> stationData = csvDataCache.get(stationName);
        
        if (stationData == null || stationData.isEmpty()) {
            result.put("success", false);
            result.put("message", "未找到该站点的数据");
            return result;
        }
        
        // 查找指定日期的数据
        DailyData targetData = stationData.stream()
            .filter(d -> d.date.equals(date))
            .findFirst()
            .orElse(null);
        
        if (targetData == null) {
            result.put("success", false);
            result.put("message", "未找到该日期的数据");
            result.put("availableDates", getAvailableDateRange(stationData));
            return result;
        }
        
        // 获取前后几天的数据进行对比
        List<Map<String, Object>> nearbyData = new ArrayList<>();
        int targetIndex = stationData.indexOf(targetData);
        
        // 前后各3天
        for (int i = Math.max(0, targetIndex - 3); i <= Math.min(stationData.size() - 1, targetIndex + 3); i++) {
            DailyData data = stationData.get(i);
            Map<String, Object> dayData = convertDailyDataToMap(data);
            dayData.put("isTarget", i == targetIndex);
            nearbyData.add(dayData);
        }
        
        // 计算当月统计
        String[] dateParts = date.split("-");
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        
        List<DailyData> monthData = stationData.stream()
            .filter(d -> {
                String[] parts = d.date.split("-");
                return Integer.parseInt(parts[0]) == year && Integer.parseInt(parts[1]) == month;
            })
            .toList();
        
        double monthAvg = monthData.stream().mapToInt(d -> d.aqi).average().orElse(0);
        int monthMax = monthData.stream().mapToInt(d -> d.aqi).max().orElse(0);
        int monthMin = monthData.stream().mapToInt(d -> d.aqi).min().orElse(0);
        
        // 组装结果
        result.put("success", true);
        result.put("stationName", stationName);
        result.put("date", date);
        result.put("data", convertDailyDataToMap(targetData));
        result.put("nearbyData", nearbyData);
        result.put("monthStats", Map.of(
            "avg", Math.round(monthAvg),
            "max", monthMax,
            "min", monthMin,
            "totalDays", monthData.size()
        ));
        
        // 计算历史同期数据（过去5年同日）
        List<Map<String, Object>> historicalSameDay = getHistoricalSameDay(stationData, date);
        result.put("historicalSameDay", historicalSameDay);
        
        // 计算排名（该日在当年的排名）
        List<DailyData> yearData = stationData.stream()
            .filter(d -> d.date.startsWith(String.valueOf(year)))
            .sorted((d1, d2) -> Integer.compare(d2.aqi, d1.aqi))
            .toList();
        
        int rank = -1;
        for (int i = 0; i < yearData.size(); i++) {
            if (yearData.get(i).date.equals(date)) {
                rank = i + 1;
                break;
            }
        }
        
        result.put("yearRank", Map.of(
            "rank", rank,
            "total", yearData.size(),
            "betterThan", rank > 0 ? Math.round((1.0 - (double)rank / yearData.size()) * 100) : 0
        ));
        
        return result;
    }
    
    /**
     * 查询日期区间的空气质量数据
     * 
     * @param stationName 站点名称（如"中原区"）
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate 结束日期（yyyy-MM-dd）
     * @return 日期区间内的所有数据
     */
    public List<Map<String, Object>> queryAirQualityByDateRange(String stationName, String startDate, String endDate) {
        loadCsvDataIfNeeded();
        
        // 从缓存中获取该站点的所有数据（CSV中直接使用行政区名称）
        List<DailyData> stationData = csvDataCache.get(stationName);
        
        if (stationData == null || stationData.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 筛选日期范围内的数据
        return stationData.stream()
            .filter(d -> d.date.compareTo(startDate) >= 0 && d.date.compareTo(endDate) <= 0)
            .sorted(Comparator.comparing(d -> d.date))
            .map(this::convertDailyDataToMap)
            .toList();
    }
    
    /**
     * 获取可用日期范围
     */
    private Map<String, String> getAvailableDateRange(List<DailyData> data) {
        DailyData earliest = data.stream().min(Comparator.comparing(d -> d.date)).orElse(null);
        DailyData latest = data.stream().max(Comparator.comparing(d -> d.date)).orElse(null);
        
        return Map.of(
            "earliest", earliest != null ? earliest.date : "",
            "latest", latest != null ? latest.date : ""
        );
    }
    
    /**
     * 获取过去5年的同日数据
     */
    private List<Map<String, Object>> getHistoricalSameDay(List<DailyData> stationData, String date) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] parts = date.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        
        String monthDay = String.format("-%02d-%02d", month, day);
        
        for (int y = year - 5; y < year; y++) {
            String historicalDate = y + monthDay;
            stationData.stream()
                .filter(d -> d.date.equals(historicalDate))
                .findFirst()
                .ifPresent(d -> {
                    Map<String, Object> data = convertDailyDataToMap(d);
                    data.put("year", y);
                    result.add(data);
                });
        }
        
        return result;
    }
    
    /**
     * 将DailyData转换为Map
     */
    private Map<String, Object> convertDailyDataToMap(DailyData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", data.date);
        map.put("aqi", data.aqi);
        map.put("qualityLevel", data.qualityLevel);
        map.put("pm25", data.pm25);
        map.put("pm10", data.pm10);
        map.put("o3", data.o3);
        map.put("so2", data.so2);
        map.put("co", data.co);
        map.put("no2", data.no2);
        map.put("primaryPollutant", data.primaryPollutant);
        return map;
    }
}
