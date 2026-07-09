# Plan: Implementación de API de Productos Similares con Arquitectura Hexagonal MULTIMODULO

## TL;DR
Crear una aplicación Spring Boot **MULTIMODULO** en puerto 5000 que expone un endpoint `/product/{productId}/similar` consumiendo dos APIs externas. 

**Estructura multimodulo hexagonal:**
- **domain**: Lógica pura sin dependencias externas
- **application**: Use cases, DTOs, mappers
- **infrastructure**: Adapters HTTP, controllers, Swagger
- **boot**: Punto de entrada Spring Boot

Implementar con tests unitarios/funcionales, SwaggerUI, Docker, manejo de excepciones y código limpio. 🎯

---

## Fase 1: Estructura Inicial del Proyecto

### Task 1.1: Configurar proyecto Maven MULTIMODULO con Spring Boot
- **Objetivo:** Crear estructura base de proyecto Maven multimodulo
- **Subtareas:**
  - Crear `pom.xml` PARENT (root) que gestione:
    - Versión de proyecto
    - Propiedades comunes (Java version, encoding, etc.)
    - Dependencymanagement con versiones compartidas:
      - Spring Boot BOM
      - `spring-boot-starter-web` (REST)
      - `spring-boot-starter-test` (testing)
      - `lombok` (boilerplate reduction)
      - `springdoc-openapi-starter-webmvc-ui` (Swagger/OpenAPI)
      - `spring-boot-starter-validation` (bean validation)
      - `restassured` (testing HTTP)
    - Módulos: `<modules>` para 4 submódulos
    - `<packaging>pom</packaging>`
  
  - Crear `pom.xml` para cada submódulo:
    - **similarproducts-domain**: Sin dependencias Spring, solo core Java
    - **similarproducts-application**: Depende de domain
    - **similarproducts-infrastructure**: Depende de domain, application, Spring
    - **similarproducts-boot**: Spring Boot main, depende de todos

  - Configurar `application.yml` SOLO en boot:
    - Puerto 5000
    - URLs de APIs externas (localhost:3001)
    - Timeouts y configuraciones de cliente HTTP
    - Perfiles (dev, test, prod)
  
  - Estructura de carpetas Maven estándar para CADA módulo (`src/main/java`, `src/test/java`)

### Task 1.2: Establecer arquitectura hexagonal MULTIMODULO
- **Objetivo:** Organizar código según arquitectura hexagonal (ports & adapters) en MÚLTIPLES MÓDULOS Maven
- **Estructura multimodulo:**
  ```
  similarproducts-parent/
  ├── pom.xml (parent - gestiona versiones comunes)
  ├── similarproducts-domain/
  │   ├── pom.xml
  │   └── src/main/java/com/example/similarproducts/domain/
  │       ├── model/
  │       │   ├── Product.java
  │       │   ├── SimilarProductsRequest.java
  │       │   └── SimilarProductsResponse.java
  │       ├── port/
  │       │   ├── SimilarIdsPort.java
  │       │   └── ProductDetailPort.java
  │       ├── service/
  │       │   └── GetSimilarProductsUseCase.java
  │       └── exception/
  │           ├── ProductNotFoundException.java
  │           └── InvalidProductIdException.java
  ├── similarproducts-application/
  │   ├── pom.xml (depende de domain)
  │   └── src/main/java/com/example/similarproducts/application/
  │       ├── dto/
  │       │   ├── ProductDetailDto.java
  │       │   └── SimilarProductsResponseDto.java
  │       ├── mapper/
  │       │   └── ProductMapper.java
  │       ├── service/
  │       │   └── GetSimilarProductsService.java (orquesta use case)
  │       └── config/
  │           └── ApplicationConfig.java
  ├── similarproducts-infrastructure/
  │   ├── pom.xml (depende de domain, application)
  │   └── src/main/java/com/example/similarproducts/infrastructure/
  │       ├── adapter/
  │       │   ├── in/
  │       │   │   ├── rest/
  │       │   │   │   ├── SimilarProductsController.java
  │       │   │   │   └── GlobalExceptionHandler.java
  │       │   │   └── config/
  │       │   │       └── SwaggerConfig.java
  │       │   └── out/
  │       │       ├── client/
  │       │       │   ├── SimilarIdsAdapter.java
  │       │       │   └── ProductDetailAdapter.java
  │       │       └── config/
  │       │           └── HttpClientConfig.java
  │       ├── config/
  │       │   └── InfrastructureConfig.java
  │       └── mapper/
  │           └── AdapterMapper.java
  └── similarproducts-boot/
      ├── pom.xml (depende de infrastructure, application, domain)
      └── src/main/java/com/example/similarproducts/
          ├── SimilarProductsApplication.java (Main)
          ├── resources/
          │   ├── application.yml
          │   └── application-dev.yml
          └── config/
              └── BootConfig.java

  src/test/java/
  ├── similarproducts-domain-tests/
  ├── similarproducts-application-tests/
  └── similarproducts-infrastructure-tests/
  ```

