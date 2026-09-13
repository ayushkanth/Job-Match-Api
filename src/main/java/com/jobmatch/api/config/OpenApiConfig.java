package com.jobmatch.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobMatchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Job Match Recommendation API")
                        .description("A high-performance, transparent, rule-based job recommendation service built with Spring Boot 3.")
                        .version("1.0.0")
                        .contact(new Contact().name("Job Match Engineering").email("support@jobmatch.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
