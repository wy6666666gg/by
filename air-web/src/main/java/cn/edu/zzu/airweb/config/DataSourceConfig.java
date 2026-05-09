package cn.edu.zzu.airweb.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 多数据源配置
 * 配置MySQL业务数据源和Hive分析数据源
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.mysql.url}")
    private String mysqlUrl;

    @Value("${spring.datasource.mysql.username}")
    private String mysqlUsername;

    @Value("${spring.datasource.mysql.password}")
    private String mysqlPassword;

    @Value("${spring.datasource.mysql.driver-class-name}")
    private String mysqlDriverClass;

    @Value("${spring.datasource.mysql.druid.initial-size}")
    private int initialSize;

    @Value("${spring.datasource.mysql.druid.min-idle}")
    private int minIdle;

    @Value("${spring.datasource.mysql.druid.max-active}")
    private int maxActive;

    @Value("${spring.datasource.mysql.druid.max-wait}")
    private long maxWait;

    /**
     * MySQL主数据源（业务数据）
     */
    @Primary
    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(mysqlUrl);
        dataSource.setUsername(mysqlUsername);
        dataSource.setPassword(mysqlPassword);
        dataSource.setDriverClassName(mysqlDriverClass);
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        return dataSource;
    }

    /**
     * Hive数据源（分析数据仓库）
     * scope=provided，仅在Hive环境可用时启用
     */
    @Bean(name = "hiveDataSource")
    public DataSource hiveDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            dataSource.setUrl("jdbc:hive2://localhost:10000/air_quality_db");
            dataSource.setUsername("hive");
            dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
            dataSource.setInitialSize(2);
            dataSource.setMinIdle(1);
            dataSource.setMaxActive(5);
            dataSource.setMaxWait(30000);
        } catch (ClassNotFoundException e) {
            dataSource.setUrl(mysqlUrl);
            dataSource.setUsername(mysqlUsername);
            dataSource.setPassword(mysqlPassword);
            dataSource.setDriverClassName(mysqlDriverClass);
        }
        return dataSource;
    }
}