- **Dependencias de módulos:**
  ```
  boot → infrastructure → application
              ↓               ↓
           domain ← ← ← ← ← ← 
  ```
  - **domain:** Sin dependencias externas (solo Java, excepciones)
  - **application:** Depende de `domain`
  - **infrastructure:** Depende de `domain` y `application`
  - **boot:** Punto de entrada, depende de todos

- **Beneficios demostrables:**
  - Separación clara de responsabilidades
  - `domain` puede reutilizarse en otros proyectos
  - Testing independiente de cada módulo
  - Fácil mantener arquitectura limpia
  - Claramente demuestra dominio de hexagonal

### Task 1.3: Inicializar repositorio Git
- **Objetivo:** Preparar proyecto para repo público
- **Subtareas:**
  - Crear `.gitignore` (Maven, IDE, OS files)
  - Commit inicial con estructura base
  - Crear README inicial con descripción del proyecto

### Task 1.4: Configurar Docker para multimodulo
- **Objetivo:** Permitir ejecución en contenedores
- **Subtareas:**
  - En `similarproducts-boot/pom.xml`:
    - Configurar `spring-boot-maven-plugin` con `<executable>true</executable>`
    - Build fat JAR en `similarproducts-boot-{version}.jar`
  
  - Crear `Dockerfile` (en raíz o similarproducts-boot/):
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
  
  - Actualizar `docker-compose.yaml`:
    - Servicio `yourapp` (o nombre similar) en puerto 5000
    - Debe comunicarse con `simulado` (puerto 3001)
    - Network compartida entre servicios
  
  - Verificar buildcontext multilayer funciona correctamente

---

## Fase 2: Core Domain y Use Cases

### Task 2.1: Definir modelos de dominio
- **Objetivo:** Crear entidades de dominio sin dependencias técnicas
- **Subtareas:**
  - Crear clase `Product` (id, name, price, availability)
  - Crear clase `SimilarProductsRequest` (productId)
  - Crear clase `SimilarProductsResponse` (lista de productos)
  - Usar `@Value` o registros (records) de Java 15+ para inmutabilidad

### Task 2.2: Crear puertos (interfaces)
- **Objetivo:** Definir contratos para adapters externos
- **Subtareas:**
  - Crear `SimilarIdsPort`: 
    ```java
    List<String> getSimilarIds(String productId)
    ```
  - Crear `ProductDetailPort`:
    ```java
    Product getProductDetail(String productId)
    ```

### Task 2.3: Implementar use case
- **Objetivo:** Orquestar lógica de negocio
- **Subtareas:**
  - Crear `GetSimilarProductsUseCase` (servicio de aplicación)
  - Inyectar `SimilarIdsPort` y `ProductDetailPort`
  - Implementar lógica:
    1. Obtener IDs similares del producto
    2. Para cada ID, obtener detalles del producto
    3. Retornar lista de productos
  - Manejar casos edge (sin similares, producto no encontrado)

### Task 2.4: Agregar manejo de excepciones de dominio
- **Objetivo:** Crear excepciones específicas del dominio
- **Subtareas:**
  - `ProductNotFoundException` - Cuando producto no existe en APIs externas
  - `InvalidProductIdException` - Cuando productId tiene formato inválido
  - Heredar de `RuntimeException` para propagarse adecuadamente

---

## Fase 3: Adapters de Entrada (Controllers)

### Task 3.1: Crear REST controller
- **Objetivo:** Exponer endpoint HTTP
- **Subtareas:**
  - Crear `SimilarProductsController` con `@RestController`
  - Implementar endpoint: `GET /product/{productId}/similar`
  - Validar parámetro `productId` (no vacío, formato correcto)
  - Inyectar `GetSimilarProductsUseCase`
  - Retornar `ResponseEntity<List<ProductDetailDto>>`

