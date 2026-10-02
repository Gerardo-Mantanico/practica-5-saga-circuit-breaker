package com.practica5.shipping.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shippingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shipping Service")
                        .description("Practica 5 - Saga y Circuit Breaker. Servicio de envios y entregas.")
                        .version("1.0.0"));
    }
}
