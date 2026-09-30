package com.practica5.shipping.service;

import com.practica5.shipping.model.Shipment;
import com.practica5.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final ShipmentRepository repository;

    public ShippingService(ShipmentRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> agendarEnvio(Long orderId, String address) {
        Shipment envio = new Shipment();
        envio.setOrderId(orderId);
        envio.setAddress(address);
        envio.setStatus("SCHEDULED");
        envio = repository.save(envio);
        log.info("Envio {} agendado para la orden {}", envio.getId(), orderId);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "OK");
        respuesta.put("shippingId", envio.getId());
        respuesta.put("orderId", orderId);
        return respuesta;
    }

    public void cancelarEnvio(Long id) {
        Shipment envio = repository.findById(id).orElse(null);
        if (envio != null && !"CANCELLED".equals(envio.getStatus())) {
            envio.setStatus("CANCELLED");
            repository.save(envio);
            log.info("Envio {} cancelado", id);
        }
    }

    public Shipment obtenerEnvio(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Shipment> listarEnvios() {
        return repository.findAll();
    }
}