### Task 3.2: Mapeo de DTOs
- **Objetivo:** Convertir modelos de dominio a DTOs HTTP
- **Subtareas:**
  - Crear `ProductDetailDto` (id, name, price, availability)
  - Crear `ProductMapper` con método `toDto(Product): ProductDetailDto`
  - Usar en controller para serializar respuesta

### Task 3.3: Configurar SwaggerUI/Springdoc
- **Objetivo:** Documentar API automáticamente
- **Subtareas:**
  - Agregar `springdoc-openapi-starter-webmvc-ui` en pom.xml
  - Anotar endpoint con `@Operation`, `@ApiResponse`
  - Crear `SwaggerConfig` con información de la API
  - Accesible en `http://localhost:5000/swagger-ui.html`

### Task 3.4: Implementar error handlers
- **Objetivo:** Mapear excepciones a respuestas HTTP consistentes
- **Subtareas:**
  - Crear `GlobalExceptionHandler` con `@ControllerAdvice`
  - `ProductNotFoundException` → 404 Not Found
  - `InvalidProductIdException` → 400 Bad Request
  - Excepciones no controladas → 500 Internal Server Error
  - Response con estructura: `{error: "...", message: "...", timestamp: ...}`

---

## Fase 4: Adapters de Salida (External Clients)

### Task 4.1: Implementar SimilarIdsAdapter
- **Objetivo:** Llamar API externa de IDs similares
- **Subtareas:**
  - Crear `SimilarIdsAdapter` implementando `SimilarIdsPort`
  - Usar `RestTemplate` o `WebClient` para HTTP
  - Endpoint: `GET http://localhost:3001/product/{productId}/similarids`
  - Parsear respuesta JSON a `List<String>`
  - Manejo de errores: 404 → lanzar `ProductNotFoundException`

### Task 4.2: Implementar ProductDetailAdapter
- **Objetivo:** Llamar API externa de detalles de producto
- **Subtareas:**
  - Crear `ProductDetailAdapter` implementando `ProductDetailPort`
  - Endpoint: `GET http://localhost:3001/product/{productId}`
  - Parsear respuesta JSON a `Product`
  - Manejo de errores: 404 → lanzar `ProductNotFoundException`

### Task 4.3: Configurar resilencia
- **Objetivo:** Mejorar robustez ante fallos de servicios externos
- **Subtareas:**
  - Configurar timeouts (ej: 5s para conexión, 10s para lectura)
  - Logging detallado de llamadas HTTP (request/response)
  - Manejo de excepciones de red (timeout, connection refused)
  - Opcional: Agregar reintentos con `@Retry` o `CircuitBreaker`

### Task 4.4: Agregar logging
- **Objetivo:** Facilitar debugging y monitoreo
- **Subtareas:**
  - Usar SLF4J con Logback
  - Logs en adapters (entrada/salida de llamadas HTTP)
  - Logs en use case (inicio/fin de procesamiento)
  - Logs en controller (requests entrantes)

---

## Fase 5: Tests Unitarios (Multimodulo)

### Task 5.1: Tests del Domain (similarproducts-domain-tests o en src/test)
- **Objetivo:** Verificar modelos y excepciones del dominio
- **Casuísticas (3-4 tests):**
  1. Crear Product correctamente con validaciones
  2. Excepciones de dominio se lanzan apropiadamente
  3. Modelos son inmutables (records/valores)
- **Ubicación:** `similarproducts-domain/src/test/java`
- **Herramientas:** JUnit 5

### Task 5.2: Tests de Application (similarproducts-application-tests)
- **Objetivo:** Verificar use cases y orquestación
- **Casuísticas (6-8 tests):**
  1. GetSimilarProductsService obtiene similares correctamente
  2. Retorna lista vacía cuando no hay similares
  3. Lanza ProductNotFoundException cuando producto no existe
  4. Llama adapters en orden correcto
  5. Maneja fallo en adapter de IDs
  6. Maneja fallo en adapter de detalles (uno de los IDs)
  7. No duplica productos
  8. Mapea correctamente de Domain a DTOs
- **Ubicación:** `similarproducts-application/src/test/java`
- **Herramientas:** JUnit 5, Mockito

