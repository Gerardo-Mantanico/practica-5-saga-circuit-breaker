package com.practica5.order.service;

import com.practica5.order.client.PaymentClient;
import com.practica5.order.model.Order;
import com.practica5.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository repository, PaymentClient paymentClient) {
        this.repository = repository;
        this.paymentClient = paymentClient;
    }

    private String ultimoMensajeError;

    public Order crearOrden(String productId, Integer quantity, Double amount) {
        ultimoMensajeError = null;
        Order orden = new Order();
        orden.setProductId(productId);
        orden.setQuantity(quantity);
        orden.setAmount(amount);
        orden.setStatus("PENDING");
        orden = repository.save(orden);
        log.info("Orden {} creada en estado PENDING", orden.getId());

        try {
            Map<String, Object> resultadoPago =
                    paymentClient.procesarPago(orden.getId(), amount, productId, quantity);

            if (resultadoPago != null && resultadoPago.get("paymentId") != null) {
                orden.setPaymentId(Long.valueOf(String.valueOf(resultadoPago.get("paymentId"))));
            }

            if (resultadoPago == null || !"OK".equals(resultadoPago.get("status"))) {
                log.warn("El pago no se proceso correctamente para la orden {}", orden.getId());
                if (resultadoPago != null && resultadoPago.get("mensaje") != null) {
                    ultimoMensajeError = String.valueOf(resultadoPago.get("mensaje"));
                }
                orden = repository.save(orden);
                cancelarOrden(orden.getId());
                orden.setStatus("CANCELLED");
                return repository.save(orden);
            }

            if (resultadoPago.get("inventoryId") != null) {
                orden.setInventoryId(Long.valueOf(String.valueOf(resultadoPago.get("inventoryId"))));
            }
            if (resultadoPago.get("shippingId") != null) {
                orden.setShippingId(Long.valueOf(String.valueOf(resultadoPago.get("shippingId"))));
            }

            orden.setStatus("CONFIRMED");
            orden = repository.save(orden);
            log.info("Orden {} confirmada correctamente", orden.getId());
            return orden;

        } catch (Exception e) {
            log.error("Error en la saga para la orden {}: {}", orden.getId(), e.getMessage());
            cancelarOrden(orden.getId());
            orden.setStatus("CANCELLED");
            return repository.save(orden);
        }
    }

    public void cancelarOrden(Long id) {
        Order orden = repository.findById(id).orElse(null);
        if (orden != null && !"CANCELLED".equals(orden.getStatus())) {
            if (orden.getPaymentId() != null) {
                try {
                    paymentClient.reembolsar(orden.getPaymentId());
                    log.info("Reembolso solicitado para el pago {} de la orden {}",
                            orden.getPaymentId(), id);
                } catch (Exception e) {
                    log.error("No se pudo reembolsar el pago {}: {}",
                            orden.getPaymentId(), e.getMessage());
                }
            }
            orden.setStatus("CANCELLED");
            repository.save(orden);
            log.info("Orden {} cancelada", id);
        }
    }

    public String obtenerUltimoMensajeError(Long ordenId) {
        return ultimoMensajeError;
    }

    public Order obtenerOrden(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Order> listarOrdenes() {
        return repository.findAll();
    }
}
