package cn.edu.zzu.airweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI接口文档配置
 * 提供Swagger UI在线文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI airQualityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("城市空气质量分析与预测系统API")
                        .description("基于Hive的城市空气质量大数据分析与预测系统接口文档，"
                                + "提供空气质量实时监测、历史趋势分析、空间分布分析、"
                                + "相关性分析、随机森林预测等功能的RESTful API。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("郑州大学")
                                .url("https://www.zzu.edu.cn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/api").description("本地开发环境")
                ));
    }
}