### Task 5.3: Tests de Infrastructure (similarproducts-infrastructure-tests)
- **Objetivo:** Validar adapters, controller, handlers
- **Casuísticas (9-10 tests):**
  - **Controller tests (5-6):**
    1. GET `/product/1/similar` retorna 200 con array
    2. ProductId vacío retorna 400
    3. Producto no encontrado retorna 404
    4. Error interno retorna 500
    5. Response mapeada correctamente a DTO
  - **Adapter tests (4-5):**
    1. SimilarIdsAdapter retorna lista correcta
    2. SimilarIdsAdapter lanza excepción en 404
    3. ProductDetailAdapter retorna producto correcto
    4. ProductDetailAdapter lanza excepción en 404
    5. Timeout en cliente HTTP maneja error
- **Ubicación:** `similarproducts-infrastructure/src/test/java`
- **Herramientas:** MockMvc, JUnit 5, WireMock/MockServer

### Task 5.4: Coverage mínimo por módulo
- **Objetivo:** Garantizar cobertura de tests en cada módulo
- **Métricas:**
  - **similarproducts-domain:** >90% (pocos tests, pero crítico)
  - **similarproducts-application:** >80% (use cases importantes)
  - **similarproducts-infrastructure:** >75% (adapters, controllers)
- **Herramienta:** JaCoCo Maven plugin con reporte agregado
- **Ejecución:** `mvn clean test jacoco:report`

---

## Fase 6: Tests Funcionales/E2E (5+ Casuísticas)

### Task 6.1: Configurar framework de tests E2E
- **Objetivo:** Preparar ambiente para tests end-to-end
- **Opciones:**
  - **RestAssured + JUnit 5** (recomendado, simple)
  - **Karate** (BDD, más potente)
  - **Cucumber + Spring** (BDD, muy verboso)
- **Setup:** Test container o env config para apuntar a `localhost:3001` y `localhost:5000`

### Task 6.2: Test 1 - Producto válido con similares
- **Escenario:** GET `/product/1/similar`
- **Expected:**
  - Status 200
  - Response es array no vacío
  - Cada elemento tiene id, name, price, availability
  - Orden coincide con similares retornados

### Task 6.3: Test 2 - Producto válido sin similares
- **Escenario:** GET `/product/X/similar` (producto sin similares en mocks)
- **Expected:**
  - Status 200
  - Response es array vacío `[]`

### Task 6.4: Test 3 - Producto no encontrado
- **Escenario:** GET `/product/999/similar`
- **Expected:**
  - Status 404
  - Response contiene mensaje de error

### Task 6.5: Test 4 - ProductId inválido
- **Escenario:** GET `/product/abc123XXXXX/similar` (parámetro inválido)
- **Expected:**
  - Status 400
  - Response contiene mensaje de validación

### Task 6.6: Test 5 - Servicio externo no disponible
- **Escenario:** Mock service caído
- **Expected:**
  - Status 500 (o resilencia)
  - Response contiene mensaje de error de servicio

### Task 6.7: Bonus - Performance bajo carga
- **Objetivo:** Verificar con k6 (ya configurado en docker-compose)
- **Criterios:**
  - Respuesta <2s en percentil 95
  - No más de 5% de errores bajo 100 usuarios

---

## Fase 7: Documentación y Polish

### Task 7.1: README completo
- **Contenido:**
  - Descripción del proyecto
  - **Estructura multimodulo:**
    - Diagrama de dependencias entre módulos
    - Responsabilidad de cada módulo
    - Por qué está organizado así
  - **Quick start:**
    - Requisitos (Java 17+, Maven 3.8+, Docker)
    - Build: `mvn clean install`
    - Run: `mvn -pl similarproducts-boot spring-boot:run` o Docker
  - **Testing:**
    - Unitarios: `mvn test`
    - Específico módulo: `mvn -pl similarproducts-domain test`
    - Con coverage: `mvn clean test jacoco:report`
    - E2E: `mvn test -P e2e` (o similar)
    - Performance: `docker-compose run --rm k6 run scripts/test.js`
  - **Arquitectura Hexagonal:**
    - Diagrama de ports & adapters
    - Explicación de domain layer
    - Cómo agregar nuevos adapters
  - **Decisiones técnicas:**
    - Por qué multimodulo
    - Por qué RestTemplate vs WebClient
    - Manejo de timeouts
    - Estrategia de error handling
    - Separación de concerns por módulo
  - **Contacto/Contribuciones**

