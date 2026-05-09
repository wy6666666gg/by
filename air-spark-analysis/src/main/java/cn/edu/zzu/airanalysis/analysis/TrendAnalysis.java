package cn.edu.zzu.airanalysis.analysis;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.functions.*;
import org.apache.spark.sql.expressions.Window;

import java.util.*;

/**
 * 趋势分析模块
 * 功能：时间序列分解、趋势提取、周期识别
 */
public class TrendAnalysis {

    private final SparkSession spark;

    public TrendAnalysis(SparkSession spark) {
        this.spark = spark;
    }

    /**
     * 1. 移动平均分析
     */
    public Dataset<Row> movingAverage(int[] windowSizes) {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        Dataset<Row> df = spark.read().format("orc").load(dwsPath)
            .filter("station_code = '410101'")
            .groupBy("year", "month", "day")
            .agg(avg("aqi_avg").as("aqi"))
            .orderBy("year", "month", "day")
            .withColumn("raw_aqi", col("aqi"));

        // 添加原始值列
        Dataset<Row> result = df.withColumn("raw_aqi", col("aqi"));

        // 计算不同窗口的移动平均
        int rowNum = 0;
        for (int window : windowSizes) {
            result = result.withColumn("ma_" + window,
                avg(col("aqi")).over(
                    Window.orderBy("year", "month", "day").rowsBetween(-window + 1, 0)
                )
            );
        }

        return result.orderBy("year", "month", "day");
    }

    /**
     * 2. 趋势检测
     */
    public Dataset<Row> trendDetection() {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        Dataset<Row> df = spark.read().format("orc").load(dwsPath)
            .groupBy("year", "month")
            .agg(
                avg("pm25_avg").as("pm25"),
                avg("pm10_avg").as("pm10"),
                avg("aqi_avg").as("aqi")
            )
            .withColumn("period_num",
                row_number().over(Window.orderBy("year", "month")));

        // 使用线性回归检测趋势
        org.apache.spark.ml.regression.LinearRegression lr =
            new org.apache.spark.ml.regression.LinearRegression()
            .setMaxIter(100);

        org.apache.spark.ml.feature.VectorAssembler assembler =
            new org.apache.spark.ml.feature.VectorAssembler()
            .setInputCols(new String[]{"period_num"})
            .setOutputCol("features");

        Dataset<Row> trainData = assembler.transform(df.select(
            col("period_num"),
            col("aqi").as("label")
        ));

        org.apache.spark.ml.regression.LinearRegressionModel model = lr.fit(trainData);

        Dataset<Row> predictions = model.transform(trainData);

        double slope = model.coefficients().apply(0);

        return predictions.withColumn("trend_slope", lit(slope));
    }

    /**
     * 3. 周期性识别（自相关分析）
     */
    public Dataset<Row> periodicDetection() {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        // 计算日均值
        Dataset<Row> dailyAQI = spark.read().format("orc").load(dwsPath)
            .groupBy("year", "month", "day")
            .agg(avg("aqi_avg").as("aqi"))
            .orderBy("year", "month", "day")
            .withColumn("day_index",
                row_number().over(Window.orderBy("year", "month", "day")));

        int maxLag = 30;
        List<Row> autocorrResults = new ArrayList<>();

        double[] aqiSeries = new double[(int) dailyAQI.count()];
        Row[] rows = dailyAQI.collect();
        for (int i = 0; i < rows.length; i++) {
            aqiSeries[i] = rows[i].getDouble(3);
        }

        double mean = Arrays.stream(aqiSeries).average().orElse(0);

        for (int lag = 1; lag <= maxLag; lag++) {
            double numerator = 0;
            double denominator = 0;

            for (int i = 0; i < aqiSeries.length - lag; i++) {
                numerator += (aqiSeries[i] - mean) * (aqiSeries[i + lag] - mean);
            }
            for (double v : aqiSeries) {
                denominator += Math.pow(v - mean, 2);
            }

            double autocorr = denominator > 0 ? numerator / denominator : 0;
            autocorrResults.add(RowFactory.create(lag, autocorr));
        }

        StructType schema = new StructType(new StructField[]{
            new StructField("lag", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("autocorrelation", DataTypes.DoubleType, false, Metadata.empty())
        });

        return spark.createDataFrame(autocorrResults, schema);
    }

    /**
     * 4. 季节性变化分析
     */
    public Dataset<Row> seasonalAnalysis() {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        Dataset<Row> df = spark.read().format("orc").load(dwsPath);

        // 月度季节性
        Dataset<Row> monthlySeasonal = df.groupBy("month")
            .agg(
                avg("pm25_avg").as("avg_pm25"),
                avg("pm10_avg").as("avg_pm10"),
                avg("aqi_avg").as("avg_aqi"),
                stddev("aqi_avg").as("std_aqi")
            )
            .withColumn("season",
                when(col("month").between(3, 5), lit("春季"))
                .when(col("month").between(6, 8), lit("夏季"))
                .when(col("month").between(9, 11), lit("秋季"))
                .otherwise(lit("冬季")));

        return monthlySeasonal;
    }

    /**
     * 生成趋势分析报告
     */
    public void generateReport() {
        System.out.println("=" + "=".repeat(60));
        System.out.println("空气质量趋势分析报告");
        System.out.println("=" + "=".repeat(60));

        // 移动平均
        System.out.println("\n【移动平均分析】");
        movingAverage(new int[]{7, 14, 30}).show(30);

        // 季节性
        System.out.println("\n【季节性分析】");
        seasonalAnalysis().show(12);

        // 周期性
        System.out.println("\n【自相关分析（识别周期性）】");
        periodicDetection().show(30);

        System.out.println("=" + "=".repeat(60));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
            .appName("TrendAnalysis")
            .master("yarn")
            .enableHiveSupport()
            .getOrCreate();

        TrendAnalysis analysis = new TrendAnalysis(spark);
        analysis.generateReport();

        spark.stop();
    }
}