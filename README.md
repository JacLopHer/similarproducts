# Similar Products API

API REST desarrollada con **Spring Boot** y **arquitectura hexagonal multimodulo** que expone un endpoint para obtener productos similares a uno dado, consumiendo servicios externos.

El servicio implementa **caching inteligente con Redis** (TTL 10 minutos) para optimizar llamadas a APIs externas, manejo robusto de errores, y versionado de API.

---

## Descripción

Dado un `productId`, el servicio:
1. Consulta una API externa para obtener los IDs de productos similares (con caché Redis).
2. Para cada ID similar, obtiene los detalles del producto desde otra API externa (con caché Redis).
3. Devuelve la lista de productos similares con sus detalles completos.

**Endpoint principal versionado:**
```
GET /v1/product/{productId}/similar
```

---

## Arquitectura Hexagonal (Ports & Adapters)

El proyecto sigue la **arquitectura hexagonal (Ports & Adapters)** organizada en **4 módulos Maven independientes**:

```
similarproducts/                          ← Parent POM
├── similarproducts-domain/               ← Núcleo del negocio (sin dependencias externas)
├── similarproducts-application/          ← Use cases y orquestación
├── similarproducts-infrastructure/       ← Adapters HTTP, controllers, Swagger, Redis
└── similarproducts-boot/                 ← Punto de entrada Spring Boot
```

### Diagrama de Flujo (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────────┐
│                    REST CLIENT (Externo)                        │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP GET /v1/product/1/similar
                         ▼
        ┌────────────────────────────────────┐
        │   SimilarProductsController         │
        │   (Adapter IN - REST)               │
        │   @RestController @RequestMapping   │
        │   /v1/product/{productId}/similar   │
        └────────────────┬────────────────────┘
                         │ Valida & invoca
                         ▼
        ┌────────────────────────────────────┐
        │  GetSimilarProductsService          │
        │  (Application Service)              │
        │  orquesta Use Case + DTOs           │
        └────────────────┬────────────────────┘
                         │ Invoca
                         ▼
        ┌────────────────────────────────────┐
        │  GetSimilarProductsUseCase          │
        │  (Domain Service)                   │
        │  Llama a puertos                    │
        └──────┬──────────────────┬───────────┘
               │                  │
         Llama │              Llama│
               ▼                  ▼
    ┌──────────────────┐  ┌─────────────────┐
    │ SimilarIdsPort   │  │ProductDetailPort│
    │ (Puerto/Interface)  │ (Puerto/Interface)
    └────────┬─────────┘  └────────┬────────┘
             │                     │
    Implementado por           Implementado por
             │                     │
    ┌────────▼─────────┐  ┌────────▼────────┐
    │ SimilarIds       │  │ProductDetail    │
    │ Adapter (OUT)    │  │Adapter (OUT)    │
    │ @Cacheable       │  │@Cacheable       │
    │ Redis TTL:10min  │  │Redis TTL:10min  │
    └────────┬─────────┘  └────────┬────────┘
             │                     │
             │ HTTP GET            │ HTTP GET
             │ /similar-ids        │ /product/{id}
             │                     │
             └──────┬──────────────┘
                    ▼
        ┌───────────────────────────┐
        │  APIs Externas (Mock)     │
        │  localhost:3001           │
        └───────────────────────────┘
```

### Dependencias de Módulos (Jerarquía)

```
                    boot
                     ↓
            infrastructure
               ↓          ↓
          application  (también depende)
               ↓          ↓
             domain  ←─────
