package com.practica5.inventory.controller;

import com.practica5.inventory.model.InventoryProduct;
import com.practica5.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reservar(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        String productId = (String) body.get("productId");
        Integer quantity = Integer.valueOf(String.valueOf(body.get("quantity")));

        Map<String, Object> resultado = inventoryService.reservarStock(orderId, productId, quantity);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> liberar(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Map<String, Object> resultado = inventoryService.liberarStock(orderId);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/fail-mode")
    public ResponseEntity<Map<String, Object>> failMode(@RequestBody Map<String, Object> body) {
        String productId = (String) body.get("productId");
        boolean failMode = Boolean.TRUE.equals(body.get("failMode"));
        inventoryService.setFailMode(productId, failMode);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("productId", productId);
        respuesta.put("failMode", failMode);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> obtenerProducto(@PathVariable String productId) {
        InventoryProduct producto = inventoryService.obtenerProducto(productId);
        if (producto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(producto);
    }
}
