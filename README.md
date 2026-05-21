# Sistema de Gestión de Stock

Aplicación multi-servicio en Spring Boot para gestionar productos e inventario. Construida como referencia práctica de patrones production-ready: JSON:API, versionado de API, seguridad, circuit breakers, rate limiting, migraciones de base de datos, observabilidad y más.

---

## Arquitectura

```
                    ┌─────────────────────────────┐
                    │   frontend (Vue 3 + Quasar)  │  puerto 9000
                    │        Purchase UI            │
                    └──────────────┬──────────────┘
                                   │ HTTP (proxy Vite)
                    ┌──────────────▼──────────────┐
┌───────────────────────────┐   ┌───────────────────────────┐
│     servicio products      │   │    servicio inventory      │
│        puerto 8082         │◄──│        puerto 8081         │
│     products_db (PG)       │HTTP│    inventory_db (PG)      │
└─────────────┬─────────────┘   └─────────────┬─────────────┘
              │                                │
              └──────────────┬─────────────────┘
                             │
                 ┌───────────▼──────────┐
                 │     PostgreSQL 17     │  puerto 5432
                 │  products_db          │
                 │  inventory_db         │
                 └───────────┬──────────┘
                             │
                 ┌───────────▼──────────┐
                 │      Prometheus       │  puerto 9090
                 └───────────┬──────────┘
                             │
                 ┌───────────▼──────────┐
                 │       Grafana         │  puerto 3000
                 └──────────────────────┘
```

El frontend consume directamente el servicio de inventario (puerto 8081) a través del proxy de Vite en desarrollo. Ambos servicios backend comparten una sola instancia de PostgreSQL con bases de datos separadas. El servicio de inventario llama al servicio de productos via HTTP para validar la existencia de productos y enriquecer las respuestas.

---

## Stack Tecnológico

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| PostgreSQL | 17 |
| Flyway | (gestionado por Spring Boot) |
| Resilience4j | (via Spring Cloud Circuit Breaker) |
| Micrometer + Prometheus | (via Spring Boot Actuator) |
| Grafana | latest |
| Lombok | (via Spring Boot parent) |
| Springdoc OpenAPI | 3.0.2 |

---

## Estructura del Proyecto

```
stock/
├── pom.xml                          # POM padre (multi-módulo)
├── docker-compose.yml               # Infra compartida: PostgreSQL, Prometheus, Grafana
├── prometheus.yml                   # Configuración de scraping de Prometheus
├── init-db.sh                       # Crea products_db e inventory_db
├── grafana/
│   └── provisioning/
│       └── datasources/
│           └── prometheus.yml       # Provisiona el datasource de Prometheus automáticamente
├── products/
│   ├── pom.xml
│   ├── local.env                    # Variables de entorno para ejecución local
│   └── src/main/
│       ├── java/com/rob/products/
│       │   ├── config/              # WebConfig (prefijo de ruta, tipo de media)
│       │   ├── controller/v1/       # ProductController
│       │   ├── dto/                 # ProductAttributes, ProductRequest, ProductResponse
│       │   ├── entity/              # Product
│       │   ├── exception/           # GlobalExceptionHandler, excepciones de dominio
│       │   ├── facade/              # Interfaz ProductFacade + implementación
│       │   ├── jsonapi/             # JsonApiDocument, JsonApiData, JsonApiError, etc.
│       │   ├── repository/          # ProductRepository (JPA)
│       │   ├── security/            # ApiKeyAuthFilter, ApiKeyProperties, SecurityConfig
│       │   └── service/             # ProductService
│       └── resources/
│           ├── application.properties
│           ├── logback-spring.xml
│           └── db/migration/
│               └── V1__create_products_table.sql
└── inventory/
    ├── pom.xml
    ├── local.env                    # Variables de entorno para BD y servicio products
    └── src/main/
        ├── java/com/rob/inventory/
        │   ├── client/              # ProductsClient (RestClient + Resilience4j)
        │   ├── config/              # ProductsClientConfig, WebConfig
        │   ├── controller/v1/       # InventoryController
        │   ├── dto/                 # InventoryAttributes, PurchaseAttributes, etc.
        │   ├── entity/              # Inventory
        │   ├── exception/           # GlobalExceptionHandler, excepciones de dominio
        │   ├── facade/              # Interfaz InventoryFacade + implementación
        │   ├── jsonapi/             # (misma estructura que products)
        │   ├── repository/          # InventoryRepository (con bloqueo pesimista)
        │   ├── security/            # ApiKeyAuthFilter, ApiKeyProperties, SecurityConfig
        │   └── service/             # InventoryService
        └── resources/
            ├── application.properties
            ├── logback-spring.xml
            └── db/migration/
                └── V1__create_inventory_table.sql
```