```

**Regla de oro:** `domain` NUNCA importa de `application` ni `infrastructure`. El flujo es unidireccional hacia adentro.

### Responsabilidad de cada módulo

| Módulo | Responsabilidad | Dependencias | ¿Spring? |
|--------|-----------------|--------------|----------|
| `similarproducts-domain` | Modelos, puertos (interfaces), excepciones de negocio, lógica pura | Solo Java stdlib | **NO** ❌ |
| `similarproducts-application` | Use cases, DTOs, mappers, orquestación de puertos | `domain` | **NO** ❌ |
| `similarproducts-infrastructure` | Controllers REST, clientes HTTP, Swagger, Redis caching, exception handlers | `domain` + `application` + Spring | **SÍ** ✅ |
| `similarproducts-boot` | Main class, configuración Spring Boot, punto de entrada | Todos los módulos | **SÍ** ✅ |

### Estructura de Paquetes Detallada

```
similarproducts-domain/                           (NO Spring)
└── com.example.similarproducts.domain/
    ├── model/
    │   ├── Product.java                          # Entidad de dominio (record/immutable)
    │   ├── SimilarProductsRequest.java
    │   └── SimilarProductsResponse.java
    ├── port/
    │   ├── SimilarIdsPort.java                   # Puerto: List<String> getSimilarIds(productId)
    │   └── ProductDetailPort.java                # Puerto: Product getProductDetail(productId)
    ├── service/
    │   └── GetSimilarProductsUseCase.java        # Orquestación de puertos, lógica pura
    └── exception/
        ├── ProductNotFoundException.java         # Excepciones de negocio
        └── InvalidProductIdException.java

similarproducts-application/                      (NO Spring)
└── com.example.similarproducts.application/
    ├── dto/
    │   ├── ProductDetailDto.java                 # DTO para HTTP response
    │   └── SimilarProductsResponseDto.java
    ├── mapper/
    │   └── ProductMapper.java                    # Domain model → DTO
    ├── service/
    │   └── GetSimilarProductsService.java        # Orquesta use case + mapeos
    └── config/
        └── ApplicationConfig.java                # Configuración de beans

similarproducts-infrastructure/                   (CON Spring)
└── com.example.similarproducts.infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── rest/
    │   │   │   ├── SimilarProductsController.java    # @RestController, /v1/product/{id}/similar
    │   │   │   ├── GlobalExceptionHandler.java       # @ControllerAdvice, mapea excepciones
    │   │   │   └── dto/
    │   │   │       └── ErrorResponseDto.java
    │   │   └── config/
    │   │       └── SwaggerConfig.java                # OpenAPI/Swagger documentation
    │   └── out/
    │       ├── client/
    │       │   ├── SimilarIdsAdapter.java            # Impl de SimilarIdsPort
    │       │   │                                      # + @Cacheable("similar-ids")
    │       │   │                                      # + Logs cache HIT/MISS
    │       │   └── ProductDetailAdapter.java         # Impl de ProductDetailPort
    │       │                                          # + @Cacheable("product-detail")
    │       │                                          # + Logs cache HIT/MISS
    │       └── config/
    │           └── HttpClientConfig.java
    ├── config/
    │   ├── InfrastructureConfig.java             # Beans de infraestructura
    │   ├── CacheConfig.java                      # @EnableCaching, Redis config
    │   │                                           # TTL = 600 seg (10 min)
    │   └── HttpClientConfig.java                 # RestTemplate, timeouts
    └── mapper/
        └── AdapterMapper.java                    # DTO ↔ Domain model

similarproducts-boot/                            (CON Spring)
└── com.example.similarproducts/
    ├── SimilarProductsApplication.java           # @SpringBootApplication
    └── src/main/resources/
        ├── application.yml                       # Config principal
        └── application-dev.yml                   # Config de desarrollo
```

---

## Caching con Redis

### Estrategia de Caching

El servicio cachea las respuestas de APIs externas en **Redis** con un **TTL (Time To Live) de 10 minutos** para optimizar performance y reducir carga en servicios externos.

### Cache Keys (Sin Versionado)

Las claves de caché **no incluyen versionado de API** (v1, v2, etc.) porque el versionado es solo a nivel de URL/routing:

```
similar-ids:{productId}                    # Ej: similar-ids:123
product-detail:{productId}                 # Ej: product-detail:456
```

### Ubicación del Caching

El caching se implementa en los **Adapters OUT** (clientes HTTP) usando la anotación `@Cacheable`:

```java
// SimilarIdsAdapter.java
@Cacheable(value = "similar-ids", key = "#productId")
public List<String> getSimilarIds(String productId) {
    // ... llamada HTTP a API externa
}

