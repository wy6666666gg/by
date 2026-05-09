package cn.edu.zzu.airweb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 随机森林预测服务
 * 基于历史数据使用随机森林算法预测次日空气质量
 */
@Slf4j
@Service
public class RandomForestService {

    // 随机森林参数
    private static final int NUM_TREES = 100;        // 树的数量
    private static final int MAX_DEPTH = 10;         // 最大深度
    private static final int MIN_SAMPLES_SPLIT = 2;  // 最小分裂样本数
    private static final int NUM_FEATURES = 4;       // 每棵树使用的特征数

    /**
     * 使用随机森林预测次日空气质量
     */
    public Map<String, Object> predict(String stationName, List<Map<String, Object>> historicalData) {
        log.info("开始随机森林预测，站点: {}，历史数据条数: {}", stationName, historicalData.size());
        
        // 1. 数据预处理
        List<Sample> samples = preprocessData(historicalData);
        
        if (samples.size() < 5) {
            throw new IllegalArgumentException("有效历史数据不足，需要至少5天");
        }
        
        // 2. 构建随机森林
        List<DecisionTree> forest = buildRandomForest(samples);
        
        // 3. 准备预测特征（最近一天的数据作为输入）
        Sample lastSample = samples.get(samples.size() - 1);
        double[] predictionFeatures = extractFeatures(lastSample);
        
        // 4. 使用森林进行预测
        double predictedAqi = predictWithForest(forest, predictionFeatures);
        
        // 5. 预测六项污染物
        Map<String, Double> pollutants = predictPollutants(samples, predictedAqi);
        
        // 6. 计算特征重要性
        List<Map<String, Object>> featureImportance = calculateFeatureImportance(forest);
        
        // 7. 计算置信度
        double confidence = calculateConfidence(forest, predictionFeatures, predictedAqi);
        
        // 8. 计算预测日期
        String predictDate = calculatePredictDate(historicalData);
        
        Map<String, Object> result = new HashMap<>();
        result.put("stationName", stationName);
        result.put("aqi", (int) Math.round(predictedAqi));
        result.put("qualityLevel", getAqiLevel(predictedAqi));
        result.put("primaryPollutant", predictedAqi > 100 ? "PM2.5" : "PM10");
        result.put("pm25", pollutants.get("pm25"));
        result.put("pm10", pollutants.get("pm10"));
        result.put("so2", pollutants.get("so2"));
        result.put("no2", pollutants.get("no2"));
        result.put("co", pollutants.get("co"));
        result.put("o3", pollutants.get("o3"));
        result.put("predictDate", predictDate);
        result.put("confidence", (int) Math.round(confidence * 100));
        result.put("featureImportance", featureImportance);
        result.put("algorithm", "Random Forest");
        result.put("numTrees", NUM_TREES);
        
        log.info("随机森林预测完成，预测AQI: {}", result.get("aqi"));
        return result;
    }
    
    /**
     * 数据预处理
     */
    private List<Sample> preprocessData(List<Map<String, Object>> historicalData) {
        List<Sample> samples = new ArrayList<>();
        
        for (int i = 0; i < historicalData.size(); i++) {
            Map<String, Object> data = historicalData.get(i);
            try {
                Sample sample = new Sample();
                sample.aqi = getDoubleValue(data, "aqi");
                sample.pm25 = getDoubleValue(data, "pm25");
                sample.pm10 = getDoubleValue(data, "pm10");
                sample.so2 = getDoubleValue(data, "so2");
                sample.no2 = getDoubleValue(data, "no2");
                sample.co = getDoubleValue(data, "co");
                sample.o3 = getDoubleValue(data, "o3");
                sample.season = getSeason(getStringValue(data, "date", ""));
                sample.dayIndex = i;
                
                // 计算趋势特征
                if (i > 0) {
                    Sample prevSample = samples.get(i - 1);
                    sample.aqiTrend = sample.aqi - prevSample.aqi;
                }
                
                samples.add(sample);
            } catch (Exception e) {
                log.warn("数据预处理失败，跳过该条数据: {}", e.getMessage());
            }
        }
        
        return samples;
    }
    