### Task 7.2: Actualizar OpenAPI/Swagger
- **Objetivo:** Validar contrato contra `similarProducts.yaml`
- **Subtareas:**
  - Verificar endpoint `/product/{productId}/similar` en Swagger
  - Validar schemas de response
  - Validar códigos HTTP (200, 404)
  - Generar cliente desde OpenAPI si es necesario

### Task 7.3: Code cleanup y validación multimodulo
- **Subtareas:**
  - Ejecutar build completo: `mvn clean install`
  - Revisar NO HAY dependencias circulares entre módulos
    - domain → no importa de application ni infrastructure
    - application → solo importa de domain
    - infrastructure → importa de domain y application
    - boot → importa de todos
  - Ejecutar `mvn spotbugs:check` (si se añade) en cada módulo
  - Revisar con SonarQube o CheckStyle
  - Remover código muerto
  - Revisar pom.xml de cada módulo (¿todas dependencias necesarias?)
  - Formatear código (Eclipse formatter)
  - Documentar clases públicas con Javadoc
  - Verificar imports están minimizados
  - **Generar reporte de dependencias:** `mvn dependency:tree -DoutputFile=dependencies.txt`

### Task 7.4: Configuración para repo público
- **Subtareas:**
  - Revisar `.gitignore` (sin archivos sensibles)
  - Remover credenciales (URLs con usuario/password)
  - Crear `CONTRIBUTING.md` si aplica
  - Crear `LICENSE` (MIT, Apache, etc.)
  - Commit final: "chore: prepare for public release"

---

## Consideraciones Importantes

### 0. Arquitectura Multimodulo
- **Por qué:** Demostrar expertise en arquitectura limpia y hexagonal
- **Beneficios:**
  - Separation of concerns claro
  - `domain` es agnóstico de frameworks
  - Cada módulo puede testearse independientemente
  - Fácil mantener integridad arquitectónica (sin dependencias circulares)
  - Escalable: agregar nuevos adapters sin tocar domain
- **Regla de oro:** `domain` NUNCA depende de nada excepto Java standard
- **Validación:** `mvn dependency:tree` debe mostrar jerarquía clara

### 1. Cliente HTTP
- **Opción 1 (Recomendado):** `RestTemplate` - Simple, synchronous, bien integrado
- **Opción 2:** `WebClient` - Async, reactive, mejor performance pero más complejo
- **Decision:** Usar `RestTemplate` inicialmente, migrar a `WebClient` si performance es crítica

### 2. Manejo de IDs de Producto
- **Validación:** ¿Numérico, string, formato específico?
- **Contrato:** El yaml usa `type: string`, asumir string pero validar no vacío
- **Regex:** Considerar si hay patrón (ej: solo números): `^\\d+$`

### 3. Caching
- **Decisión:** ¿Cachear respuestas?
  - **Con cache:** Mejor performance, pero datos puede estar stale
  - **Sin cache:** Datos siempre fresh, pero más lento
- **Recomendación:** Sin cache inicialmente. Si performance falla, agregar `@Cacheable`

### 4. Logging y Tracing
- **Básico:** Logs en adapters (INFO en inicio, DEBUG en detalles)
- **Avanzado:** Correlation ID para rastrear flujo completo
- **Tool:** SLF4J + Logback (ya viene con Spring Boot)

### 5. Versionado de API
- **No requerido** en enunciado, pero buena práctica
- **Opciones:** `/v1/product/{id}/similar` o header `Accept-Version: 1.0`
- **Decision:** Omitir por simplicidad inicial

### 6. Validación de Entrada
- **Framework:** Spring Validation (`@Valid`, `@NotBlank`, etc.)
- **Donde:** En DTOs de request, validar en controller

### 7. Gestión de Dependencias
- **Regla:** Solo lo necesario, cuidar transitive dependencies
- **Audit:** `mvn dependency:tree` para revisar
- **Avoid:** Conflictos de versiones, librerías obsoletas

---

## Resumen de Fases

| Fase | Tareas | Duración Est. | Entregable |
|------|--------|---------------|------------|
| 1    | Estructura multimodulo (4 tasks) | 2-3h | 4 módulos Maven con poms, sin ciclos |
| 2    | Domain & Use Cases (4 tasks) | 2-3h | domain module 100% puro (sin Spring) |
| 3    | Controllers (4 tasks) | 2-3h | infrastructure module con endpoint REST + Swagger |
| 4    | External Clients (4 tasks) | 2-3h | infrastructure adapters out/ con logging |
| 5    | Unit Tests (4 tasks) | 4-5h | 20+ tests, >80% coverage POR MÓDULO |
| 6    | E2E Tests (7 tasks) | 2-3h | 5+ casos validados contra mocks |
| 7    | Documentation (4 tasks) | 2-3h | README con diagrama multimodulo, cleanup code |