// ProductDetailAdapter.java  
@Cacheable(value = "product-detail", key = "#productId")
public Product getProductDetail(String productId) {
    // ... llamada HTTP a API externa
}
```

### Configuración de Redis

En `application.yml`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 5000
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutos en milisegundos
```

### Monitoreo de Cache

Los adapters generan logs DEBUG indicando hits/misses:

```
DEBUG com.example.similarproducts.infrastructure.adapter.out.client.SimilarIdsAdapter
      - Cache HIT: similar-ids:123

DEBUG com.example.similarproducts.infrastructure.adapter.out.client.ProductDetailAdapter  
      - Cache MISS: product-detail:456, fetching from API
```

Para inspeccionar caché en Redis directamente:

```bash
redis-cli
> KEYS "similar-ids:*"
> TTL similar-ids:123
> GET similar-ids:123
> INFO stats  # Ver global cache hits/misses
```

### Sin Invalidación Manual

El caché expira automáticamente tras 10 minutos. **No hay invalidación manual** - es estrategia simple y robusta para este caso de uso.

---

## Requisitos

- **Java 17+**
- **Maven 3.8+**
- **Docker & Docker Compose** (para ejecución con contenedores y Redis)
- Servicios externos corriendo en `localhost:3001` (o configurables en `application.yml`)

---

## Quick Start

### 1. Compilar el proyecto multimodulo

```bash
mvn clean install
```

Esto compila en orden: `domain` → `application` → `infrastructure` → `boot`

### 2. Ejecutar localmente

#### Opción A: Maven plugin (sin Docker)

```bash
# Asegúrate de tener Redis corriendo localmente
# docker run -d -p 6379:6379 redis:7-alpine

mvn -pl similarproducts-boot spring-boot:run
```

#### Opción B: JAR ejecutable

```bash
java -jar similarproducts-boot/target/similarproducts-boot-1.0-SNAPSHOT.jar
```

#### Opción C: Docker Compose (recomendado)

```bash
# Levanta aplicación + Redis + servicios mock
docker-compose up
```

La aplicación estará disponible en: **http://localhost:5000**

Swagger UI: **http://localhost:5000/swagger-ui.html**

---

## Testing

### Tests Unitarios (todos los módulos)

```bash
mvn test
```

Ejecuta todos los tests de los 4 módulos.

### Tests de un módulo específico

```bash
# Domain tests
mvn -pl similarproducts-domain test

# Application tests  
mvn -pl similarproducts-application test

# Infrastructure tests (includes controller + adapter tests)
mvn -pl similarproducts-infrastructure test
```

### Tests con Reporte de Cobertura (JaCoCo)

```bash
mvn clean test jacoco:report
```

Genera reportes HTML en `{módulo}/target/site/jacoco/index.html`

**Mínimos de cobertura por módulo:**
- `similarproducts-domain`: >90%
- `similarproducts-application`: >80%  
- `similarproducts-infrastructure`: >75%

### Tests de Caching con Redis

#### Tests Unitarios (con Mock)

```bash
mvn -pl similarproducts-infrastructure test -Dtest=*CacheTest
```

Tests que validan:
- Primera llamada a adapter → Redis MISS, llama API
- Segunda llamada con mismo productId → Redis HIT, no llama API
- Caché es distinto por productId
- Caché no se aplica en caso de error

#### Tests E2E (con Redis Real)

```bash
# Levanta Redis y servicios mock
docker-compose up -d redis simulado

# Ejecuta tests E2E
mvn verify -P e2e
```

Tests que validan:
- Primera llamada mide tiempo T1 (sin caché)
- Segunda llamada mide tiempo T2 (T2 < T1 debido a caché)
- Cache expira después de 10 minutos

### Tests E2E contra Mock APIs

```bash
# Opción 1: Con servicios levantados
docker-compose up -d simulado
mvn verify -P e2e

# Opción 2: Con Docker Compose todo integrado
docker-compose -f docker-compose.test.yaml up --abort-on-container-exit
```

