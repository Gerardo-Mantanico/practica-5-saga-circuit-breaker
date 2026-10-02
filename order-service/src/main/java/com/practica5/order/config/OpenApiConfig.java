package com.practica5.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service")
                        .description("Practica 5 - Saga y Circuit Breaker. Servicio de ordenes del e-commerce.")
                        .version("1.0.0"));
    }
}
