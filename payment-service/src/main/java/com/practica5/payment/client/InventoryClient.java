package com.practica5.payment.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;
    private final String urlInventario;

    public InventoryClient(@Value("${servicio-inventario.url}") String urlInventario) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
        this.urlInventario = urlInventario;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackInventario")
    public Map<String, Object> reservarStock(Long orderId, String productId, Integer quantity) {
        log.info("Payment Service llamando a Inventory Service para la orden {}", orderId);
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("productId", productId);
        body.put("quantity", quantity);
        return restTemplate.postForObject(urlInventario + "/inventory/reserve", body, Map.class);
    }

    public Map<String, Object> fallbackInventario(Long orderId, String productId,
                                                  Integer quantity, Throwable t) {
        log.warn("Circuit Breaker de inventario ABIERTO o error. Fallback para orden {}: {}",
                orderId, t.getMessage());
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ERROR");
        respuesta.put("mensaje", "Servicio de inventario no disponible, intente mas tarde");
        return respuesta;
    }

    public void liberarStock(Long orderId) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        restTemplate.postForObject(urlInventario + "/inventory/release", body, Map.class);
    }
}
