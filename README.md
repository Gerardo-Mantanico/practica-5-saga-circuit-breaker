# Práctica 5 — Saga Pattern vs Circuit Breaker Pattern

PoC de microservicios para un flujo de compra en e-commerce. Se implementan:

- **Saga Pattern** (coreografía simplificada con REST + compensaciones)
- **Circuit Breaker Pattern** (Resilience4j, 2 breakers, fallbacks)

## Arquitectura

```
Cliente
  │
  ▼
Order Service (8081)
  │  POST /orders
  ▼
Payment Service (8082)
  │  POST /payments
  ▼
Inventory Service (8083)
  │  POST /inventory/reserve
  ▼
Shipping Service (8084)
     POST /shipping/schedule
```

Si un paso falla, se ejecutan compensaciones hacia atrás:
reembolso (Payment), liberar stock (Inventory), cancelar orden (Order).

## Requisitos

- Java 21+
- Maven
- Docker (para PostgreSQL)

## Cómo ejecutar

### 1. Base de datos

```bash
cd practica5
docker compose up -d
```

Esto crea PostgreSQL con la BD `practica5`, usuario `practica5` y datos de prueba.

### 2. Servicios

```bash
./start-all.sh
```

O levanta cada uno en una terminal:

```bash
cd order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd shipping-service && mvn spring-boot:run
```

Espera unos 40-60 segundos a que arranquen.

### 3. Productos de prueba

| Código | Producto | Stock |
|--------|----------|-------|
| P001 | Teclado mecánico | 10 |
| P002 | Mouse gamer | 0 |

## Swagger UI

Cada microservicio tiene su propia documentación interactiva:

| Servicio | Swagger UI | OpenAPI JSON |
|----------|------------|--------------|
| Order | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Payment | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Inventory | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| Shipping | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |

## Pruebas

### Flujo feliz (Saga)

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"P001","quantity":1,"amount":150.0}'
```

Esperado: orden `CONFIRMED`, pago `CHARGED`, stock de P001 baja a 9, envío creado.

### Consultar estado

```bash
curl http://localhost:8081/orders
curl http://localhost:8082/payments
curl http://localhost:8083/inventory/P001
curl http://localhost:8084/shipping
```

### Fallo de Saga (sin stock)

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"P002","quantity":1,"amount":80.0}'
```

Esperado: orden `CANCELLED`, pago `REFUNDED`, mensaje de stock insuficiente.

### Fallo simulado (fail-mode)

```bash
# Activar fallo en P001
curl -X POST http://localhost:8083/inventory/fail-mode \
  -H "Content-Type: application/json" \
  -d '{"productId":"P001","failMode":true}'

# Intentar compra
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"P001","quantity":1,"amount":150.0}'

# Desactivar
curl -X POST http://localhost:8083/inventory/fail-mode \
  -H "Content-Type: application/json" \
  -d '{"productId":"P001","failMode":false}'
```

### Circuit Breaker

1. Con todo arriba: CB **Closed**, flujo funciona.
2. Detén inventory-service:
   ```bash
   # busca el proceso y mátalo, o reinicia solo inventory
   pkill -f inventory-service
   ```
3. Intenta comprar P001 varias veces → Payment falla, CB de inventario se **Open**.
4. Respuesta controlada: `Servicio de inventario no disponible, intente mas tarde`
5. Espera ~10 segundos, reinicia inventory-service → CB pasa a **Half-Open** y luego **Closed**.

Los cambios de estado se ven en los logs (`logs/*.log`).

## Circuit Breakers configurados

| Breaker | Ubicación | Parámetros |
|---------|-----------|------------|
| `paymentService` | Order → Payment | failureRateThreshold=50%, waitDurationInOpenState=10s, slidingWindowSize=10, permittedNumberOfCallsInHalfOpenState=3 |
| `inventoryService` | Payment → Inventory | mismos parámetros |

## Endpoints

### Order (8081)
- `POST /orders` — crear orden e iniciar saga
- `DELETE /orders/{id}` — cancelar orden
- `GET /orders` / `GET /orders/{id}`

### Payment (8082)
- `POST /payments` — procesar pago
- `POST /payments/{id}/refund` — reembolso
- `GET /payments` / `GET /payments/{id}`

### Inventory (8083)
- `POST /inventory/reserve` — reservar stock
- `POST /inventory/release` — liberar stock
- `POST /inventory/fail-mode` — simular fallo
- `GET /inventory/{productId}`

### Shipping (8084)
- `POST /shipping/schedule` — agendar envío
- `DELETE /shipping/{id}` — cancelar envío
- `GET /shipping` / `GET /shipping/{id}`

## Estructura

```
practica5/
├── docker-compose.yml
├── init-db.sql
├── start-all.sh
├── README.md
├── INFORME.md
├── order-service/      (8081)
├── payment-service/    (8082)
├── inventory-service/  (8083)
└── shipping-service/   (8084)
```

Cada servicio tiene su propia base de código Spring Boot, su propio Swagger y comparte la misma instancia PostgreSQL usando tablas separadas.

## Informe

Ver [INFORME.md](INFORME.md) para teoría, diseño, análisis de ventajas/desventajas y conclusiones.
