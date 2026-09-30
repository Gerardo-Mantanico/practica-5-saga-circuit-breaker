package com.practica5.inventory.service;

import com.practica5.inventory.client.ShippingClient;
import com.practica5.inventory.model.InventoryProduct;
import com.practica5.inventory.model.InventoryReservation;
import com.practica5.inventory.repository.InventoryProductRepository;
import com.practica5.inventory.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ShippingClient shippingClient;

    public InventoryService(InventoryProductRepository productRepository,
                            InventoryReservationRepository reservationRepository,
                            ShippingClient shippingClient) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.shippingClient = shippingClient;
    }

    @Transactional
    public Map<String, Object> reservarStock(Long orderId, String productCode, Integer quantity) {
        Optional<InventoryProduct> productoOpt = productRepository.findByProductCode(productCode);

        if (productoOpt.isEmpty()) {
            log.warn("Producto {} no encontrado", productCode);
            return error("Producto no encontrado: " + productCode);
        }

        InventoryProduct producto = productoOpt.get();

        if (Boolean.TRUE.equals(producto.getFailMode())) {
            log.warn("Inventory en fail-mode, se simula un error interno");
            return error("Error interno del inventario (fail-mode activado)");
        }

        if (producto.getStock() < quantity) {
            log.warn("Stock insuficiente para {}. Disponible: {}, solicitado: {}",
                    productCode, producto.getStock(), quantity);
            return error("Stock insuficiente para el producto " + productCode);
        }

        producto.setStock(producto.getStock() - quantity);
        productRepository.save(producto);

        InventoryReservation reserva = new InventoryReservation();
        reserva.setOrderId(orderId);
        reserva.setProductCode(productCode);
        reserva.setQuantity(quantity);
        reserva.setStatus("RESERVED");
        reserva = reservationRepository.save(reserva);

        log.info("Stock reservado. Orden {}, producto {}, cantidad {}. Stock restante: {}",
                orderId, productCode, quantity, producto.getStock());

        // Coreografia: Inventory llama a Shipping
        try {
            Map<String, Object> resultadoEnvio = shippingClient.agendarEnvio(orderId);

            if (resultadoEnvio == null || !"OK".equals(resultadoEnvio.get("status"))) {
                log.warn("Shipping fallo para la orden {}, se libera stock", orderId);
                liberarInterno(orderId);
                return error("No se pudo agendar el envio para la orden " + orderId);
            }

            reserva.setStatus("CONFIRMED");
            reservationRepository.save(reserva);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("status", "OK");
            respuesta.put("reservationId", reserva.getId());
            respuesta.put("shippingId", resultadoEnvio.get("shippingId"));
            respuesta.put("stockRestante", producto.getStock());
            log.info("Reserva y envio confirmados para la orden {}", orderId);
            return respuesta;

        } catch (Exception e) {
            log.error("Error al agendar envio para la orden {}: {}", orderId, e.getMessage());
            liberarInterno(orderId);
            return error("Servicio de envio no disponible para la orden " + orderId);
        }
    }

    public Map<String, Object> liberarStock(Long orderId) {
        liberarInterno(orderId);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "OK");
        respuesta.put("orderId", orderId);
        respuesta.put("mensaje", "Stock liberado correctamente");
        return respuesta;
    }

    private void liberarInterno(Long orderId) {
        Optional<InventoryReservation> reservaOpt = reservationRepository.findByOrderId(orderId);
        if (reservaOpt.isEmpty()) {
            return;
        }

        InventoryReservation reserva = reservaOpt.get();
        if ("RELEASED".equals(reserva.getStatus())) {
            return;
        }

        Optional<InventoryProduct> productoOpt = productRepository.findByProductCode(reserva.getProductCode());
        if (productoOpt.isPresent()) {
            InventoryProduct producto = productoOpt.get();
            producto.setStock(producto.getStock() + reserva.getQuantity());
            productRepository.save(producto);
            log.info("Stock devuelto. Producto {}, cantidad {}. Stock ahora: {}",
                    reserva.getProductCode(), reserva.getQuantity(), producto.getStock());
        }

        reserva.setStatus("RELEASED");
        reservationRepository.save(reserva);
    }

    public void setFailMode(String productCode, boolean failMode) {
        Optional<InventoryProduct> productoOpt = productRepository.findByProductCode(productCode);
        if (productoOpt.isPresent()) {
            productoOpt.get().setFailMode(failMode);
            productRepository.save(productoOpt.get());
            log.info("Fail-mode del producto {} = {}", productCode, failMode);
        }
    }

    public InventoryProduct obtenerProducto(String productCode) {
        return productRepository.findByProductCode(productCode).orElse(null);
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ERROR");
        respuesta.put("mensaje", mensaje);
        return respuesta;
    }
}
