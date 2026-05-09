package cn.edu.zzu.airanalysis.analysis;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.linalg.Matrix;
import org.apache.spark.sql.functions.*;
import org.apache.spark.sql.expressions.Window;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 相关性分析模块
 * 功能：计算污染物与气象因素之间的相关性
 */
public class CorrelationAnalysis {

    private final SparkSession spark;

    public CorrelationAnalysis(SparkSession spark) {
        this.spark = spark;
    }

    /**
     * 皮尔逊/斯皮尔曼相关性分析
     */
    public Dataset<Row> analyzeCorrelation(Dataset<Row> data) {
        String[] columns = {"pm25", "pm10", "so2", "no2", "co", "o3",
                           "temperature", "humidity", "wind_speed", "pressure"};

        // 过滤有效数据
        Dataset<Row> validData = data.na().drop();

        // 向量化
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(columns)
            .setOutputCol("features");

        Dataset<Row> vectorData = assembler.transform(validData).select("features");

        // 计算皮尔逊相关系数矩阵
        Dataset<Row> pearsonCorr = org.apache.spark.ml.stat.Correlation
            .corr(vectorData, "features", "pearson")
            .select(org.apache.spark.sql.functions.col("pearson(features)"));

        Matrix pearsonMatrix = pearsonCorr.head().getAs(0);

        // 转换为DataFrame
        List<Row> results = new ArrayList<>();
        for (int i = 0; i < pearsonMatrix.numRows(); i++) {
            for (int j = i + 1; j < pearsonMatrix.numCols(); j++) {
                results.add(RowFactory.create(
                    columns[i],
                    columns[j],
                    pearsonMatrix.apply(i, j)
                ));
            }
        }

        StructType schema = new StructType(new StructField[]{
            new StructField("factor_x", DataTypes.StringType, false, Metadata.empty()),
            new StructField("factor_y", DataTypes.StringType, false, Metadata.empty()),
            new StructField("pearson_corr", DataTypes.DoubleType, false, Metadata.empty())
        });

        return spark.createDataFrame(results, schema);
    }

    /**
     * 污染物与气象因素相关性分析
     */
    public Dataset<Row> pollutantWeatherCorrelation() {
        String dwdPath = "/user/hive/warehouse/air_quality_db.db/dwd_air_quality_dt";
        String weatherPath = "/user/hive/warehouse/air_quality_db.db/dwd_weather_dt";

        Dataset<Row> airDF = spark.read().format("orc").load(dwdPath);
        Dataset<Row> weatherDF = spark.read().format("orc").load(weatherPath);

        // 关联空气质量与气象数据
        Dataset<Row> joined = airDF.join(
            weatherDF,
            airDF.col("station_code").equalTo(weatherDF.col("station_code"))
                .and(airDF.col("monitor_time").equalTo(weatherDF.col("monitor_time"))),
            "inner"
        );

        // 分析相关性
        Dataset<Row> airData = joined.select(
            col("pm25"), col("pm10"), col("no2"), col("o3"),
            col("temperature"), col("humidity"), col("wind_speed"), col("pressure")
        );

        return analyzeCorrelation(airData);
    }

    /**
     * 时间滞后相关性分析
     */
    public Dataset<Row> laggedCorrelation(String pollutant, int maxLag) {
        String dwdPath = "/user/hive/warehouse/air_quality_db.db/dwd_air_quality_dt";

        Dataset<Row> df = spark.read().format("orc").load(dwdPath)
            .filter("station_code = '410101'")
            .orderBy("monitor_time");

        WindowSpec windowSpec = Window.orderBy("monitor_time");

        List<Row> results = new ArrayList<>();
        for (int lag = 1; lag <= maxLag; lag++) {
            Dataset<Row> withLag = df.withColumn(pollutant + "_lag_" + lag,
                org.apache.spark.sql.functions.lag(col(pollutant), lag).over(windowSpec));

            Dataset<Row> validData = withLag.filter(col(pollutant + "_lag_" + lag).isNotNull());

            if (validData.count() > 0) {
                double corr = validData.stat().corr(pollutant, pollutant + "_lag_" + lag);
                results.add(RowFactory.create(lag, corr));
            }
        }

        StructType schema = new StructType(new StructField[]{
            new StructField("lag_hours", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("correlation", DataTypes.DoubleType, false, Metadata.empty())
        });

        return spark.createDataFrame(results, schema).orderBy(col("lag_hours"));
    }

    /**
     * 特征重要性分析（使用随机森林）
     */
    public Dataset<Row> featureImportance() {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";
        Dataset<Row> df = spark.read().format("orc").load(dwsPath);

        // 准备特征和标签
        String[] features = {
            "pm25_avg", "pm10_avg", "so2_avg", "no2_avg", "co_avg", "o3_avg",
            "temp_avg", "humidity_avg", "wind_speed_avg"
        };

        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(features)
            .setOutputCol("features");

        Dataset<Row> vectorData = assembler.transform(df.na().drop())
            .select(col("aqi_avg").as("label"), col("features"));

        // 训练随机森林
        org.apache.spark.ml.regression.RandomForestRegressor rf =
            new org.apache.spark.ml.regression.RandomForestRegressor()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setNumTrees(50)
            .setMaxDepth(10);

        org.apache.spark.ml.regression.RandomForestRegressionModel model = rf.fit(vectorData);

        // 提取特征重要性
        List<Row> importance = new ArrayList<>();
        double[] values = model.featureImportances().toArray();
        for (int i = 0; i < features.length; i++) {
            importance.add(RowFactory.create(features[i], values[i]));
        }

        StructType schema = new StructType(new StructField[]{
            new StructField("feature_name", DataTypes.StringType, false, Metadata.empty()),
            new StructField("importance", DataTypes.DoubleType, false, Metadata.empty())
        });

        Dataset<Row> result = spark.createDataFrame(importance, schema);
        return result.orderBy(org.apache.spark.sql.functions.desc("importance"));
    }

    /**
     * 生成相关性分析报告
     */
    public void generateReport() {
        System.out.println("=" + "=".repeat(60));
        System.out.println("空气质量相关性分析报告");
        System.out.println("=" + "=".repeat(60));

        // 污染物间相关性
        System.out.println("\n【污染物间相关性】");
        String dwdPath = "/user/hive/warehouse/air_quality_db.db/dwd_air_quality_dt";
        Dataset<Row> airDF = spark.read().format("orc").load(dwdPath);
        Dataset<Row> correlation = analyzeCorrelation(airDF.select(
            col("pm25"), col("pm10"), col("so2"), col("no2"), col("co"), col("o3")));
        correlation.show(10);

        // 污染物与气象因素相关性
        System.out.println("\n【污染物与气象因素相关性】");
        pollutantWeatherCorrelation().show(10);

        // PM2.5自相关分析
        System.out.println("\n【PM2.5时间滞后相关性】");
        laggedCorrelation("pm25", 24).show(24);

        // 特征重要性
        System.out.println("\n【影响AQI的关键因素】");
        featureImportance().show();

        System.out.println("=" + "=".repeat(60));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
            .appName("CorrelationAnalysis")
            .master("yarn")
            .enableHiveSupport()
            .getOrCreate();

        CorrelationAnalysis analysis = new CorrelationAnalysis(spark);
        analysis.generateReport();

        spark.stop();
    }
}