    /**
     * 构建随机森林
     */
    private List<DecisionTree> buildRandomForest(List<Sample> samples) {
        List<DecisionTree> forest = new ArrayList<>();
        Random random = new Random(42); // 固定种子保证可重复性
        
        for (int i = 0; i < NUM_TREES; i++) {
            // Bootstrap采样
            List<Sample> bootstrapSamples = bootstrapSample(samples, random);
            
            // 构建决策树
            DecisionTree tree = buildTree(bootstrapSamples, 0, random);
            forest.add(tree);
        }
        
        return forest;
    }
    
    /**
     * Bootstrap采样
     */
    private List<Sample> bootstrapSample(List<Sample> samples, Random random) {
        List<Sample> bootstrap = new ArrayList<>();
        int n = samples.size();
        
        for (int i = 0; i < n; i++) {
            int index = random.nextInt(n);
            bootstrap.add(samples.get(index));
        }
        
        return bootstrap;
    }
    
    /**
     * 构建决策树
     */
    private DecisionTree buildTree(List<Sample> samples, int depth, Random random) {
        DecisionTree tree = new DecisionTree();
        
        // 停止条件
        if (depth >= MAX_DEPTH || samples.size() < MIN_SAMPLES_SPLIT) {
            tree.prediction = samples.stream().mapToDouble(s -> s.aqi).average().orElse(100);
            return tree;
        }
        
        // 随机选择特征子集
        List<Integer> featureIndices = selectRandomFeatures(random);
        
        // 寻找最佳分裂
        SplitResult bestSplit = findBestSplit(samples, featureIndices);
        
        if (bestSplit == null) {
            tree.prediction = samples.stream().mapToDouble(s -> s.aqi).average().orElse(100);
            return tree;
        }
        
        tree.splitFeature = bestSplit.featureIndex;
        tree.splitValue = bestSplit.splitValue;
        
        // 分裂数据
        List<Sample> leftSamples = new ArrayList<>();
        List<Sample> rightSamples = new ArrayList<>();
        
        for (Sample sample : samples) {
            double featureValue = getFeatureValue(sample, bestSplit.featureIndex);
            if (featureValue <= bestSplit.splitValue) {
                leftSamples.add(sample);
            } else {
                rightSamples.add(sample);
            }
        }
        
        // 递归构建子树
        if (!leftSamples.isEmpty()) {
            tree.left = buildTree(leftSamples, depth + 1, random);
        }
        if (!rightSamples.isEmpty()) {
            tree.right = buildTree(rightSamples, depth + 1, random);
        }
        
        return tree;
    }
    
    /**
     * 随机选择特征
     */
    private List<Integer> selectRandomFeatures(Random random) {
        List<Integer> allFeatures = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7); // 8个特征
        List<Integer> selected = new ArrayList<>();
        
        while (selected.size() < NUM_FEATURES && selected.size() < allFeatures.size()) {
            int index = random.nextInt(allFeatures.size());
            Integer feature = allFeatures.get(index);
            if (!selected.contains(feature)) {
                selected.add(feature);
            }
        }
        
