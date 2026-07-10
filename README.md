# Similar Products API

API REST que obtiene productos similares a uno dado, consumiendo servicios externos con **caching Redis** (TTL 10 minutos).

**Endpoint:** `GET /v1/product/{productId}/similar`

---

## Requisitos

- **Java 17+**
- **Maven 3.8+**
- **Docker & Docker Compose** (opcional, para Redis y servicios mock)

---

## Ejecutar la Aplicación

### Opción 1: Maven (sin Docker)

```bash
# Compilar
mvn clean install

# Ejecutar (requiere Redis en localhost:6379)
mvn -pl similarproducts-boot spring-boot:run
```

### Opción 2: Docker Compose (recomendado)

```bash
docker-compose up
```

Levanta la aplicación, Redis y servicios mock en un solo comando.

**URL:** http://localhost:5000  
**Swagger UI:** http://localhost:5000/swagger-ui.html

---

## Testing

### Tests Unitarios

```bash
mvn test
```

### Tests con Cobertura de Código

```bash
mvn clean test jacoco:report
```

Genera reportes en `{módulo}/target/site/jacoco/index.html`

---

## Arquitectura

Proyecto **multimodulo** (Maven) con **arquitectura hexagonal**:

```
similarproducts-domain/           ← Modelos y lógica de negocio pura
similarproducts-application/       ← Use cases y orquestación
similarproducts-infrastructure/    ← Controllers REST, adapters, Redis, Swagger
similarproducts-boot/              ← Punto de entrada Spring Boot
```

**Caching:** Las llamadas a servicios externos se cachean en Redis con TTL de 10 minutos.

---

## Ejemplo de Uso

```bash
# Obtener productos similares al producto 1
curl http://localhost:5000/v1/product/1/similar

# Respuesta
[
  {
    "id": "2",
    "name": "Product 2",
    "price": 19.99,
    "availability": true
  }
]
```