---

## Funcionalidades Implementadas

### 1. Especificación JSON:API

Todos los endpoints producen y consumen `application/vnd.api+json` siguiendo la especificación [JSON:API v1.0](https://jsonapi.org/).

**Estructura del cuerpo de una petición:**
```json
{
  "data": {
    "type": "products",
    "attributes": {
      "name": "Widget A",
      "price": 9.99,
      "description": "Un gran widget"
    }
  }
}
```

**Estructura del cuerpo de una respuesta:**
```json
{
  "data": {
    "type": "products",
    "id": "1",
    "attributes": {
      "name": "Widget A",
      "price": 9.99,
      "description": "Un gran widget"
    }
  }
}
```

**Estructura de respuesta de error:**
```json
{
  "errors": [
    {
      "status": "404",
      "title": "Product Not Found",
      "detail": "Product with id 99 not found"
    }
  ]
}
```

El paquete `jsonapi` en cada servicio contiene las clases reutilizables: `JsonApiDocument`, `JsonApiData`, `JsonApiMeta`, `JsonApiLinks`, `JsonApiError`, `JsonApiErrorDocument`, `JsonApiErrorSource` y la interfaz `JsonApiMapper<S, A>`.

---

### 2. Versionado de API (Prefijo en la URL)

Todos los controladores están bajo el prefijo `/v1/` sin necesidad de declararlo en cada `@RequestMapping`. Se configura en `WebConfig` usando `PathMatchConfigurer` de Spring MVC:

```java
configurer.addPathPrefix("/v1",
    c -> c.getPackageName().startsWith("com.rob.products.controller.v1"));
```

Para introducir una v2 basta con crear un controlador en el paquete `controller.v2` y registrar un nuevo prefijo, sin tocar nada del código v1.

**Endpoints disponibles:**
- `GET  /v1/products`               — listado paginado
- `GET  /v1/products/{id}`          — producto por ID
- `POST /v1/products`               — crear producto
- `GET  /v1/inventory/{productId}`  — inventario de un producto
- `POST /v1/inventory`              — crear o actualizar inventario (upsert)
- `POST /v1/inventory/purchase`     — realizar una compra (descuenta stock)

---

### 3. Seguridad — API Key via Bearer Token

La autenticación usa un `OncePerRequestFilter` personalizado que lee el header `Authorization: Bearer <clave>`, resuelve la clave a una lista de roles definida en `application.properties`, y construye un `UsernamePasswordAuthenticationToken` con autoridades con prefijo `ROLE_`.

```
Authorization: Bearer read-only-key
Authorization: Bearer admin-key
```

**Claves del servicio products:**

| Clave | Roles |
|---|---|
| `read-only-key` | `PRODUCTS_READ` |
| `admin-key` | `PRODUCTS_READ`, `PRODUCTS_WRITE` |

**Claves del servicio inventory:**

| Clave | Roles |
|---|---|
| `read-only-key` | `INVENTORY_READ` |
| `admin-key` | `INVENTORY_READ`, `INVENTORY_WRITE` |

Endpoints públicos (sin clave): `/actuator/health`, `/actuator/prometheus`, `/v3/api-docs/**`, `/swagger-ui/**`.

La misma clave `read-only-key` es usada internamente por el servicio de inventario cuando llama al servicio de productos, configurada via `app.products.api-key`.

---

### 4. Migraciones de Base de Datos — Flyway

La gestión del esquema usa Flyway en lugar de `hibernate.ddl-auto=create/update`. JPA está configurado en modo `validate` — verifica que el esquema coincide con las entidades al arrancar, y falla rápido si hay divergencias.

Los archivos de migración viven en `src/main/resources/db/migration/` con la convención `V{version}__{descripcion}.sql`.

**`products` — `V1__create_products_table.sql`:**
```sql
CREATE TABLE products (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(150)    NOT NULL UNIQUE,
    price       NUMERIC(12, 2)  NOT NULL,
    description TEXT
);
```

**`inventory` — `V1__create_inventory_table.sql`:**
```sql
CREATE TABLE inventory (
    product_id  BIGINT   PRIMARY KEY,
    quantity    INTEGER  NOT NULL CHECK (quantity >= 0)
);
```

`inventory.product_id` es a la vez la PK y la referencia a `products.id`. Al ser una relación estrictamente 1:1, no se necesita una clave sustituta adicional. El `CHECK (quantity >= 0)` garantiza stock no negativo a nivel de base de datos.

Para futuras migraciones sólo hace falta agregar un archivo `V2__...sql` — Flyway aplica las migraciones pendientes automáticamente al arrancar.

---

### 5. Patrón Facade

Cada servicio tiene una interfaz facade entre el controlador y la capa de servicio:

```
ProductController   →  ProductFacade   →  ProductService   →  ProductRepository
InventoryController →  InventoryFacade →  InventoryService →  InventoryRepository
                                                            →  ProductsClient
```

La interfaz facade (`ProductFacade`, `InventoryFacade`) desacopla el controlador de la implementación. Para introducir una v2 basta con crear un `ProductFacadeV2Impl` e inyectarlo en el nuevo controlador — el controlador v1 y la implementación v1 no se tocan.

---

### 6. Circuit Breaker y Reintentos (Resilience4j)

El servicio de inventario llama al servicio de productos via `ProductsClient`. La llamada está decorada con `@Retry` y `@CircuitBreaker` (el circuit breaker envuelve al retry, por lo que todos los intentos de retry cuentan como una sola llamada desde la perspectiva del circuit breaker).

**Configuración de Retry (instancia `products`):**

| Parámetro | Valor | Descripción |
|---|---|---|
| `max-attempts` | 3 | Hasta 3 intentos en total |
| `wait-duration` | 500ms | Espera entre intentos |
| `retry-exceptions` | `ResourceAccessException`, `HttpServerErrorException` | Errores de red y 5xx |
| `ignore-exceptions` | `HttpClientErrorException` | Errores 4xx no se reintentan |

**Configuración de Circuit Breaker (instancia `products`):**

| Parámetro | Valor | Descripción |
|---|---|---|
| `sliding-window-type` | COUNT_BASED | Evalúa las últimas N llamadas |
| `sliding-window-size` | 10 | Rastrea las últimas 10 llamadas |
| `failure-rate-threshold` | 50% | Se abre si el 50% de llamadas falla |
| `wait-duration-in-open-state` | 10s | Permanece abierto por 10s |
| `permitted-calls-in-half-open` | 3 | Prueba con 3 llamadas en half-open |
| `automatic-transition-to-half-open` | true | No requiere reset manual |

**Comportamiento del fallback en `ProductsClient`:**
- Si el servicio de productos devuelve **404** → lanza `ProductNotFoundException` (no se reintenta, no cuenta como falla para el circuit breaker)
- Si el servicio de productos **no está disponible** tras todos los reintentos → registra un warning y devuelve `null` (el upsert de inventario continúa sin datos del producto; la compra funciona igual porque el stock ya fue actualizado)

El estado del circuit breaker se expone via `/actuator/health`.

---

### 7. Rate Limiting (Resilience4j)

Todos los endpoints de los controladores están anotados con `@RateLimiter`. Cuando se supera el límite, Resilience4j lanza `RequestNotPermitted`, que el `GlobalExceptionHandler` captura y mapea a HTTP 429.

**Servicio products:**

| Instancia | Límite | Ventana |
|---|---|---|
| `products-read` | 100 req | 10s |
| `products-write` | 20 req | 10s |

**Servicio inventory:**

| Instancia | Límite | Ventana |
|---|---|---|
| `inventory-read` | 100 req | 10s |
| `inventory-write` | 20 req | 10s |

`timeout-duration=0s` significa que las peticiones que superan el límite fallan inmediatamente en lugar de ponerse en cola.

---

### 8. Decisión de Diseño — ¿En qué servicio vive el endpoint de compra?

Una compra podría implementarse en cualquiera de los dos servicios. La decisión de ubicarla en **inventory** se justifica desde tres ángulos:

#### Responsabilidad única (Single Responsibility)

El servicio `products` es el catálogo — gestiona la existencia, nombre, precio y descripción de un producto. No sabe cuántas unidades hay disponibles ni tiene acceso a esa información. El servicio `inventory` es el que posee la tabla `inventory` y el único con visibilidad sobre el stock. Una compra es esencialmente una **mutación de stock**: verificar disponibilidad, decrementar cantidad, registrar la operación. Todo eso vive naturalmente en `inventory`.

Mover el endpoint a `products` crearía una responsabilidad que no le pertenece: `products` tendría que llamar a `inventory` para leer el stock, luego volver a llamar para decrementarlo, convirtiéndose en un orquestador externo de datos que no le son propios.

#### Acoplamiento y dirección de dependencias

En la arquitectura actual, la dependencia va en una sola dirección:

```
inventory  →  products   (inventory llama a products para enriquecer respuestas)
products   ✗  inventory  (products no conoce inventory)
```

Si la compra viviera en `products`, se invertiría la dirección: `products` dependería de `inventory`. Ambos servicios se conocerían mutuamente, creando un **acoplamiento bidireccional** que dificulta el despliegue independiente, los tests y la evolución de cada servicio.

#### Patrón de propiedad de datos (Data Ownership)

En microservicios, la regla fundamental es que **un servicio es el único dueño de su base de datos**. `inventory_db` es propiedad exclusiva del servicio `inventory`. Ningún otro servicio debe leer ni escribir directamente en esa base de datos. Dado que una compra requiere escritura en `inventory_db` (decrementar `quantity`), solo `inventory` puede ejecutarla de forma segura y consistente — con transaccionalidad, bloqueos y validaciones controladas desde un único punto.

#### Resumen

| Criterio | Inventory ✓ | Products ✗ |
|---|---|---|
| Dueño de los datos de stock | Sí | No |
| Requiere acceso de escritura a `inventory_db` | Nativo | Vía llamada HTTP |
| Introduce acoplamiento bidireccional | No | Sí |
| Tiene el contexto para validar disponibilidad | Sí | No |
| Puede usar `@Lock` transaccional sobre su propia tabla | Sí | No |

---

### 9. Endpoint de Compra con Bloqueo Pesimista

`POST /v1/inventory/purchase` descuenta stock de forma atómica. Para evitar condiciones de carrera bajo compras concurrentes del mismo producto, la fila de inventario se obtiene con un bloqueo `SELECT FOR UPDATE`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByIdWithLock(@Param("productId") Long productId);
```

Flujo completo de una compra:
1. Adquirir bloqueo `PESSIMISTIC_WRITE` sobre la fila de inventario
2. Validar stock suficiente — lanza `InsufficientStockException` (HTTP 422) si no hay
3. Descontar cantidad y guardar
4. Obtener datos del producto del servicio de products para enriquecer la respuesta
5. Devolver resumen de la compra (nombre, precio unitario, cantidad, stock restante, total)

Casos de error:

| Escenario | HTTP |
|---|---|
| Producto sin registro de inventario | 404 `Inventory Not Found` |
| Stock insuficiente | 422 `Insufficient Stock` |
| Producto no existe en el servicio products | 404 `Product Not Found` |
| Límite de rate excedido | 429 `Too Many Requests` |
| Error de validación (ej. cantidad ≤ 0) | 400 `Validation Error` |

---

### 10. Semántica Upsert en Inventario

`POST /v1/inventory` crea o actualiza el inventario de un producto:

- Devuelve **HTTP 201** si el producto no tenía registro de inventario
- Devuelve **HTTP 200** si el registro ya existía y fue actualizado

Antes de guardar, el endpoint valida que el producto exista llamando a `ProductsClient.findById()`. Si el producto no existe, la petición se rechaza con 404 antes de cualquier escritura en BD.

---

### 11. Graceful Shutdown

Ambos servicios están configurados para apagado elegante:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

Al recibir `SIGTERM`, Spring Boot:
1. Deja de aceptar nuevas peticiones
2. Espera hasta 30 segundos para que terminen las peticiones en vuelo
3. Luego apaga el contexto de la aplicación

Esto evita cortar conexiones abruptamente durante despliegues o escalados hacia abajo.

---

### 12. Estrategias de Timeout

Los timeouts están configurados en cada capa para evitar bloqueos en cascada.

**RestClient — HTTP saliente (inventory → products):**

| Parámetro | Valor | Descripción |
|---|---|---|
| `app.products.connect-timeout` | 2s | Establecimiento de la conexión TCP |
| `app.products.read-timeout` | 3s | Espera del cuerpo de la respuesta |

Con 3 intentos y 500ms de espera entre ellos, el presupuesto máximo de tiempo para una cadena de llamadas fallidas es `3 × 3s + 2 × 500ms = 10s`.

**Resilience4j TimeLimiter (instancia `products`):**

| Parámetro | Valor |
|---|---|
| `timeout-duration` | 5s |
| `cancel-running-future` | true |

Aplica como límite superior cuando las llamadas se envuelven en `CompletableFuture`.

**HikariCP — pool de conexiones a BD:**

| Parámetro | Valor | Descripción |
|---|---|---|
| `connection-timeout` | 20000ms | Espera máxima para obtener una conexión del pool |
| `socketTimeout` (JDBC) | 30s | Espera máxima por el resultado de una query |

**Tomcat — conexiones entrantes:**

| Parámetro | Valor | Descripción |
|---|---|---|
| `server.tomcat.connection-timeout` | 5000ms | Tiempo para leer los headers de la petición tras aceptar TCP |

---

### 13. Observabilidad

#### Métricas — Prometheus + Micrometer

Spring Boot Actuator expone métricas en `/actuator/prometheus` usando el registry de Prometheus de Micrometer. Endpoints expuestos:

```
/actuator/health      — probes de liveness y readiness
/actuator/prometheus  — endpoint de scraping para Prometheus
/actuator/metrics     — nombres de todas las métricas disponibles
/actuator/info        — información de la aplicación
```

Las métricas incluyen: JVM, latencia de peticiones HTTP, pool de HikariCP, estado del circuit breaker, conteos de reintentos y eventos de rate limiter.

#### Trazabilidad — Micrometer Tracing + Brave

La dependencia `micrometer-tracing-bridge-brave` agrega `traceId` y `spanId` al MDC. Aparecen en cada línea de log via `%X{traceId}` y `%X{spanId}` en el patrón de Logback.

#### Logging — Logback

`logback-spring.xml` personalizado en cada servicio con:
- Salida con colores usando `ColorConverter` de Spring Boot (`%clr`)
- IDs de traza y span en cada línea: `[traceId, spanId]`
- Paquete `com.rob.*` en nivel `DEBUG` (código de la aplicación)
- `org.hibernate.SQL` en nivel `DEBUG` (queries SQL)
- Todo lo demás en nivel `INFO`

Formato de log:
```
2025-05-20 14:32:01.123  INFO 12345 --- [    main] [abc123,def456] c.r.i.service.InventoryService : ...
```

#### Swagger UI

Ambos servicios exponen documentación interactiva de la API:
- Products: `http://localhost:8082/swagger-ui.html`
- Inventory: `http://localhost:8081/swagger-ui.html`

El Swagger UI está preconfigurado con el esquema de autenticación `Bearer` para poder ingresar la API key directamente desde la interfaz.

---

## Levantando la Aplicación

### Prerrequisitos

- Java 21
- Maven 3.9+
- Docker y Docker Compose

### Paso 1 — Levantar la Infraestructura

Toda la infraestructura se levanta desde el `docker-compose.yml` en la raíz del proyecto. Inicia PostgreSQL, Prometheus y Grafana.

```bash
cd stock
docker compose up -d
```

Verificar que todos los contenedores estén corriendo:
```bash
docker compose ps
```

Salida esperada:
```
NAME         STATUS    PORTS
grafana      running   0.0.0.0:3000->3000/tcp
postgres     running   0.0.0.0:5432->5432/tcp
prometheus   running   0.0.0.0:9090->9090/tcp
```

El script `init-db.sh` se ejecuta automáticamente la primera vez que arranca PostgreSQL y crea ambas bases de datos:
```bash
# Crea:
#   products_db  — usada por el servicio products
#   inventory_db — usada por el servicio inventory
```

### Paso 2 — Compilar el Proyecto

Desde el directorio raíz `stock/`:
```bash
./mvnw clean package -DskipTests
```

### Paso 3 — Levantar el Servicio products

```bash
cd stock/products
export $(cat local.env | xargs)
../mvnw spring-boot:run
```

O con el JAR:
```bash
export $(cat local.env | xargs)
java -jar target/products-0.0.1-SNAPSHOT.jar
```

**Contenido de `local.env`:**
```properties
PRODUCTS_DB_URL=jdbc:postgresql://localhost:5432/products_db
PRODUCTS_DB_USERNAME=stock_user
PRODUCTS_DB_PASSWORD=stock_pass
```

Flyway se ejecuta automáticamente y aplica `V1__create_products_table.sql` la primera vez que arranca.

### Paso 4 — Levantar el Servicio inventory

```bash
cd stock/inventory
export $(cat local.env | xargs)
../mvnw spring-boot:run
```

**Contenido de `local.env`:**
```properties
INVENTORY_DB_URL=jdbc:postgresql://localhost:5432/inventory_db
INVENTORY_DB_USERNAME=stock_user
INVENTORY_DB_PASSWORD=stock_pass
PRODUCTS_BASE_URL=http://localhost:8082
PRODUCTS_API_KEY=read-only-key
```

Flyway aplica `V1__create_inventory_table.sql` la primera vez que arranca.

### Paso 5 — Verificar Health

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8081/actuator/health
```

Ambos deben devolver `{"status":"UP"}`.

---

## Verificar Prometheus

Abrir `http://localhost:9090` en el navegador.

### Verificar que los targets estén UP

Ir a **Status → Targets**. Deberían aparecer dos targets:
- `spring-boot-products` → `host.docker.internal:8082` → **UP**
- `spring-boot-inventory` → `host.docker.internal:8081` → **UP**

Si un target aparece como **DOWN**, verificar que el servicio esté corriendo y que `/actuator/prometheus` devuelva datos:
```bash
curl -H "Authorization: Bearer read-only-key" http://localhost:8082/actuator/prometheus | head -20
```

### Consultas de métricas de ejemplo

En la pestaña **Graph**, probar estas consultas PromQL:

```promql
# Tasa de peticiones HTTP (todos los endpoints)
rate(http_server_requests_seconds_count[1m])

# Percentil 95 de tiempo de respuesta
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Estado del circuit breaker (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="products"}

# Conexiones activas a la BD
hikaricp_connections_active

# Heap JVM usado
jvm_memory_used_bytes{area="heap"}
```

---

## Verificar Grafana

Abrir `http://localhost:3000` — credenciales: **admin / admin**.

### Paso 1 — Verificar el datasource

Ir a **Sidebar → Connections → Data sources**. Debe aparecer `Prometheus` ya configurado apuntando a `http://prometheus:9090` (provisionado automáticamente al levantar Docker).

Si no aparece, agregarlo manualmente:
1. **Add new data source** → seleccionar `Prometheus`
2. URL: `http://prometheus:9090`
3. Clic en **Save & test**

### Paso 2 — Importar el dashboard oficial de Spring Boot

1. **Sidebar → Dashboards → New → Import**
2. En el campo **Import via grafana.com**, escribir el ID **`19004`** → **Load**
3. Seleccionar el datasource `Prometheus`
4. Clic en **Import**

El dashboard muestra de inmediato:

| Panel | Qué mide |
|---|---|
| HTTP requests | Rate de peticiones, errores y latencia por endpoint |
| JVM heap / non-heap | Uso de memoria de la JVM |
| CPU y threads | Carga del proceso y threads activos |
| GC activity | Frecuencia y duración de garbage collection |
| HikariCP | Tamaño del pool, conexiones activas y tiempo de espera |

> **Ajustar el contexto del dashboard:** los paneles usan la variable `$application` para filtrar por servicio. Al entrar al dashboard, en la parte superior aparece un selector — elige `products` o `inventory` para ver las métricas del servicio correspondiente. Si los paneles aparecen vacíos, verifica que el label `application` esté presente en las métricas usando esta query en Prometheus:
> ```promql
> http_server_requests_seconds_count
> ```
> y confirma que las series incluyen el label `application="products"` o `application="inventory"`.

### Crear un panel personalizado

1. Clic en **+ → New Dashboard → Add visualization**
2. Seleccionar el datasource `Prometheus`
3. Ingresar una consulta PromQL, por ejemplo:
   ```promql
   rate(http_server_requests_seconds_count{job="spring-boot-products"}[1m])
   ```
4. Asignar título al panel y guardar

---

## Ejemplos de Uso de la API

Todas las peticiones requieren el header `Authorization: Bearer <clave>`. Usar `admin-key` para operaciones de escritura y `read-only-key` para lectura.

### Products

**Crear un producto:**
```bash
curl -s -X POST http://localhost:8082/v1/products \
  -H "Authorization: Bearer admin-key" \
  -H "Content-Type: application/vnd.api+json" \
  -d '{
    "data": {
      "type": "products",
      "attributes": {
        "name": "Widget A",
        "price": 9.99,
        "description": "Un gran widget"
      }
    }
  }' | jq
```

**Listar productos (paginado):**
```bash
curl -s "http://localhost:8082/v1/products?page=0&size=10" \
  -H "Authorization: Bearer read-only-key" | jq
```

**Obtener un producto por ID:**
```bash
curl -s http://localhost:8082/v1/products/1 \
  -H "Authorization: Bearer read-only-key" | jq
```

### Inventory

**Establecer inventario para un producto (upsert):**
```bash
curl -s -X POST http://localhost:8081/v1/inventory \
  -H "Authorization: Bearer admin-key" \
  -H "Content-Type: application/vnd.api+json" \
  -d '{
    "data": {
      "type": "inventory",
      "attributes": {
        "productId": 1,
        "quantity": 100
      }
    }
  }' | jq
```

Devuelve `201 Created` la primera vez, `200 OK` en llamadas posteriores.

**Consultar inventario de un producto:**
```bash
curl -s http://localhost:8081/v1/inventory/1 \
  -H "Authorization: Bearer read-only-key" | jq
```

**Realizar una compra (descuenta stock):**
```bash
curl -s -X POST http://localhost:8081/v1/inventory/purchase \
  -H "Authorization: Bearer admin-key" \
  -H "Content-Type: application/vnd.api+json" \
  -d '{
    "data": {
      "type": "purchases",
      "attributes": {
        "productId": 1,
        "quantity": 5
      }
    }
  }' | jq
```

La respuesta incluye `productName`, `unitPrice`, `quantityPurchased`, `remainingStock` y `totalPrice`.

---

## Frontend — Purchase UI

Aplicación web construida con **Vue 3 + Quasar 2 + Vite** que consume los endpoints del servicio de inventario.

### Estructura

```
frontend/
├── package.json
├── quasar.config.js          # Config de Quasar + proxy de dev hacia localhost:8081
├── index.html
├── src/
│   ├── App.vue
│   ├── boot/
│   │   └── axios.js          # Instancia de axios compartida
│   ├── css/
│   │   ├── app.scss
│   │   └── quasar.variables.scss
│   ├── layouts/
│   │   └── MainLayout.vue
│   ├── pages/
│   │   └── PurchasePage.vue  # Página principal
│   └── router/
│       ├── index.js
│       └── routes.js
```

### Levantar el frontend

```bash
cd stock/frontend
npm install
npm run dev
```

La app queda disponible en `http://localhost:9000`.

> El servicio de inventario (`http://localhost:8081`) debe estar corriendo. El proxy de Vite configurado en `quasar.config.js` redirige todas las peticiones `/v1/*` al backend, evitando problemas de CORS en desarrollo.

### Pantalla principal — `PurchasePage`

La página tiene dos secciones:

**1. Consultar inventario** — `GET /v1/inventory/{productId}`

- Campo de ID de producto + botón Consultar
- Muestra nombre del producto, precio y stock disponible
- Stock menor a 10 unidades se resalta en amarillo con ícono de advertencia
- Errores (producto no encontrado, etc.) se muestran dentro del mismo card

**2. Nueva compra** — `POST /v1/inventory/purchase`

- Campos: ID del producto, cantidad y API Key (default `admin-key`)
- Envía el body en formato JSON:API con `Content-Type: application/vnd.api+json`
- En éxito muestra: nombre, precio unitario, cantidad comprada, total pagado y stock restante
- Después de una compra exitosa, si el producto consultado en la sección de arriba coincide con el comprado, el inventario se refresca automáticamente
- Los errores del backend (404 producto no encontrado, 422 stock insuficiente, 429 rate limit) se muestran con el título y detalle del JSON:API error

### Proxy de desarrollo

El `quasar.config.js` configura Vite para que las peticiones a `/v1/*` apunten al backend sin necesidad de configurar CORS:

```javascript
devServer: {
  proxy: {
    '/v1': {
      target: 'http://localhost:8081',
      changeOrigin: true
    }
  }
}
```

---

## Áreas de Mejora

### Centralizar las clases reutilizables de JSON:API

Actualmente cada servicio tiene su propia copia del paquete `jsonapi`:

```
products/src/main/java/com/rob/products/jsonapi/
  JsonApiData.java
  JsonApiDocument.java
  JsonApiError.java
  JsonApiErrorDocument.java
  JsonApiErrorSource.java
  JsonApiLinks.java
  JsonApiMapper.java
  JsonApiMeta.java

inventory/src/main/java/com/rob/inventory/jsonapi/
  (mismas clases duplicadas)
```

Estas clases son idénticas entre servicios y no contienen lógica de negocio. La duplicación implica que cualquier cambio al contrato JSON:API (nuevo campo, nueva convención de errores, cambio en la serialización) debe aplicarse en ambos sitios manualmente, con riesgo de inconsistencia.

**Solución propuesta:** extraer estas clases a un módulo Maven compartido dentro del mismo multi-módulo:

```
stock/
├── pom.xml                  ← agregar módulo "common"
├── common/
│   └── src/main/java/com/rob/common/jsonapi/
│       ├── JsonApiData.java
│       ├── JsonApiDocument.java
│       ├── JsonApiError.java
│       ├── JsonApiErrorDocument.java
│       ├── JsonApiErrorSource.java
│       ├── JsonApiLinks.java
│       ├── JsonApiMapper.java
│       └── JsonApiMeta.java
├── products/
│   └── pom.xml              ← agrega dependencia a "common"
└── inventory/
    └── pom.xml              ← agrega dependencia a "common"
```

Cada servicio declara la dependencia interna:

```xml
<dependency>
    <groupId>com.rob</groupId>
    <artifactId>common</artifactId>
    <version>${project.version}</version>
</dependency>
```

Y los paquetes propios (`com.rob.products.jsonapi`, `com.rob.inventory.jsonapi`) desaparecen, reemplazados por `com.rob.common.jsonapi`.

**Consideración:** este patrón introduce un acoplamiento de compilación entre servicios a través del módulo `common`. Es aceptable mientras el módulo contenga únicamente DTOs y contratos de API sin dependencias de Spring ni lógica de negocio. Si los servicios evolucionan hacia repositorios separados (por ejemplo en un monorepo con builds independientes), la alternativa es publicar `common` como artefacto versionado en un repositorio Maven privado.

---

## Detener la Aplicación

Detener los servicios con `Ctrl+C` (el graceful shutdown drenará las peticiones en vuelo durante hasta 30s).

Detener la infraestructura:
```bash
cd stock
docker compose down
```

Para eliminar también los datos almacenados (PostgreSQL + dashboards de Grafana):
```bash
docker compose down -v
```