**Casos de test E2E:**
1. ✅ Producto válido con similares → 200 OK
2. ✅ Producto válido sin similares → 200 OK (array vacío)
3. ✅ Producto no encontrado → 404 Not Found
4. ✅ ProductId inválido → 400 Bad Request
5. ✅ Servicio externo no disponible → 500 Internal Server Error

### Tests de Performance (k6)

```bash
docker-compose run --rm k6 run scripts/test.js
```

O con configuraciones específicas:

```bash
# Load test (100 usuarios, 10 minutos)
docker-compose run --rm k6 run scripts/load-test.js

# Spike test  
docker-compose run --rm k6 run scripts/spike-test.js

# Soak test
docker-compose run --rm k6 run scripts/soak-test.js
```

---

## API Reference

### GET /v1/product/{productId}/similar

Obtiene la lista de productos similares al producto indicado.

**Parámetros:**

| Nombre | Tipo | Ubicación | Descripción | Validación |
|--------|------|-----------|-------------|-----------|
| `productId` | `string` | Path | ID del producto base | No vacío, formato numérico recomendado |

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200 OK` | Lista de productos similares (puede ser vacía `[]`) |
| `400 Bad Request` | `productId` vacío o con formato inválido |
| `404 Not Found` | Producto no encontrado en APIs externas |
| `500 Internal Server Error` | Error interno o servicio externo no disponible |

**Ejemplo de respuesta exitosa (200 OK):**

```json
[
  {
    "id": "2",
    "name": "Dress",
    "price": 19.99,
    "availability": true
  },
  {
    "id": "3",
    "name": "Blazer",
    "price": 29.99,
    "availability": false
  }
]
```

**Ejemplo de error (404 Not Found):**

```json
{
  "error": "ProductNotFoundException",
  "message": "Product with id '999' not found",
  "timestamp": "2024-07-10T14:23:45.123Z"
}
```

### Swagger UI / OpenAPI

Documentación interactiva y ejecutable:

```
http://localhost:5000/swagger-ui.html
```

O descargar OpenAPI JSON:

```
http://localhost:5000/v3/api-docs
```

---

## Configuración

### application.yml (Principal)

Ubicación: `similarproducts-boot/src/main/resources/application.yml`

```yaml
server:
  port: 5000
  servlet:
    context-path: /

# APIs externas
external:
  api:
    base-url: http://localhost:3001
    timeout:
      connect: 5000      # 5 segundos
      read: 10000        # 10 segundos

# Redis caching
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutos

# Logging
logging:
  level:
    com.example.similarproducts: DEBUG
    com.example.similarproducts.infrastructure.adapter.out: DEBUG
```

### application-dev.yml (Desarrollo)

Para desarrollo local, utiliza overrides:

```yaml
external:
  api:
    base-url: http://localhost:3001

spring:
  data:
    redis:
      host: localhost

logging:
  level:
    root: INFO
    com.example.similarproducts: DEBUG
```

---

## Decisiones Técnicas

### ¿Por qué Arquitectura Multimodulo?

✅ **Separación de concerns explícita** - El compilador Maven fuerza dependencias correctas
✅ **Domain agnóstico de frameworks** - Puede reutilizarse en otros proyectos (CLI, gRPC, etc.)
✅ **Tests independientes** - Cada módulo testeable sin dependencias de otros
✅ **Escalabilidad** - Agregar nuevos adapters IN/OUT sin tocar el dominio
✅ **Mantenibilidad** - Arquitectura hexagonal clara y verificable

**Sin multimodulo (monolítico):**
- ❌ Difícil prevenir dependencias circulares
- ❌ Domain puede "contaminarse" con Spring sin darse cuenta
- ❌ Más difícil reutilizar en otros contextos

### ¿Por qué RestTemplate en lugar de WebClient?

✅ **RestTemplate:**
- Síncrono, fácil de entender
- Suficiente para este caso de uso (llamadas secuenciales)
- Menos complejidad conceptual

❌ **WebClient (Reactive):**
- Asíncrono, curva de aprendizaje más alta
- Overkill para este flujo simple

**Decisión:** Usar RestTemplate ahora, migración a WebClient es sencilla gracias al desacoplamiento de puertos (solo cambiar `ProductDetailAdapter`).

### Manejo de Errores y Excepciones

**Flujo:**

```
API externa falla
    ↓
