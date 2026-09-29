package com.practica5.payment.service;

import com.practica5.payment.client.InventoryClient;
import com.practica5.payment.model.Payment;
import com.practica5.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final InventoryClient inventoryClient;

    public PaymentService(PaymentRepository repository, InventoryClient inventoryClient) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
    }

    public Map<String, Object> procesarPago(Long orderId, Double amount,
                                            String productId, Integer quantity) {
        Payment pago = new Payment();
        pago.setOrderId(orderId);
        pago.setAmount(amount);
        pago.setStatus("PENDING");
        pago = repository.save(pago);
        log.info("Pago {} creado para la orden {}", pago.getId(), orderId);

        try {
            Map<String, Object> resultadoInventario =
                    inventoryClient.reservarStock(orderId, productId, quantity);

            if (resultadoInventario == null || !"OK".equals(resultadoInventario.get("status"))) {
                log.warn("El inventario no acepto la reserva para la orden {}", orderId);
                pago.setStatus("REFUNDED");
                repository.save(pago);
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("status", "ERROR");
                respuesta.put("paymentId", pago.getId());
                respuesta.put("mensaje", resultadoInventario != null
                        ? resultadoInventario.get("mensaje")
                        : "Inventario no disponible");
                return respuesta;
            }

            pago.setStatus("CHARGED");
            repository.save(pago);
            log.info("Pago {} cobrado correctamente", pago.getId());

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("status", "OK");
            respuesta.put("paymentId", pago.getId());
            respuesta.put("inventoryId", resultadoInventario.get("reservationId"));
            respuesta.put("shippingId", resultadoInventario.get("shippingId"));
            return respuesta;

        } catch (Exception e) {
            log.error("Error en el pago {} de la orden {}: {}", pago.getId(), orderId, e.getMessage());
            pago.setStatus("REFUNDED");
            repository.save(pago);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("status", "ERROR");
            respuesta.put("paymentId", pago.getId());
            respuesta.put("mensaje", "Error procesando el pago o el inventario");
            return respuesta;
        }
    }

    public void reembolsar(Long paymentId) {
        Payment pago = repository.findById(paymentId).orElse(null);
        if (pago != null && "CHARGED".equals(pago.getStatus())) {
            pago.setStatus("REFUNDED");
            repository.save(pago);
            log.info("Pago {} reembolsado", paymentId);
        }
    }

    public Payment obtenerPago(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Payment> listarPagos() {
        return repository.findAll();
    }
}