**Total estimado:** 17-23 horas de desarrollo

**Key Differentiator:** Arquitectura multimodulo clara, sin dependencias circulares, domain desacoplado de Spring = expertise en arquitectura hexagonal real

---

## Checklist Final antes de entregar

- [ ] Código compila sin errores: `mvn clean compile`
- [ ] Estructura multimodulo correcta (4 módulos con pom.xml each)
- [ ] NO hay dependencias circulares entre módulos: `mvn dependency:tree`
- [ ] `domain` SIN dependencias externas (solo Java)
- [ ] `application` solo depende de `domain`
- [ ] `infrastructure` depende de `domain` + `application`
- [ ] `boot` es punto de entrada único
- [ ] Tests unitarios pasan: `mvn test`
- [ ] Tests por módulo pasan: `mvn -pl similarproducts-domain test` (etc)
- [ ] Coverage >80% agregado en lógica crítica
- [ ] Tests E2E pasan contra mocks: `docker-compose up -d simulado && mvn verify`
- [ ] Docker build funciona: `docker build -t similarproducts .` y `docker-compose up`
- [ ] README actualizado con instrucciones de multimodulo
- [ ] README documenta responsabilidad de cada módulo
- [ ] Swagger accesible en http://localhost:5000/swagger-ui.html
- [ ] Sin código muerto, imports limpios en cada módulo
- [ ] .gitignore completo, sin secretos, sin JAR/WAR
- [ ] Estructura clara refleja conocimiento de hexagonal architecture
- [ ] Commit con mensaje descriptivo final: "feat: implement similar products API with hexagonal multimodule architecture"

---

## Verificación de Arquitectura Hexagonal (Criterios de Éxito)

### ✅ Domain Module (similarproducts-domain)
- [ ] **Zero Spring dependencies** - Solo Java, sin `@Spring*` annotations
- [ ] **Modelos inmutables** - Records o `@Value`
- [ ] **Puertos (interfaces)** - Definen contratos abstractos
- [ ] **Excepciones de negocio** - Propias del dominio
- [ ] **Lógica pura** - Sin side effects externos
- **Verificación:** `mvn -pl similarproducts-domain dependency:tree | grep spring` → No debe devolver nada

### ✅ Application Module (similarproducts-application)
- [ ] **Solo depende de domain** - No de infrastructure
- [ ] **Use cases/Services** - Orquestan lógica usando puertos
- [ ] **DTOs y Mappers** - Conviertes entre domain y external worlds
- [ ] **Config de aplicación** - Wiring de beans, no Spring Boot yet
- [ ] **Agnóstico de entrada** - No sabe si es REST, gRPC, etc.
- **Verificación:** Puede reutilizarse con diferentes adapters in/out

### ✅ Infrastructure Module (similarproducts-infrastructure)
- [ ] **Adapters IN** - Controllers REST, handlers, config Swagger
- [ ] **Adapters OUT** - HTTP clients, repositorios, APIs externas
- [ ] **Isolado de business logic** - Solo técnica, sin reglas de negocio
- [ ] **Implementa puertos del domain** - Resuelve abstracciones
- **Verificación:** Cambiar adapter HTTP de RestTemplate a WebClient NO toca domain/application

### ✅ Boot Module (similarproducts-boot)
- [ ] **Punto de entrada único** - Main class + application.yml
- [ ] **Mínimo código** - Solo config de Spring Boot
- [ ] **Depende de todos** - El "orquestador" final
- **Verificación:** `mvn -pl similarproducts-boot dependency:tree` muestra todos los módulos

### ✅ Sin Ciclos de Dependencia
```
✅ Correcto:
  boot → infrastructure → application → domain
           └─────────────↑──────────────┘
        (infrastructure depende de domain y application)

❌ Incorrecto (evitar):
  domain → application (OK)
  application → infrastructure → domain (CICLO!)
```

**Test:** `mvn org.jgrapht:jgrapht-core:1.5.1:... dependency:tree` sin ciclos reportados

---

**Next:** Una vez completes este plan, comparte el link del repo o sigue al siguiente chat para implementación detallada.