HTTP client lanza exception (HttpClientErrorException, TimeoutException, etc.)
    ↓
Adapter OUT captura y lanza ProductNotFoundException (excepción de dominio)
    ↓
GlobalExceptionHandler captura y mapea:
  - ProductNotFoundException → 404 Not Found
  - InvalidProductIdException → 400 Bad Request
  - Exception → 500 Internal Server Error
    ↓
Response JSON consistente con { error, message, timestamp }
```

**Ventaja:** Las excepciones de dominio son agnósticas del HTTP - pueden usarse en CLI, gRPC, etc.

### Validación de Entrada

- `productId` se valida en controller con `@NotBlank` (Spring Validation)
- Formato esperado: string no vacío
- Opcional: agregar `@Pattern(regexp = "^\\d+$")` si es estrictamente numérico

### Versionado de API

- **Patrón:** `/v1/product/{productId}/similar`
- **Scope:** Solo a nivel URL/routing en infrastructure
- **NO afecta:** Domain, application, ni cache keys
- **Futuro:** Facilita agregar `/v2/...` en futuro sin cambiar lógica

### Redis Caching

**Beneficios:**
- ✅ Reduce latencia (cache hit ~1ms vs API ~100-500ms)
- ✅ Reduce carga en APIs externas
- ✅ Mejora throughput bajo concurrencia

**Implementación:**
- TTL 10 minutos (configurable en `application.yml`)
- Keys sin versionado: `similar-ids:productId`, no `v1:similar-ids:productId`
- Logs DEBUG para monitoreo de hits/misses
- Sin invalidación manual - expira automáticamente

### Logging y Observabilidad

- **SLF4J + Logback** (incluido con Spring Boot)
- **Niveles:**
  - `INFO`: Inicio de aplicación, cambios importantes
  - `DEBUG`: Detalles de llamadas HTTP, cache hits/misses
  - `ERROR`: Excepciones y errores

**Logs típicos:**

```
INFO  SimilarProductsApplication : Starting application...
INFO  SimilarProductsApplication : Server running on port 5000

DEBUG SimilarIdsAdapter : Cache MISS: similar-ids:1, fetching from API http://localhost:3001
DEBUG SimilarIdsAdapter : Response from API: [2, 3, 5]
DEBUG SimilarIdsAdapter : Caching result for similar-ids:1 (TTL: 600s)

DEBUG SimilarIdsAdapter : Cache HIT: similar-ids:1, returning cached value
```

---

## Verificación de Arquitectura Hexagonal

Para asegurar que la arquitectura hexagonal se respeta:

### 1. Verificar NO hay dependencias circulares

```bash
mvn dependency:tree
```

Debe mostrar jerarquía clara sin ciclos.

### 2. Verificar `domain` NO tiene Spring

```bash
mvn -pl similarproducts-domain dependency:tree | grep spring
# No debe devolver nada
```

### 3. Verificar `application` NO depende de `infrastructure`

```bash
mvn -pl similarproducts-application dependency:tree | grep infrastructure
# No debe devolver nada
```

### 4. Generar árbol de dependencias en archivo

```bash
mvn dependency:tree -DoutputFile=dependencies.txt
cat dependencies.txt
```

### 5. Validar estructura de imports (manual)

- ✅ `domain` → importa solo Java stdlib
- ✅ `application` → importa `domain`
- ✅ `infrastructure` → importa `domain` + `application` + Spring
- ✅ `boot` → importa todo

---

## Estructura Completa de Directorios

```
similarproducts/
├── pom.xml                                   ← Parent POM (gestión de versiones)
├── README.md                                 ← Esta documentación
├── .gitignore
├── Dockerfile
├── docker-compose.yaml
│
├── similarproducts-domain/                   ← Domain module (NO Spring)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/similarproducts/domain/
│       │   ├── model/
│       │   ├── port/
│       │   ├── service/
│       │   └── exception/
│       └── test/java/com/example/similarproducts/domain/
│
├── similarproducts-application/              ← Application module (NO Spring)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/similarproducts/application/
│       │   ├── dto/
│       │   ├── mapper/
│       │   ├── service/
│       │   └── config/
│       └── test/java/com/example/similarproducts/application/
│
├── similarproducts-infrastructure/           ← Infrastructure module (CON Spring)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/similarproducts/infrastructure/
│       │   ├── adapter/
│       │   │   ├── in/
│       │   │   └── out/
│       │   └── config/
│       └── test/java/com/example/similarproducts/infrastructure/
│
└── similarproducts-boot/                     ← Boot module (CON Spring)
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   ├── java/com/example/similarproducts/
    │   │   │   └── SimilarProductsApplication.java
    │   │   └── resources/
    │   │       ├── application.yml
    │   │       └── application-dev.yml
    │   └── test/java/...
    └── target/
        └── similarproducts-boot-1.0-SNAPSHOT.jar
