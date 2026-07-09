# Similar Products API

API REST desarrollada con **Spring Boot** y **arquitectura hexagonal multimodulo** que expone un endpoint para obtener productos similares a uno dado, consumiendo servicios externos.

---

## Descripción

Dado un `productId`, el servicio:
1. Consulta una API externa para obtener los IDs de productos similares.
2. Para cada ID similar, obtiene los detalles del producto desde otra API externa.
3. Devuelve la lista de productos similares con sus detalles.

**Endpoint principal:**
```
GET /product/{productId}/similar
```

---

## Arquitectura

El proyecto sigue la **arquitectura hexagonal (Ports & Adapters)** organizada en **4 módulos Maven independientes**:

```
similarproducts/                          ← Parent POM
├── similarproducts-domain/               ← Núcleo del negocio (sin dependencias externas)
├── similarproducts-application/          ← Use cases y orquestación
├── similarproducts-infrastructure/       ← Adapters HTTP, controllers, Swagger
└── similarproducts-boot/                 ← Punto de entrada Spring Boot
```

### Dependencias entre módulos

```
boot → infrastructure → application → domain
              └──────────────↑
         (infrastructure también depende de domain)
```

### Responsabilidad de cada módulo

| Módulo | Responsabilidad | Dependencias |
|--------|-----------------|--------------|
| `similarproducts-domain` | Modelos, puertos (interfaces), excepciones de negocio | Solo Java (sin Spring) |
| `similarproducts-application` | Use cases, DTOs, mappers, orquestación | `domain` |
| `similarproducts-infrastructure` | Controllers REST, clientes HTTP, Swagger | `domain` + `application` + Spring |
| `similarproducts-boot` | Main class, configuración Spring Boot | Todos los módulos |

### Estructura de paquetes

```
similarproducts-domain/
└── com.example.similarproducts.domain/
    ├── model/          # Product, SimilarProductsRequest, SimilarProductsResponse
    ├── port/           # SimilarIdsPort, ProductDetailPort (interfaces)
    ├── service/        # GetSimilarProductsUseCase
    └── exception/      # ProductNotFoundException, InvalidProductIdException

similarproducts-application/
└── com.example.similarproducts.application/
    ├── dto/            # ProductDetailDto, SimilarProductsResponseDto
    ├── mapper/         # ProductMapper
    ├── service/        # GetSimilarProductsService
    └── config/         # ApplicationConfig

similarproducts-infrastructure/
└── com.example.similarproducts.infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/   # SimilarProductsController, GlobalExceptionHandler
    │   └── out/
    │       └── client/ # SimilarIdsAdapter, ProductDetailAdapter
    ├── config/         # InfrastructureConfig, HttpClientConfig, SwaggerConfig
    └── mapper/         # AdapterMapper

similarproducts-boot/
└── com.example.similarproducts/
    └── SimilarProductsApplication.java  # @SpringBootApplication
```

---

## Requisitos

- **Java 17+**
- **Maven 3.8+**
- **Docker** (para ejecución con `docker-compose`)
- Servicios externos corriendo en `localhost:3001` (o configurables en `application.yml`)

---

## Quick Start

### Compilar el proyecto

```bash
mvn clean install
```

### Ejecutar localmente

```bash
# Opción 1: Maven plugin
mvn -pl similarproducts-boot spring-boot:run

# Opción 2: JAR
java -jar similarproducts-boot/target/similarproducts-boot-1.0-SNAPSHOT.jar
```

### Ejecutar con Docker

```bash
# Build de imagen
docker build -t similarproducts .

# Levantar todo el stack (app + servicios mock)
docker-compose up
```

La aplicación estará disponible en: **http://localhost:5000**

---

## Testing

### Tests unitarios (todos los módulos)

```bash
mvn test
```

### Tests de un módulo específico

```bash
mvn -pl similarproducts-domain test
mvn -pl similarproducts-application test
mvn -pl similarproducts-infrastructure test
```

### Tests con reporte de cobertura (JaCoCo)

```bash
mvn clean test jacoco:report
```

Los reportes se generan en `target/site/jacoco/index.html` de cada módulo.

### Tests E2E (requiere servicios externos corriendo)

```bash
docker-compose up -d simulado
mvn verify -P e2e
```

### Tests de performance (k6)

```bash
docker-compose run --rm k6 run scripts/test.js
```

---

## API Reference

### GET /product/{productId}/similar

Obtiene la lista de productos similares al producto indicado.

**Parámetros:**

| Nombre | Tipo | Descripción |
|--------|------|-------------|
| `productId` | `string` (path) | ID del producto base |

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200 OK` | Lista de productos similares (puede ser vacía `[]`) |
| `400 Bad Request` | `productId` con formato inválido |
| `404 Not Found` | Producto no encontrado |
| `500 Internal Server Error` | Error interno o servicio externo no disponible |

**Ejemplo de respuesta exitosa:**

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

### Swagger UI

Documentación interactiva disponible en:
```
http://localhost:5000/swagger-ui.html
```

---

## Configuración

La configuración principal está en `similarproducts-boot/src/main/resources/application.yml`:

```yaml
server:
  port: 5000

external:
  api:
    base-url: http://localhost:3001
    timeout:
      connect: 5000
      read: 10000
```

Para configuración de desarrollo, usa `application-dev.yml`.

---

## Decisiones Técnicas

### ¿Por qué arquitectura multimodulo?
- **Separación de concerns** explícita: el compilador fuerza las dependencias correctas.
- El módulo `domain` es **agnóstico de frameworks**: puede reutilizarse en cualquier proyecto Java.
- Cada módulo puede **testearse de forma independiente**.
- Facilita la escalabilidad: agregar nuevos adapters sin tocar el dominio.

### ¿Por qué RestTemplate y no WebClient?
- `RestTemplate` es **síncrono y simple**, suficiente para este caso de uso.
- Menor complejidad conceptual para demostrar la arquitectura.
- Migración a `WebClient` (reactivo) es sencilla gracias al desacoplamiento hexagonal.

### Manejo de errores
- Las excepciones de dominio (`ProductNotFoundException`, `InvalidProductIdException`) se lanzan desde los adapters OUT.
- `GlobalExceptionHandler` las captura y mapea a respuestas HTTP apropiadas.
- Estructura de error consistente: `{ "error": "...", "message": "...", "timestamp": "..." }`.

### Validación de entrada
- `productId` se valida en el controller con Spring Validation.
- Formato esperado: string no vacío (numérico preferiblemente).

---

## Estructura del Proyecto Completa

```
similarproducts/
├── pom.xml                              ← Parent POM (gestión de versiones)
├── README.md
├── .gitignore
├── Dockerfile
├── docker-compose.yaml
├── similarproducts-domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── test/java/...
├── similarproducts-application/
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── test/java/...
├── similarproducts-infrastructure/
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── test/java/...
└── similarproducts-boot/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/...
        │   └── resources/
        │       ├── application.yml
        │       └── application-dev.yml
        └── test/java/...
```

---

## Verificación de Arquitectura

Para verificar que no hay dependencias circulares:

```bash
mvn dependency:tree
```

Para verificar que `domain` no tiene dependencias Spring:

```bash
mvn -pl similarproducts-domain dependency:tree | grep spring
# No debe devolver ningún resultado
```

---

## Contribuciones

Las contribuciones son bienvenidas. Por favor, asegúrate de:
1. Respetar la arquitectura hexagonal (no mezclar capas).
2. Añadir tests para el código nuevo.
3. Mantener el coverage por encima del 80%.

---

## Licencia

MIT License — ver [LICENSE](LICENSE) para más detalles.

