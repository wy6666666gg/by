package cn.edu.zzu.airweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 空气质量分析系统Web模块启动类
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务
public class AirWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirWebApplication.class, args);
    }
}
