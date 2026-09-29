package com.practica5.payment.controller;

import com.practica5.payment.model.Payment;
import com.practica5.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> procesarPago(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Double amount = Double.valueOf(String.valueOf(body.get("amount")));
        String productId = body.get("productId") != null ? (String) body.get("productId") : "P001";
        Integer quantity = body.get("quantity") != null
                ? Integer.valueOf(String.valueOf(body.get("quantity")))
                : 1;

        Map<String, Object> resultado = paymentService.procesarPago(orderId, amount, productId, quantity);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Map<String, Object>> reembolsar(@PathVariable Long id) {
        paymentService.reembolsar(id);
        Payment pago = paymentService.obtenerPago(id);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("paymentId", id);
        respuesta.put("status", pago != null ? pago.getStatus() : "NOT_FOUND");
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPago(@PathVariable Long id) {
        Payment pago = paymentService.obtenerPago(id);
        if (pago == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pago);
    }

    @GetMapping
    public List<Payment> listarPagos() {
        return paymentService.listarPagos();
    }
}
