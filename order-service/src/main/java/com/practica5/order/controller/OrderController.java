package com.practica5.order.controller;

import com.practica5.order.model.Order;
import com.practica5.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> crearOrden(@RequestBody Map<String, Object> body) {
        String productId = (String) body.get("productId");
        Integer quantity = Integer.valueOf(String.valueOf(body.get("quantity")));
        Double amount = Double.valueOf(String.valueOf(body.get("amount")));

        Order orden = orderService.crearOrden(productId, quantity, amount);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("orderId", orden.getId());
        respuesta.put("status", orden.getStatus());
        respuesta.put("productId", orden.getProductId());
        respuesta.put("quantity", orden.getQuantity());
        respuesta.put("amount", orden.getAmount());
        respuesta.put("paymentId", orden.getPaymentId());
        respuesta.put("inventoryId", orden.getInventoryId());
        respuesta.put("shippingId", orden.getShippingId());

        if ("CANCELLED".equals(orden.getStatus())) {
            String detalle = orderService.obtenerUltimoMensajeError(orden.getId());
            if (detalle == null || detalle.isBlank()) {
                detalle = "La orden fue cancelada porque un paso de la saga fallo";
            }
            respuesta.put("mensaje", detalle);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
        }
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelarOrden(@PathVariable Long id) {
        orderService.cancelarOrden(id);
        Order orden = orderService.obtenerOrden(id);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("orderId", id);
        respuesta.put("status", orden != null ? orden.getStatus() : "NOT_FOUND");
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrden(@PathVariable Long id) {
        Order orden = orderService.obtenerOrden(id);
        if (orden == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orden);
    }

    @GetMapping
    public List<Order> listarOrdenes() {
        return orderService.listarOrdenes();
    }
}
