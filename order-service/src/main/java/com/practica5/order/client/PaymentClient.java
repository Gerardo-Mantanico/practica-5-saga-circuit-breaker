package com.practica5.order.client;

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
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestTemplate restTemplate;
    private final String urlPago;

    public PaymentClient(@Value("${servicio-pago.url}") String urlPago) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
        this.urlPago = urlPago;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPago")
    public Map<String, Object> procesarPago(Long orderId, Double amount, String productId, Integer quantity) {
        log.info("Order Service llamando a Payment Service para la orden {}", orderId);
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("amount", amount);
        body.put("productId", productId);
        body.put("quantity", quantity);
        return restTemplate.postForObject(urlPago + "/payments", body, Map.class);
    }

    public Map<String, Object> fallbackPago(Long orderId, Double amount, String productId,
                                            Integer quantity, Throwable t) {
        log.warn("Circuit Breaker de pago ABIERTO o error. Fallback para orden {}: {}",
                orderId, t.getMessage());
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ERROR");
        respuesta.put("paymentId", null);
        respuesta.put("mensaje", "Servicio de pago no disponible, intente mas tarde");
        return respuesta;
    }

    public void reembolsar(Long paymentId) {
        restTemplate.postForObject(urlPago + "/payments/" + paymentId + "/refund", null, Map.class);
    }
}
