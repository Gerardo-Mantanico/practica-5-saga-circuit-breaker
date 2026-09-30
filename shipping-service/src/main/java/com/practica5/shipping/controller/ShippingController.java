package com.practica5.shipping.controller;

import com.practica5.shipping.model.Shipment;
import com.practica5.shipping.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shipping")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> agendar(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        String address = body.get("address") != null ? (String) body.get("address") : "Sin direccion";
        Map<String, Object> resultado = shippingService.agendarEnvio(orderId, address);
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelar(@PathVariable Long id) {
        shippingService.cancelarEnvio(id);
        Shipment envio = shippingService.obtenerEnvio(id);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("shippingId", id);
        respuesta.put("status", envio != null ? envio.getStatus() : "NOT_FOUND");
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        Shipment envio = shippingService.obtenerEnvio(id);
        if (envio == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(envio);
    }

    @GetMapping
    public List<Shipment> listar() {
        return shippingService.listarEnvios();
    }
}
