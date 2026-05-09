package cn.edu.zzu.airanalysis.etl;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.functions.*;
import org.apache.spark.sql.expressions.Window;

import java.util.*;

/**
 * Spark数据清洗与ETL主程序
 */
public class DataETL {

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
            .appName("AirQualityDataETL")
            .master("yarn")
            .config("spark.sql.adaptive.enabled", "true")
            .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
            .enableHiveSupport()
            .getOrCreate();

        try {
            // 1. 从ODS层读取原始数据
            Dataset<Row> odsDF = readODSData(spark);

            // 2. 数据质量检查
            Dataset<Row> qualityReport = checkDataQuality(odsDF);

            // 3. 数据清洗
            Dataset<Row> cleanedDF = dataCleaning(spark, odsDF);

            // 4. 数据转换
            Dataset<Row> transformedDF = dataTransformation(cleanedDF);

            // 5. 写入DWD层
            writeDWDData(transformedDF);

            // 6. 生成DWS层汇总数据
            generateDWSData(transformedDF);

            System.out.println("ETL流程执行完成");

        } catch (Exception e) {
            System.err.println("ETL执行失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            spark.stop();
        }
    }

    /**
     * 读取ODS层原始数据
     */
    public static Dataset<Row> readODSData(SparkSession spark) {
        String odsPath = "/user/hive/warehouse/air_quality_db.db/ods_air_quality_raw";

        StructType schema = new StructType(new StructField[]{
            new StructField("id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("station_code", DataTypes.StringType, false, Metadata.empty()),
            new StructField("station_name", DataTypes.StringType, false, Metadata.empty()),
            new StructField("monitor_time", DataTypes.StringType, false, Metadata.empty()),
            new StructField("pm25", DataTypes.DoubleType, true, Metadata.empty()),
            new StructField("pm10", DataTypes.DoubleType, true, Metadata.empty()),
            new StructField("so2", DataTypes.DoubleType, true, Metadata.empty()),
            new StructField("no2", DataTypes.DoubleType, true, Metadata.empty()),
            new StructField("co", DataTypes.DoubleType, true, Metadata.empty()),
            new StructField("o3", DataTypes.DoubleType, true, Metadata.empty()),
            new StructField("aqi", DataTypes.IntegerType, true, Metadata.empty()),
            new StructField("primary_pollutant", DataTypes.StringType, true, Metadata.empty()),
            new StructField("quality_level", DataTypes.StringType, true, Metadata.empty()),
            new StructField("data_status", DataTypes.StringType, true, Metadata.empty()),
            new StructField("create_time", DataTypes.StringType, true, Metadata.empty())
        });

        return spark.read()
            .option("format", "textfile")
            .option("delimiter", "\t")
            .schema(schema)
            .load(odsPath);
    }

    /**
     * 数据质量检查
     */
    public static Dataset<Row> checkDataQuality(Dataset<Row> df) {
        long totalCount = df.count();
        long nullCount = df.filter(col("pm25").isNull().or(col("aqi").isNull())).count();
        long outOfRangeCount = df.filter(
            col("pm25").lt(lit(0)).or(col("pm25").gt(lit(1000)))
            .or(col("pm10").lt(lit(0))).or(col("pm10").gt(lit(1000)))
            .or(col("aqi").lt(lit(0))).or(col("aqi").gt(lit(500)))
        ).count();

        // 生成质量报告
        List<Row> report = new ArrayList<>();
        report.add(RowFactory.create("total_count", (double) totalCount));
        report.add(RowFactory.create("null_count", (double) nullCount));
        report.add(RowFactory.create("out_of_range_count", (double) outOfRangeCount));
        report.add(RowFactory.create("null_rate", (double) nullCount / totalCount));
        report.add(RowFactory.create("out_of_range_rate", (double) outOfRangeCount / totalCount));

        StructType schema = new StructType(new StructField[]{
            new StructField("metric", DataTypes.StringType, false, Metadata.empty()),
            new StructField("value", DataTypes.DoubleType, false, Metadata.empty())
        });

        Dataset<Row> result = df.sparkSession().createDataFrame(report, schema);
        result.show();
        return result;
    }

    /**
     * 数据清洗
     * - 异常值检测（3σ原则/箱线图）
     * - 缺失值处理
     * - 数据类型转换
     */
    public static Dataset<Row> dataCleaning(SparkSession spark, Dataset<Row> df) {
        // 1. 类型转换
        Dataset<Row> cleaned = df
            .withColumn("monitor_time", to_timestamp(col("monitor_time")))
            .withColumn("dt", date_format(col("monitor_time"), "yyyy-MM-dd"));

        // 2. 过滤明显异常值（浓度为负数或极大值）
        cleaned = cleaned.filter(
            col("pm25").between(lit(0), lit(1000))
            .and(col("pm10").between(lit(0), lit(1000)))
            .and(col("so2").between(lit(0), lit(500)))
            .and(col("no2").between(lit(0), lit(300)))
            .and(col("co").between(lit(0), lit(50)))
            .and(col("o3").between(lit(0), lit(500)))
            .and(col("aqi").between(lit(0), lit(500)))
        );

        // 3. 标记数据有效性（使用3σ原则检测异常）
        cleaned = addOutlierFlag(cleaned);

        // 4. 缺失值处理（使用窗口函数进行线性插值）
        cleaned = handleMissingValues(cleaned);

        return cleaned;
    }

    /**
     * 添加异常值标记
     */
    public static Dataset<Row> addOutlierFlag(Dataset<Row> df) {
        WindowSpec windowSpec = Window.partitionBy("station_code")
            .orderBy("monitor_time")
            .rowsBetween(-24, 0);

        return df.withColumn("pm25_mean", avg(col("pm25")).over(windowSpec))
            .withColumn("pm25_std", stddev(col("pm25")).over(windowSpec))
            .withColumn("is_valid",
                when(abs(col("pm25").minus(col("pm25_mean"))).gt(col("pm25_std").multiply(lit(3))), lit(0))
                    .otherwise(lit(1)))
            .drop("pm25_mean", "pm25_std");
    }

    /**
     * 处理缺失值（线性插值）
     */
    public static Dataset<Row> handleMissingValues(Dataset<Row> df) {
        WindowSpec windowSpec = Window.partitionBy("station_code")
            .orderBy("monitor_time")
            .rowsBetween(-1, 1);

        // 对缺失值进行前后插值
        return df.withColumn("pm25",
            when(col("pm25").isNull(),
                avg(col("pm25")).over(windowSpec))
                .otherwise(col("pm25")));
    }

    /**
     * 数据转换（维度退化、指标计算）
     */
    public static Dataset<Row> dataTransformation(Dataset<Row> df) {
        return df
            // 时间维度提取
            .withColumn("year", year(col("monitor_time")))
            .withColumn("month", month(col("monitor_time")))
            .withColumn("day", dayofmonth(col("monitor_time")))
            .withColumn("hour", hour(col("monitor_time")))
            .withColumn("day_of_week", dayofweek(col("monitor_time")))
            .withColumn("week_of_year", weekofyear(col("monitor_time")))
            // AQI等级转换
            .withColumn("aqi_level",
                when(col("aqi").leq(50), lit(1))
                    .when(col("aqi").leq(100), lit(2))
                    .when(col("aqi").leq(150), lit(3))
                    .when(col("aqi").leq(200), lit(4))
                    .when(col("aqi").leq(300), lit(5))
                    .otherwise(lit(6)))
            // ETL时间戳
            .withColumn("etl_time", current_timestamp());
    }

    /**
     * 写入DWD层
     */
    public static void writeDWDData(Dataset<Row> df) {
        String dwdPath = "/user/hive/warehouse/air_quality_db.db/dwd_air_quality_dt";

        df.write()
            .mode(SaveMode.Append)
            .partitionBy("year", "month", "day")
            .format("orc")
            .option("orc.compress", "SNAPPY")
            .save(dwdPath);

        System.out.println("DWD层数据写入完成: " + dwdPath);
    }

    /**
     * 生成DWS层汇总数据
     */
    public static void generateDWSData(Dataset<Row> df) {
        // 站点小时汇总
        Dataset<Row> stationHourAgg = df
            .groupBy("station_code", "station_name", "year", "month", "day", "hour")
            .agg(
                avg("pm25").as("pm25_avg"),
                avg("pm10").as("pm10_avg"),
                avg("so2").as("so2_avg"),
                avg("no2").as("no2_avg"),
                avg("co").as("co_avg"),
                avg("o3").as("o3_avg"),
                avg("aqi").as("aqi_avg"),
                max("aqi").as("aqi_max"),
                min("aqi").as("aqi_min"),
                count(lit(1)).as("valid_count")
            )
            .withColumn("hour_time",
                to_timestamp(concat(
                    col("year"), lit("-"),
                    lpad(col("month"), 2, "0"), lit("-"),
                    lpad(col("day"), 2, "0"), lit(" "),
                    lpad(col("hour"), 2, "0"), lit(":00:00")
                )))
            .withColumn("etl_time", current_timestamp());

        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";
        stationHourAgg.write()
            .mode(SaveMode.Append)
            .partitionBy("year", "month")
            .format("orc")
            .save(dwsPath);

        System.out.println("DWS层数据生成完成");
    }
}