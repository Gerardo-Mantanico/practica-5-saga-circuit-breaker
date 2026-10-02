package com.practica5.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service")
                        .description("Practica 5 - Saga y Circuit Breaker. Servicio de inventario, stock y fail-mode.")
                        .version("1.0.0"));
    }
}