```

---

## Docker & Docker Compose

### Dockerfile

Build image con Maven multi-stage:

```dockerfile
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY similarproducts-domain similarproducts-domain
COPY similarproducts-application similarproducts-application
COPY similarproducts-infrastructure similarproducts-infrastructure
COPY similarproducts-boot similarproducts-boot
RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /app/similarproducts-boot/target/similarproducts-boot-*.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

Servicios:

- **similarproducts** (puerto 5000) - La aplicación
- **simulado** (puerto 3001) - APIs mock
- **redis** (puerto 6379) - Caché distribuido

```bash
docker-compose up
```

---

## Contribuciones

Las contribuciones son bienvenidas. Por favor, asegúrate de:

1. **Respetar la arquitectura hexagonal:**
   - Domain sin Spring ❌
   - Application sin Spring ❌  
   - Infrastructure implementa puertos del domain ✅
   - No hay dependencias circulares ✅

2. **Agregar tests para código nuevo:**
   - Unit tests en módulo respectivo
   - Cobertura mínima: 80% en application y infrastructure

3. **Verificar el build completo:**
   ```bash
   mvn clean install
   mvn verify -P e2e
   ```

4. **Actualizar documentación:**
   - Si cambias contrato API, actualiza Swagger/README
   - Si cambias arquitectura, actualiza diagramas en README

---

## Contacto & Soporte

- 📧 Email: support@example.com
- 🐛 Issues: [GitHub Issues](https://github.com/example/similarproducts/issues)
- 📚 Wiki: [Documentación adicional](https://github.com/example/similarproducts/wiki)

---

## Licencia

MIT License — ver [LICENSE](LICENSE) para más detalles.

---

## Checklist de Arquitectura Hexagonal ✅

Antes de considerar el proyecto completo, verifica:

- [ ] ✅ Código compila: `mvn clean compile`
- [ ] ✅ 4 módulos con pom.xml cada uno
- [ ] ✅ NO hay dependencias circulares: `mvn dependency:tree`
- [ ] ✅ Domain SIN Spring: `mvn -pl similarproducts-domain dependency:tree | grep spring`
- [ ] ✅ Tests unitarios pasan: `mvn test`
- [ ] ✅ Coverage >80%: `mvn clean test jacoco:report`
- [ ] ✅ Tests E2E pasan: `mvn verify -P e2e`
- [ ] ✅ Docker build funciona: `docker build -t similarproducts .`
- [ ] ✅ Docker Compose levanta todo: `docker-compose up`
- [ ] ✅ Swagger accesible: http://localhost:5000/swagger-ui.html
- [ ] ✅ Endpoint versionado: `/v1/product/{productId}/similar`
- [ ] ✅ Redis funcionando: `docker-compose up redis`
- [ ] ✅ Logs de cache HIT/MISS: Ver en application logs
- [ ] ✅ Cache expira en 10 min: `redis-cli TTL similar-ids:1`
- [ ] ✅ Tests de caching pasan: Unit tests + E2E tests
- [ ] ✅ Sin código muerto, imports limpios
- [ ] ✅ .gitignore completo, sin secretos
- [ ] ✅ README actualizado con arquitectura completa

---

**Última actualización:** 2024-07-10
**Versión:** 1.0

