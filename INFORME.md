# Informe técnico — Práctica 5

## Saga Pattern vs Circuit Breaker Pattern

**Escenario:** flujo de compra en un e-commerce con 4 microservicios.

---

## 1. Introducción

En arquitecturas de microservicios, cada servicio suele tener su propia base de datos. Esto hace que no sea posible usar transacciones ACID tradicionales a través de varios servicios (el modelo 2PC es costoso, bloqueante y no escala bien).

Cuando una operación de negocio toca varios servicios —como crear una orden, cobrar, reservar stock y agendar envío— hace falta un mecanismo que mantenga la consistencia aunque algo falle en el camino.

En esta práctica se implementó un PoC con dos patrones complementarios:

1. **Saga Pattern** para coordinar la transacción distribuida y compensar si algo falla.
2. **Circuit Breaker Pattern** para no saturar un servicio caído y evitar fallos en cascada.

---

## 2. Saga Pattern

### 2.1 Problema

Si la orden se crea, el pago se cobra y luego el inventario no tiene stock, el sistema no puede quedarse en un estado inconsistente (orden activa + dinero cobrado sin producto). Se necesita deshacer los pasos que ya se hicieron.

### 2.2 Enfoques

| Enfoque | Descripción |
|---------|-------------|
| **Coreografía** | Cada servicio conoce al siguiente y publica/reacta a eventos. No hay un único dueño del flujo. |
| **Orquestación** | Un servicio central (orquestador) invoca a los demás y coordina compensaciones. |

### 2.3 Enfoque elegido

**Coreografía simplificada con REST.**

En este PoC, cada servicio llama al siguiente por HTTP cuando termina su paso:

1. **Order** crea la orden (`PENDING`) y llama a Payment.
2. **Payment** cobra y llama a Inventory.
3. **Inventory** reserva stock y llama a Shipping.
4. **Shipping** agenda el envío y devuelve OK hacia arriba.

No se usó broker de mensajes (Kafka/RabbitMQ) a propósito: para la práctica se prefirió una cadena REST visible y fácil de demostrar. En un entorno productivo, la coreografía real se haría con eventos.

### 2.4 Compensaciones

Si un paso falla, se ejecutan operaciones inversas:

| Paso fallido | Compensación |
|--------------|--------------|
| Shipping falla | Inventory libera el stock reservado |
| Inventory falla | Payment reembolsa (`REFUNDED`) |
| Payment falla o CB abierto | Order cancela la orden (`CANCELLED`) |

Las compensaciones son idempotentes en la medida de lo posible: por ejemplo, un pago solo se reembolsa si estaba en estado `CHARGED`.

### 2.5 Estados de la orden

```
PENDING → CONFIRMED   (saga completa)
PENDING → CANCELLED   (alguno de los pasos fallo)
```

---

## 3. Circuit Breaker Pattern

### 3.1 Problema

Si Inventory está caído y Payment sigue llamándolo en cada compra, las peticiones se acumulan, el hilo/tiempo de espera se agota y el fallo se propaga (caídas en cascada). El cliente además espera respuestas lentas o errores poco claros.

### 3.2 Los tres estados

| Estado | Comportamiento |
|--------|----------------|
| **Closed** | Las llamadas pasan normalmente. Se cuentan fallos. |
| **Open** | Se rechazan llamadas de inmediato y se ejecuta el fallback. |
| **Half-Open** | Se permiten unas pocas llamadas de prueba. Si funcionan, vuelve a Closed. |

### 3.3 Configuración usada

```yaml
failureRateThreshold: 50          # 50% de fallos abre el breaker
waitDurationInOpenState: 10s      # espera antes de ir a Half-Open
slidingWindowSize: 10             # ultimas 10 llamadas para calcular la tasa
permittedNumberOfCallsInHalfOpenState: 3
minimumNumberOfCalls: 5
automaticTransitionFromOpenToHalfOpenEnabled: true
```

### 3.4 Breakers implementados (2)

| Nombre | Quién lo usa | Servicio protegido |
|--------|--------------|--------------------|
| `paymentService` | Order Service | Payment Service |
| `inventoryService` | Payment Service | Inventory Service |

### 3.5 Fallbacks

Cuando el breaker está abierto o hay error de conexión:

- Order → Payment:  
  `"Servicio de pago no disponible, intente mas tarde"`
- Payment → Inventory:  
  `"Servicio de inventario no disponible, intente mas tarde"`

La saga no se cuelga: recibe el error, ejecuta compensaciones y responde al cliente.

### 3.6 Patrones complementarios (mención)

En la teoría se estudian también Retry, Timeout, Bulkhead y Fallback. En este PoC se usó principalmente **Fallback** + **Circuit Breaker**. Se podrían añadir reintentos o timeouts más agresivos, pero se evitó a propósito para no complicar el código de estudiante.

---

## 4. Diseño de la solución

### 4.1 Microservicios

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| order-service | 8081 | Crear/cancelar órdenes, iniciar saga |
| payment-service | 8082 | Cobrar y reembolsar |
| inventory-service | 8083 | Reservar/libre stock, fail-mode |
| shipping-service | 8084 | Agendar/cancelar envíos |

### 4.2 Comunicación

REST HTTP con `RestTemplate`. Cada servicio expone sus endpoints y llama al siguiente según la secuencia de la saga.

### 4.3 Datos

Una instancia de PostgreSQL (Docker) con tablas separadas por servicio:

