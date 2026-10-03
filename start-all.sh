#!/bin/bash
# Practica 5 - Saga y Circuit Breaker
# Compila (si hace falta) y levanta los 4 microservicios

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
LOGS="$DIR/logs"
mkdir -p "$LOGS"

echo "=== Practica 5: Saga Pattern vs Circuit Breaker ==="

if ! pg_isready -h localhost -p 5432 -U practica5 -d practica5 >/dev/null 2>&1; then
    echo "PostgreSQL no responde. Ejecuta primero:"
    echo "  docker compose up -d"
    exit 1
fi

echo "PostgreSQL OK."

compilar_si_falta() {
    local servicio=$1
    if [ ! -f "$DIR/$servicio/target/$servicio-1.0.0.jar" ]; then
        echo "Compilando $servicio..."
        (cd "$DIR/$servicio" && mvn -q -DskipTests package)
    fi
}

compilar_si_falta order-service
compilar_si_falta payment-service
compilar_si_falta inventory-service
compilar_si_falta shipping-service

iniciar() {
    local nombre=$1
    local puerto=$2
    if curl -s -m 1 -o /dev/null "http://localhost:$puerto/" 2>/dev/null; then
        echo "$nombre ya parece estar corriendo en el puerto $puerto"
        return
    fi
    echo "Iniciando $nombre (puerto $puerto)..."
    (cd "$DIR" && nohup java -jar "$nombre/target/$nombre-1.0.0.jar" \
        > "$LOGS/$nombre.log" 2>&1 &)
}

iniciar shipping-service 8084
iniciar inventory-service 8083
iniciar payment-service 8082
iniciar order-service 8081

echo ""
echo "Servicios lanzando en background."
echo "Logs: $LOGS/"
echo "Espera ~30-40 segundos y luego prueba:"
echo ""
echo "  curl http://localhost:8081/orders"
echo "  curl http://localhost:8083/inventory/P001"
echo ""
echo "Flujo feliz:"
echo '  curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\":\"P001\",\"quantity\":1,\"amount\":150.0}"'
echo ""
echo "Flujo con fallo (sin stock):"
echo '  curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\":\"P002\",\"quantity\":1,\"amount\":80.0}"'
