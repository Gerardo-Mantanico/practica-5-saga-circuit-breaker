package com.practica5.inventory.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class ShippingClient {

    private final RestTemplate restTemplate;
    private final String urlEnvio;

    public ShippingClient(@Value("${servicio-envio.url}") String urlEnvio) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
        this.urlEnvio = urlEnvio;
    }

    public Map<String, Object> agendarEnvio(Long orderId) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("address", "Calle ejemplo 123, Ciudad");
        return restTemplate.postForObject(urlEnvio + "/shipping/schedule", body, Map.class);
    }

    public void cancelarEnvio(Long shippingId) {
        restTemplate.delete(urlEnvio + "/shipping/" + shippingId);
    }
}