- `orders`
- `payments`
- `inventory_products` / `inventory_reservations`
- `shipments`

Cada servicio solo accede a sus tablas (aislamiento lógico a nivel de aplicación).

### 4.4 Datos de prueba

| Producto | Stock | Uso |
|----------|-------|-----|
| P001 Teclado mecánico | 10 | Flujo feliz |
| P002 Mouse gamer | 0 | Fallo por falta de stock |

---

## 5. Implementación

### 5.1 Saga (coreografía)

**Flujo exitoso**

1. `POST /orders` con `P001`, cantidad 1.
2. Order guarda `PENDING` y llama a Payment.
3. Payment guarda pago `PENDING`, llama a Inventory.
4. Inventory descuenta stock, llama a Shipping.
5. Shipping crea el envío `SCHEDULED`.
6. Los servicios confirman hacia arriba: Payment `CHARGED`, Order `CONFIRMED`.

**Flujo con fallo (sin stock)**

1. Inventory detecta `stock < cantidad`.
2. Devuelve `status=ERROR` con mensaje de stock insuficiente.
3. Payment marca el pago `REFUNDED`.
4. Order cancela la orden (`CANCELLED`).
5. El cliente recibe la orden cancelada.

**Fail-mode**

El inventario tiene un endpoint `POST /inventory/fail-mode` que simula un error interno. Sirve para demostrar compensaciones sin tener que matar el servicio.

### 5.2 Circuit Breaker

En Order Service:

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPago")
public Map<String, Object> procesarPagoConCircuitBreaker(...) { ... }
```

En Payment Service:

```java
@CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackInventario")
public Map<String, Object> reservarStockConCircuitBreaker(...) { ... }
```

Configuración centralizada en `application.yml` de cada servicio.

### 5.3 Pruebas manuales realizadas

| Prueba | Resultado esperado | Resultado |
|--------|--------------------|-----------|
| Compra P001 | Orden CONFIRMED, stock baja | OK |
| Compra P002 | Orden CANCELLED, pago REFUNDED | OK |
| Fail-mode + compra P001 | Compensación completa | OK |
| Detener inventory + compras | CB Open + fallback | OK |
| Reiniciar inventory + compras | Half-Open → Closed | OK |

---

## 6. Análisis

### 6.1 Saga — ventajas

- Mantiene consistencia eventual entre servicios sin 2PC.
- Cada servicio puede desplegarse por separado.
- Las compensaciones hacen explícito qué hacer cuando algo falla.
- Funciona bien cuando los pasos del proceso de negocio son conocidos.

### 6.2 Saga — desventajas

- El código de compensaciones crece con cada paso nuevo.
- Puede haber estados intermedios visibles (orden PENDING, pago temporal).
- Sin broker, la coreografía con REST queda acoplada a la disponibilidad de los servicios.
- Difícil de depurar cuando hay varios fallos a la vez.
- No es una transacción ACID: es consistencia eventual.

### 6.3 Circuit Breaker — ventajas

- Evita que un servicio caído sature a los demás.
- Mejora la experiencia: respuesta rápida con fallback en lugar de timeout largo.
- Permite recuperación automática (Half-Open).
- Protege recursos (hilos, conexiones) del servicio consumidor.

### 6.4 Circuit Breaker — desventajas

- Hay que elegir umbrales (por qué 50% y no 30% o 70%).
- Si el breaker se abre “de más”, se rechazan llamadas que podrían haber funcionado.
- No arregla el servicio caído: solo lo aísla.
- Hay que monitorear los estados para detectar problemas reales.
- Los fallbacks pueden ocultar fallos si nadie mira los logs.

### 6.5 ¿Cuándo usar cada uno?

| Patrón | Cuándo aplicarlo |
|--------|------------------|
| **Saga** | Operaciones de negocio que cruzan varios servicios y necesitan deshacerse si algo falla (compras, transferencias, reservas). |
| **Circuit Breaker** | Cuando un servicio depende de otro que puede fallar o estar lento y no quiere sufrir fallos en cascada. |

En esta práctica **son complementarios**: la Saga coordina el proceso; el Circuit Breaker protege las llamadas entre servicios dentro de esa Saga.

---

## 7. Conclusiones

1. En microservicios no se puede confiar en una transacción ACID clásica entre bases de datos distintas; se usan sagas con compensaciones.
2. La coreografía simplificada con REST fue suficiente para demostrar el flujo y las compensaciones de forma visual.
3. El Circuit Breaker evitó que un inventario caído bloqueara o degradara el resto del proceso: se respondió rápido con un mensaje controlado.
4. La combinación de ambos patrones mejora la resiliencia: consistencia (Saga) + disponibilidad razonable (Circuit Breaker).
5. En un caso real se añadirían colas de mensajes, reintentos con backoff, timeouts estrictos y monitoreo de los estados del breaker.

---

## 8. Referencias

- Fowler, M. — *Pattern: Saga*
- Nyber, T. — *Circuit Breaker* (martinfowler.com)
- Resilience4j documentation — https://resilience4j.readme.io/
- Richardson, C. — *Microservices Patterns*
- Chris Richardson — *Microservices Patterns* (capítulos de Sagas)

---

## 9. Cómo reproducir las pruebas del video

1. `docker compose up -d`
2. `./start-all.sh`
3. Compra exitosa con P001
4. Compra fallida con P002
5. Activar fail-mode y repetir
6. Detener inventory-service y ver el Circuit Breaker abrirse
7. Reiniciar inventory y ver la recuperación