        return selected;
    }
    
    /**
     * 寻找最佳分裂点
     */
    private SplitResult findBestSplit(List<Sample> samples, List<Integer> featureIndices) {
        double bestGain = -1;
        SplitResult bestSplit = null;
        
        double parentVariance = calculateVariance(samples);
        
        for (int featureIndex : featureIndices) {
            List<Double> values = new ArrayList<>();
            for (Sample sample : samples) {
                values.add(getFeatureValue(sample, featureIndex));
            }
            Collections.sort(values);
            
            // 尝试不同的分裂点
            for (int i = 0; i < values.size() - 1; i++) {
                double splitValue = (values.get(i) + values.get(i + 1)) / 2;
                
                List<Sample> left = new ArrayList<>();
                List<Sample> right = new ArrayList<>();
                
                for (Sample sample : samples) {
                    if (getFeatureValue(sample, featureIndex) <= splitValue) {
                        left.add(sample);
                    } else {
                        right.add(sample);
                    }
                }
                
                if (left.isEmpty() || right.isEmpty()) continue;
                
                // 计算方差减少量
                double leftVar = calculateVariance(left);
                double rightVar = calculateVariance(right);
                double weightedVar = (left.size() * leftVar + right.size() * rightVar) / samples.size();
                double gain = parentVariance - weightedVar;
                
                if (gain > bestGain) {
                    bestGain = gain;
                    bestSplit = new SplitResult(featureIndex, splitValue);
                }
            }
        }
        
        return bestSplit;
    }
    
    /**
     * 计算方差
     */
    private double calculateVariance(List<Sample> samples) {
        if (samples.size() < 2) return 0;
        
        double mean = samples.stream().mapToDouble(s -> s.aqi).average().orElse(0);
        double variance = samples.stream()
                .mapToDouble(s -> Math.pow(s.aqi - mean, 2))
                .average()
                .orElse(0);
        
        return variance;
    }
    
    /**
     * 获取特征值
     */
    private double getFeatureValue(Sample sample, int featureIndex) {
        switch (featureIndex) {
            case 0: return sample.aqi;
            case 1: return sample.pm25;
            case 2: return sample.pm10;
            case 3: return sample.so2;
            case 4: return sample.no2;
            case 5: return sample.co;
            case 6: return sample.o3;
            case 7: return sample.season;
            default: return 0;
        }
    }
    
    /**
     * 提取特征向量
     */
    private double[] extractFeatures(Sample sample) {
        return new double[] {
            sample.aqi,
            sample.pm25,
            sample.pm10,
            sample.so2,
            sample.no2,
            sample.co,
            sample.o3,
            sample.season
        };
    }
    
    /**
     * 使用森林预测
     */
    private double predictWithForest(List<DecisionTree> forest, double[] features) {
        double sum = 0;
        for (DecisionTree tree : forest) {
            sum += predictWithTree(tree, features);
        }
        return sum / forest.size();
    }
    
    /**
     * 使用单棵树预测
     */
    private double predictWithTree(DecisionTree tree, double[] features) {
        if (tree.prediction != null) {
            return tree.prediction;
        }
        
        double featureValue = features[tree.splitFeature];
        if (featureValue <= tree.splitValue && tree.left != null) {
            return predictWithTree(tree.left, features);
        } else if (tree.right != null) {
            return predictWithTree(tree.right, features);
        }
        
        return tree.prediction != null ? tree.prediction : 100;
    }
    
    /**
     * 预测六项污染物
     */
    private Map<String, Double> predictPollutants(List<Sample> samples, double predictedAqi) {
        Map<String, Double> result = new HashMap<>();
        
        // 基于历史数据的比例关系预测
        double avgAqi = samples.stream().mapToDouble(s -> s.aqi).average().orElse(predictedAqi);
        double ratio = predictedAqi / avgAqi;
        
        result.put("pm25", round(samples.stream().mapToDouble(s -> s.pm25).average().orElse(50) * ratio, 1));
        result.put("pm10", round(samples.stream().mapToDouble(s -> s.pm10).average().orElse(80) * ratio, 1));
        result.put("so2", round(samples.stream().mapToDouble(s -> s.so2).average().orElse(15), 1));
        result.put("no2", round(samples.stream().mapToDouble(s -> s.no2).average().orElse(40) * ratio, 1));
        result.put("co", round(samples.stream().mapToDouble(s -> s.co).average().orElse(0.8) * ratio, 2));
        result.put("o3", round(samples.stream().mapToDouble(s -> s.o3).average().orElse(95), 1));
        
        return result;
    }
    
    /**
     * 计算特征重要性
     */
    private List<Map<String, Object>> calculateFeatureImportance(List<DecisionTree> forest) {
        Map<String, Double> importanceMap = new HashMap<>();
        String[] featureNames = {"历史AQI", "PM2.5", "PM10", "SO2", "NO2", "CO", "O3", "季节"};
        
        for (DecisionTree tree : forest) {
            calculateTreeImportance(tree, importanceMap, featureNames);
        }
        
        // 归一化
        double total = importanceMap.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            importanceMap.replaceAll((k, v) -> v / total);
        }
        
        // 转换为列表并排序
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : importanceMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("importance", entry.getValue());
            result.add(item);
        }
        
        result.sort((a, b) -> Double.compare((Double) b.get("importance"), (Double) a.get("importance")));
        
        // 只返回前5个
        return result.size() > 5 ? result.subList(0, 5) : result;
    }
    
    private void calculateTreeImportance(DecisionTree tree, Map<String, Double> importanceMap, String[] featureNames) {
        if (tree == null || tree.prediction != null) return;
        
        String featureName = featureNames[tree.splitFeature];
        importanceMap.merge(featureName, 1.0, Double::sum);
        
        calculateTreeImportance(tree.left, importanceMap, featureNames);
        calculateTreeImportance(tree.right, importanceMap, featureNames);
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(List<DecisionTree> forest, double[] features, double prediction) {
        double variance = 0;
        for (DecisionTree tree : forest) {
            double treePred = predictWithTree(tree, features);
            variance += Math.pow(treePred - prediction, 2);
        }
        variance /= forest.size();
        
        // 方差越小，置信度越高
        double confidence = Math.max(0.6, 1 - Math.sqrt(variance) / prediction);
        return Math.min(0.95, confidence);
    }
    
    /**
     * 计算预测日期
     */
    private String calculatePredictDate(List<Map<String, Object>> historicalData) {
        try {
            String lastDate = (String) historicalData.get(historicalData.size() - 1).get("date");
            java.time.LocalDate date = java.time.LocalDate.parse(lastDate);
            return date.plusDays(1).toString();
        } catch (Exception e) {
            return java.time.LocalDate.now().plusDays(1).toString();
        }
    }
    
    /**
     * 获取AQI等级
     */
    private String getAqiLevel(double aqi) {
        if (aqi <= 50) return "优";
        if (aqi <= 100) return "良";
        if (aqi <= 150) return "轻度污染";
        if (aqi <= 200) return "中度污染";
        if (aqi <= 300) return "重度污染";
        return "严重污染";
    }
    
    /**
     * 获取季节
     */
    private int getSeason(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            int month = Integer.parseInt(parts[1]);
            if (month >= 3 && month <= 5) return 1; // 春
            if (month >= 6 && month <= 8) return 2; // 夏
            if (month >= 9 && month <= 11) return 3; // 秋
            return 4; // 冬
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
    
    /**
     * 样本数据类
     */
    private static class Sample {
        double aqi;
        double pm25;
        double pm10;
        double so2;
        double no2;
        double co;
        double o3;
        int season;
        int dayIndex;
        double aqiTrend;
    }
    
    /**
     * 决策树节点
     */
    private static class DecisionTree {
        Integer splitFeature;  // 分裂特征索引
        Double splitValue;     // 分裂阈值
        Double prediction;     // 叶子节点预测值
        DecisionTree left;     // 左子树
        DecisionTree right;    // 右子树
    }
    
    /**
     * 分裂结果
     */
    private static class SplitResult {
        int featureIndex;
        double splitValue;
        
        SplitResult(int featureIndex, double splitValue) {
            this.featureIndex = featureIndex;
            this.splitValue = splitValue;
        }
    }
}
