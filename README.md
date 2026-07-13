# GeoService

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Arquitectura y patrón de diseño](#2-arquitectura-y-patrón-de-diseño)
3. [Stack tecnológico completo](#3-stack-tecnológico-completo)
4. [Estructura de paquetes](#4-estructura-de-paquetes)
5. [Catálogo detallado de clases](#5-catálogo-detallado-de-clases)
6. [Endpoints REST y WebSocket expuestos](#6-endpoints-rest-y-websocket-expuestos)
7. [Modelo de datos](#7-modelo-de-datos)
8. [Configuración](#8-configuración)
9. [Persistencia](#9-persistencia)
10. [Seguridad](#10-seguridad)
11. [Manejo de errores / excepciones](#11-manejo-de-errores--excepciones)
12. [Comunicación con otros microservicios](#12-comunicación-con-otros-microservicios)
13. [Contenerización (Dockerfile)](#13-contenerización-dockerfile)
14. [Integración continua / despliegue continuo](#14-integración-continua--despliegue-continuo)
15. [Testing](#15-testing)
16. [Cómo ejecutar el proyecto localmente](#16-cómo-ejecutar-el-proyecto-localmente)
17. [Justificación de decisiones tecnológicas](#17-justificación-de-decisiones-tecnológicas)
18. [Limitaciones y riesgos conocidos](#18-limitaciones-y-riesgos-conocidos)

---

## 1. Descripción general

**GeoService** (artefacto Maven `alpha`, `groupId` `eci.edu.dosw`) es un microservicio Spring Boot cuyo propósito de negocio es implementar el **RF29 — "Geolocalización Simplificada en Campus"**. Es un componente dentro de un ecosistema mayor de microservicios (denominado internamente "Alpha") orientado, a juzgar por el dominio (usuarios, zonas de campus, "parches" — término coloquial colombiano para lugares de encuentro/reunión), a una aplicación universitaria o de campus donde los usuarios pueden:

- **Compartir su ubicación geográfica en tiempo real** (latitud/longitud) con otros usuarios conectados, a través de un canal WebSocket con protocolo STOMP.
- **Seleccionar y consultar una "zona de campus"** (un bloque académico, la biblioteca, la cafetería, una cancha, el auditorio, la entrada principal o el parqueadero) como una forma simplificada y de bajo costo computacional de indicar dónde se encuentran, en lugar de exponer coordenadas exactas de forma permanente.
- **Descubrir zonas cercanas ("parches cercanos")** a la zona en la que se encuentran, mediante un grafo de adyacencia estático definido en el propio dominio (enum `CampusZone`).
- **Activar o desactivar la geolocalización** por preferencia de privacidad del usuario (`geoLocationEnabled`), en cuyo caso el servicio no expone ni la zona actual ni las zonas cercanas.

En otras palabras, GeoService resuelve dos necesidades complementarias del dominio "geolocalización":

1. Un **modelo de ubicación fina y efímera** (lat/lng, en memoria, en tiempo real) pensado para tracking en vivo vía WebSocket.
2. Un **modelo de ubicación gruesa y persistente en sesión** (zona discreta del campus) pensado para consultas REST tradicionales, catálogos y agregación de "quién está cerca de quién" sin necesidad de compartir coordenadas exactas.

El código hace referencia explícita al requisito funcional **RF29** tanto en un comentario del `application.properties` como en la documentación OpenAPI (`OpenApiConfig`) y en comentarios de los servicios, lo que confirma que se trata de un microservicio construido para satisfacer ese requisito específico dentro de un proyecto académico (ECI — Escuela Colombiana de Ingeniería, a juzgar por el paquete raíz `eci.edu.dosw`).

## 2. Arquitectura y patrón de diseño

GeoService sigue una **arquitectura en capas (layered architecture)** clásica de Spring Boot, minimalista y sin capa de persistencia relacional:

```
┌───────────────────────────────────────────────────────────┐
│  Cliente (frontend web/móvil, otro microservicio, etc.)   │
└───────────────────────────┬─────────────────────────────────┘
                             │ HTTP REST / WebSocket-STOMP
┌───────────────────────────▼─────────────────────────────────┐
│  Capa de presentación (controller)                          │
│   - ZoneController      (@RestController, REST clásico)     │
│   - LocationController  (@Controller, STOMP + REST híbrido) │
└───────────────────────────┬─────────────────────────────────┘
                             │
┌───────────────────────────▼─────────────────────────────────┐
│  Capa de utilidades transversales (util)                    │
│   - JwtUtil (extracción de identidad desde el token Bearer) │
└───────────────────────────┬─────────────────────────────────┘
                             │
┌───────────────────────────▼─────────────────────────────────┐
│  Capa de lógica de negocio (service)                         │
│   - ZoneService     (reglas de zona / preferencia)           │
│   - LocationService (reglas de ubicación en tiempo real)     │
└───────────────────────────┬─────────────────────────────────┘
                             │
┌───────────────────────────▼─────────────────────────────────┐
│  Capa de dominio / modelo (model, dto)                       │
│   - CampusZone, LocationMessage, UserZonePreference           │
│   - ZoneRequest, ZoneResponse (DTOs de entrada/salida)        │
└───────────────────────────┬─────────────────────────────────┘
                             │
┌───────────────────────────▼─────────────────────────────────┐
│  Almacenamiento (in-memory, ConcurrentHashMap)                │
│   No hay base de datos externa ni capa JPA en este servicio.  │
└───────────────────────────────────────────────────────────────┘
```

Características arquitectónicas relevantes:

- **No hay capa `repository`/DAO ni base de datos externa.** El "almacenamiento" vive directamente dentro de las clases de servicio (`ZoneService`, `LocationService`) usando `java.util.concurrent.ConcurrentHashMap` como estructura en memoria, thread-safe, para soportar accesos concurrentes desde múltiples sesiones WebSocket/HTTP sin bloqueos explícitos.
- **Doble canal de entrada**: REST síncrono (`ZoneController`) para operaciones de consulta/actualización de zona, y WebSocket asíncrono con STOMP (`LocationController` + `WebSocketConfig`) para telemetría de ubicación en tiempo real de baja latencia.
- **DTOs desacoplados del modelo de dominio**: `ZoneRequest`/`ZoneResponse` se usan exclusivamente en la capa REST y son transformados a/desde `UserZonePreference`/`CampusZone` dentro de `ZoneService`, evitando exponer el modelo interno directamente por la API.
- **Autenticación delegada, sin validación criptográfica local**: la identidad del usuario (`userId`) se extrae de un JWT recibido en el header `Authorization`, pero el propio servicio **no valida la firma** del token (ver sección 10, Seguridad). Esto sugiere que GeoService confía en que un componente perimetral (API Gateway, servicio de autenticación) ya validó el token antes de que la petición llegue aquí.
- **Manejo de errores centralizado** vía `@RestControllerAdvice` (`GlobalExceptionHandler`), que traduce excepciones de negocio (`InvalidZoneException`) y de validación (`IllegalArgumentException`) a respuestas HTTP 400 con un cuerpo JSON uniforme.
- **Documentación de API autogenerada** con springdoc-openapi/Swagger UI, anotada directamente en los controladores (`@Tag`, `@Operation`, `@ApiResponse(s)`, `@Parameter`).

## 3. Stack tecnológico completo

| Tecnología / dependencia | Versión | Propósito en GeoService | Por qué se eligió |
|---|---|---|---|
| **Java** | 21 (LTS) | Lenguaje de implementación | LTS más reciente disponible al momento de creación del proyecto; ofrece *virtual threads*, *records*, *pattern matching* y mejoras de rendimiento sobre versiones anteriores, útiles para un microservicio con carga concurrente (WebSocket + REST). |
| **Spring Boot** | 4.1.0 (vía `spring-boot-starter-parent`) | Framework base de la aplicación | Provee auto-configuración, servidor embebido (Tomcat), inyección de dependencias, gestión de starters y un modelo de desarrollo de microservicios estándar de facto en el ecosistema Java empresarial. |
| **spring-boot-starter** | (gestionado por el parent) | Starter núcleo (auto-config, logging con Logback, YAML) | Dependencia transitiva mínima requerida por cualquier aplicación Spring Boot. |
| **spring-boot-starter-web** | (gestionado por el parent) | Publica endpoints REST (`ZoneController`, `LocationController#getLocation`) | Incluye Spring MVC + Tomcat embebido + Jackson para (de)serialización JSON; es el starter estándar para exponer APIs HTTP síncronas. |
| **spring-boot-starter-websocket** | (gestionado por el parent) | Habilita WebSocket + STOMP (`WebSocketConfig`, `LocationController#receiveLocation`) | Necesario para el caso de uso de ubicación en tiempo real: permite *push* de eventos del servidor al cliente sin *polling*, con *fallback* SockJS para clientes/redes que no soportan WebSocket nativo. |
| **spring-boot-starter-test** | (gestionado por el parent, scope `test`) | Framework de pruebas | Agrega transitivamente JUnit 5, Mockito, AssertJ, Spring Test y JSONPath, cubriendo pruebas unitarias y de contexto sin configurar cada librería por separado. |
| **Lombok** | (gestionado por el parent, `optional=true`) | Reduce *boilerplate* en DTOs y modelos (`@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`) | Genera getters/setters/`equals`/`hashCode`/`toString` en tiempo de compilación, manteniendo las clases de dominio y DTO concisas y legibles. |
| **springdoc-openapi-starter-webmvc-ui** | 2.8.14 | Genera especificación OpenAPI 3 y expone Swagger UI | Documentación de API "viva" derivada directamente de las anotaciones en el código (`@Tag`, `@Operation`, `@ApiResponse`), evitando que la documentación se desactualice respecto a la implementación. |
| **JaCoCo (jacoco-maven-plugin)** | 0.8.12 | Mide cobertura de código y aplica *quality gate* | Configurado para excluir clases sin lógica de negocio (`AlphaApplication`, `conf/**`, `model/**`, `dto/**`) y exigir **mínimo 80% de cobertura de instrucciones** en la fase `verify`, evitando que el build pase si la cobertura cae por debajo del umbral. |
| **Maven Wrapper (mvnw)** | Maven 3.9.16 (wrapper 3.3.4) | Build tool | Garantiza que todos los desarrolladores y el pipeline de CI usen exactamente la misma versión de Maven sin necesidad de instalarla globalmente. |
| **JUnit 5 (Jupiter)** | (vía starter-test) | Framework de pruebas unitarias | Estándar actual para pruebas en el ecosistema Java/Spring; soporta extensiones como `MockitoExtension`. |
| **Mockito** | (vía starter-test) | *Mocking* de dependencias en pruebas unitarias de controladores | Permite testear `LocationController` y `ZoneController` aislados de la implementación real de los servicios (`@Mock`, `@InjectMocks`). |
| **AssertJ** | (vía starter-test) | Aserciones fluidas (`assertThat(...)`) | Sintaxis más legible y expresiva que `Assert` de JUnit puro; usada en absolutamente todas las clases de test del proyecto. |

**Nota sobre lo que NO está presente en el stack** (relevante para el documento técnico): este microservicio **no** usa Spring Data JPA, **no** tiene ningún *driver* de base de datos (ni relacional ni NoSQL), **no** usa Spring Security como *starter*, **no** integra Eureka/Spring Cloud/Feign/RestTemplate/WebClient para comunicarse con otros servicios, y **no** usa MapStruct (el mapeo DTO↔modelo se hace manualmente dentro de `ZoneService`). Tampoco hay ninguna librería geoespacial especializada (nada de PostGIS, H3, GeoTools, etc.): el "cálculo de cercanía" entre zonas es un grafo de adyacencia estático codificado a mano en el enum `CampusZone`, no un cálculo geométrico/geoespacial real sobre coordenadas.

## 4. Estructura de paquetes

```
GeoService/
├── pom.xml                          # Definición Maven, dependencias y plugin JaCoCo
├── Dockerfile                       # Build multi-stage de la imagen de contenedor
├── mvnw / mvnw.cmd                  # Maven Wrapper (Unix / Windows)
├── .mvn/wrapper/maven-wrapper.properties
├── .github/workflows/ci-cd.yml      # Pipeline CI/CD (GitHub Actions)
├── token.txt                        # Archivo de configuración/credencial en la raíz del repo
│                                     #   (no forma parte del código fuente de la app; su
│                                     #    contenido no se documenta ni se expone aquí)
└── src/
    ├── main/
    │   ├── java/eci/edu/dosw/alpha/
    │   │   ├── AlphaApplication.java        # Punto de entrada @SpringBootApplication
    │   │   ├── conf/                        # Configuración de beans e infraestructura
    │   │   │   ├── OpenApiConfig.java       #   Bean de metadatos OpenAPI/Swagger
    │   │   │   └── WebSocketConfig.java     #   Configuración del broker STOMP/WebSocket
    │   │   ├── controller/                  # Capa de presentación (endpoints)
    │   │   │   ├── LocationController.java  #   WebSocket + REST de ubicación en tiempo real
    │   │   │   └── ZoneController.java      #   REST de gestión de zonas de campus
    │   │   ├── dto/                         # Objetos de transferencia de datos (API pública)
    │   │   │   ├── ZoneRequest.java         #   Payload de entrada para guardar zona
    │   │   │   └── ZoneResponse.java        #   Payload de salida con zona + cercanas
    │   │   ├── exception/                   # Excepciones de negocio y su manejo global
    │   │   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice centralizado
    │   │   │   └── InvalidZoneException.java    # Excepción de dominio (zona no válida)
    │   │   ├── model/                       # Modelo de dominio interno
    │   │   │   ├── CampusZone.java          #   Enum de zonas + grafo de adyacencia
    │   │   │   ├── LocationMessage.java     #   Mensaje de ubicación en tiempo real
    │   │   │   └── UserZonePreference.java  #   Preferencia de zona/privacidad del usuario
    │   │   ├── service/                     # Lógica de negocio
    │   │   │   ├── LocationService.java     #   Reglas de ubicación en tiempo real
    │   │   │   └── ZoneService.java         #   Reglas de zona/preferencia de usuario
    │   │   └── util/                        # Utilidades transversales sin estado
    │   │       └── JwtUtil.java             #   Extracción de userId desde JWT (sin verificar firma)
    │   └── resources/
    │       └── application.properties       # Configuración única (sin perfiles por ambiente)
    └── test/
        └── java/eci/edu/dosw/alpha/
            ├── AlphaApplicationTests.java            # Test de carga de contexto Spring
            ├── controller/
            │   ├── LocationControllerTest.java       # Test unitario con Mockito
            │   └── ZoneControllerTest.java            # Test unitario con Mockito
            ├── exception/
            │   └── GlobalExceptionHandlerTest.java    # Test unitario directo (sin Spring context)
            ├── service/
            │   ├── LocationServiceTest.java           # Test unitario de reglas de validación
            │   └── ZoneServiceTest.java                # Test unitario de reglas de zona
            └── util/
                └── JwtUtilTest.java                    # Test unitario de parsing JWT
```

Se excluyen intencionalmente de este análisis (por instrucción explícita) las carpetas `.agents/` y `.claude/` (tooling del asistente de IA, no parte del microservicio) y el contenido del archivo `token.txt` (posible secreto).

## 5. Catálogo detallado de clases

### 5.1 Clase principal

#### `AlphaApplication` — paquete `eci.edu.dosw.alpha`
- **Responsabilidad**: punto de entrada de la aplicación Spring Boot.
- **Anotaciones clave**: `@SpringBootApplication` (combina `@Configuration`, `@EnableAutoConfiguration` y `@ComponentScan` sobre el paquete raíz `eci.edu.dosw.alpha`).
- **Métodos**: `main(String[] args)` — invoca `SpringApplication.run(AlphaApplication.class, args)` para arrancar el contenedor embebido y todo el contexto de Spring.
- **Relaciones**: al hacer *component scan* del paquete raíz, detecta automáticamente todos los `@Controller`, `@Service`, `@Configuration`, `@RestControllerAdvice` del proyecto.

### 5.2 Configuración (`conf`)

#### `OpenApiConfig` — paquete `eci.edu.dosw.alpha.conf`
- **Responsabilidad**: define los metadatos globales de la documentación OpenAPI/Swagger de la API.
- **Anotaciones clave**: `@Configuration`.
- **Métodos públicos**: `geoServiceOpenAPI()` — anotado con `@Bean`; construye un objeto `OpenAPI` con título ("GeoService API"), descripción ("Geolocalización simplificada en campus (RF29): gestión de zonas, preferencias y ubicación en tiempo real vía WebSocket.") y versión ("1.0.0").
- **Relaciones**: consumido internamente por springdoc-openapi para renderizar `/swagger-ui.html` y `/v3/api-docs`.

#### `WebSocketConfig` — paquete `eci.edu.dosw.alpha.conf`
- **Responsabilidad**: configura el *message broker* STOMP sobre WebSocket para la mensajería en tiempo real de ubicación.
- **Anotaciones clave**: `@Configuration`, `@EnableWebSocketMessageBroker`. Implementa la interfaz `WebSocketMessageBrokerConfigurer`.
- **Métodos públicos** (*overrides*):
  - `configureMessageBroker(MessageBrokerRegistry config)`: habilita un *simple broker* en memoria para el prefijo `/topic` (canal de salida/broadcast hacia los clientes) y define `/app` como prefijo de destino para los mensajes entrantes desde el cliente.
  - `registerStompEndpoints(StompEndpointRegistry registry)`: registra el endpoint de conexión `/ws-location`, habilita CORS permisivo (`setAllowedOriginPatterns("*")`) y activa *fallback* SockJS para clientes que no soportan WebSocket nativo.
- **Relaciones**: define la infraestructura que usa `LocationController` para recibir (`@MessageMapping("/location")`) y emitir (`@SendTo("/topic/locations")`) mensajes.

### 5.3 Controladores (`controller`)

#### `LocationController` — paquete `eci.edu.dosw.alpha.controller`
- **Responsabilidad**: gestionar la ubicación en tiempo real de los usuarios, combinando un canal WebSocket (STOMP) y un endpoint REST de consulta.
- **Anotaciones clave**: `@Controller` (no `@RestController`, porque combina *message handling* STOMP con un endpoint REST anotado manualmente con `@ResponseBody`), `@Tag(name = "Ubicación", ...)` para Swagger.
- **Dependencias inyectadas**: `LocationService` (vía constructor).
- **Métodos públicos**:
  - `receiveLocation(LocationMessage message)`: anotado con `@MessageMapping("/location")` y `@SendTo("/topic/locations")`. Es el manejador STOMP: el cliente publica en `/app/location`, el método delega la validación/almacenamiento a `LocationService.updateLocation(...)` y el valor de retorno es retransmitido automáticamente (*broadcast*) a todos los suscriptores de `/topic/locations`.
  - `getLocation(@PathVariable String userId)`: anotado con `@GetMapping("/{userId}")` y `@ResponseBody`. **Importante**: como la clase no tiene `@RequestMapping` a nivel de clase, este endpoint queda expuesto directamente en la **raíz** del servicio (`GET /{userId}`, p. ej. `GET /user123`), no bajo un prefijo como `/api/location`. Devuelve la última `LocationMessage` conocida del usuario, o `null` (serializado como cuerpo vacío/`null` JSON con código 200) si no hay datos.
- **Relaciones**: usa `LocationService` para toda la lógica; usa el modelo `LocationMessage` como *payload* de entrada/salida tanto en WebSocket como en REST.

#### `ZoneController` — paquete `eci.edu.dosw.alpha.controller`
- **Responsabilidad**: exponer la API REST para que un usuario registre y consulte su zona de campus, y para exponer el catálogo de zonas válidas.
- **Anotaciones clave**: `@RestController`, `@RequestMapping("/api/zone")`, `@Tag(name = "Zona", ...)`.
- **Dependencias inyectadas**: `ZoneService` (vía constructor).
- **Métodos públicos**:
  - `saveZone(String authHeader, ZoneRequest request)`: `POST /api/zone`. Extrae el `userId` del header `Authorization` (Bearer JWT) usando `JwtUtil.extractUserId(...)`, delega a `ZoneService.saveZone(userId, request)` y responde `200 OK` con un `ZoneResponse`. Si la zona no es válida, la excepción `InvalidZoneException` lanzada aguas abajo es capturada por `GlobalExceptionHandler` y traducida a `400` (caso **E1** del RF29).
  - `getMyZone(String authHeader)`: `GET /api/zone/me`. Extrae `userId` del JWT y delega a `ZoneService.getZone(userId)`. Si el resultado es `null` (usuario sin zona registrada) responde `404 Not Found`; si existe, responde `200 OK` con el `ZoneResponse` (que puede tener `currentZone=null` si la geolocalización está desactivada — caso **E2**).
  - `getCatalog()`: `GET /api/zone/catalog`. No requiere autenticación. Devuelve la lista de nombres (`String`) de todos los valores del enum `CampusZone`, pensada para poblar un selector en el frontend.
- **Relaciones**: usa `ZoneService` para la lógica de negocio, `JwtUtil` para resolver la identidad del usuario, y los DTOs `ZoneRequest`/`ZoneResponse` para el contrato de la API.

### 5.4 DTOs (`dto`)

#### `ZoneRequest` — paquete `eci.edu.dosw.alpha.dto`
- **Responsabilidad**: representar el cuerpo JSON de entrada de `POST /api/zone`.
- **Anotaciones clave**: `@Data` (Lombok: genera getters/setters/`equals`/`hashCode`/`toString`).
- **Campos**: `campusZone` (`String`, nombre textual de la zona, validado y convertido a enum en `ZoneService`), `geoLocationEnabled` (`boolean`, preferencia de privacidad del usuario).
- **Relaciones**: consumido por `ZoneController.saveZone` y `ZoneService.saveZone`.

#### `ZoneResponse` — paquete `eci.edu.dosw.alpha.dto`
- **Responsabilidad**: representar el cuerpo JSON de salida de las operaciones de zona.
- **Anotaciones clave**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`.
- **Campos**: `currentZone` (`String`, nombre de la zona actual, `null` si la geolocalización está desactivada), `nearbyParches` (`List<String>`, nombres de las zonas cercanas, lista vacía si la geolocalización está desactivada).
- **Relaciones**: construido por `ZoneService.buildResponse(...)` y devuelto por `ZoneController.saveZone`/`getMyZone`.

### 5.5 Excepciones (`exception`)

#### `GlobalExceptionHandler` — paquete `eci.edu.dosw.alpha.exception`
- **Responsabilidad**: centralizar la traducción de excepciones de negocio/validación a respuestas HTTP consistentes en toda la API REST.
- **Anotaciones clave**: `@RestControllerAdvice` (intercepta excepciones lanzadas desde cualquier `@RestController` de la aplicación).
- **Métodos públicos**:
  - `handleInvalidZone(InvalidZoneException ex)`: anotado con `@ExceptionHandler(InvalidZoneException.class)` y `@ResponseStatus(HttpStatus.BAD_REQUEST)`. Devuelve un `Map<String,String>` con las claves `error` (mensaje descriptivo) y `code` (`"E1"`, el código de error de negocio del RF29 para "zona inválida").
  - `handleIllegalArgument(IllegalArgumentException ex)`: anotado con `@ExceptionHandler(IllegalArgumentException.class)` y `@ResponseStatus(HttpStatus.BAD_REQUEST)`. Devuelve `Map<String,String>` con la clave `error`. Cubre tanto errores de validación de `LocationService` (userId/lat/lng inválidos) como errores de `JwtUtil` (header `Authorization` ausente o mal formado).
- **Relaciones**: intercepta excepciones lanzadas por `ZoneService` (`InvalidZoneException`), `LocationService` y `JwtUtil` (`IllegalArgumentException`).

#### `InvalidZoneException` — paquete `eci.edu.dosw.alpha.exception`
- **Responsabilidad**: excepción de dominio, no verificada (`RuntimeException`), lanzada cuando el nombre de zona recibido no corresponde a ningún valor del enum `CampusZone`.
- **Constructor**: `InvalidZoneException(String zone)` — construye un mensaje que incluye la zona inválida recibida y remite al cliente a `GET /api/zone/catalog` para consultar las zonas válidas.
- **Relaciones**: lanzada por `ZoneService.parseZone(...)`, capturada por `GlobalExceptionHandler`.

### 5.6 Modelo de dominio (`model`)

#### `CampusZone` — paquete `eci.edu.dosw.alpha.model` (enum)
- **Responsabilidad**: enumerar todas las zonas físicas válidas del campus y modelar, mediante un **grafo de adyacencia estático**, qué zonas se consideran "cercanas" entre sí.
- **Valores**: `BLOQUE_A`, `BLOQUE_B`, `BLOQUE_C`, `BLOQUE_D`, `BLOQUE_E`, `BLOQUE_F` (bloques académicos), `BIBLIOTECA`, `CAFETERIA`, `CANCHA`, `AUDITORIO` (instalaciones), `ENTRADA_PRINCIPAL`, `PARQUEADERO` (accesos y servicios). 12 zonas en total.
- **Estructura interna**: un `Map<CampusZone, List<CampusZone>>` estático (`EnumMap`, inicializado en un bloque estático) llamado `NEARBY`, que define explícitamente, para cada zona, cuáles son sus vecinas directas (p. ej. `BLOQUE_A` es cercano a `BLOQUE_B` y `CAFETERIA`).
- **Métodos públicos**: `getNearby()` — devuelve la lista de zonas cercanas a la zona actual (`this`), o una lista vacía si no hay entrada en el mapa.
- **Nota de dominio**: este NO es un cálculo geoespacial (no usa coordenadas, distancias euclidianas/Haversine, ni un motor SIG). Es un grafo de adyacencia lógico definido a mano, lo cual es coherente con el nombre "Geolocalización **Simplificada**" del RF29.
- **Relaciones**: usado por `ZoneController.getCatalog()` (para listar todos los valores) y por `ZoneService.buildResponse(...)` (para calcular `nearbyParches`).

#### `LocationMessage` — paquete `eci.edu.dosw.alpha.model`
- **Responsabilidad**: representar un evento/mensaje de ubicación geográfica en tiempo real (payload tanto de entrada como de salida en el canal WebSocket, y de salida en el endpoint REST `GET /{userId}`).
- **Anotaciones clave**: `@Data` (Lombok).
- **Campos**: `userId` (`String`), `lat` (`double`, latitud), `lng` (`double`, longitud), `timestamp` (`long`, epoch millis; si no se especifica, `LocationService` lo completa automáticamente con la hora del servidor).
- **Detalle de implementación**: declara explícitamente un método `getUserId()` redundante (Lombok ya lo generaría vía `@Data`), posiblemente remanente de una refactorización o para dejar explícito el *getter* usado en las validaciones tempranas del servicio.
- **Relaciones**: usado por `LocationController` (entrada/salida STOMP y REST) y `LocationService` (almacenamiento y validación).

#### `UserZonePreference` — paquete `eci.edu.dosw.alpha.model`
- **Responsabilidad**: representar el estado persistido en memoria de la preferencia de zona/privacidad de un usuario.
- **Anotaciones clave**: `@Data`.
- **Campos**: `userId` (`String`), `campusZone` (`CampusZone`, la zona seleccionada), `geoLocationEnabled` (`boolean`, si el usuario permite exponer su zona), `updatedAt` (`long`, epoch millis de la última actualización).
- **Relaciones**: creado y mantenido por `ZoneService` dentro de su mapa interno `preferences`; es la "entidad" de dominio interna que nunca se expone directamente por la API (se traduce a `ZoneResponse`).

### 5.7 Servicios (`service`)

#### `LocationService` — paquete `eci.edu.dosw.alpha.service`
- **Responsabilidad**: lógica de negocio para el registro y consulta de ubicaciones en tiempo real.
- **Anotaciones clave**: `@Service`.
- **Estado interno**: `ConcurrentHashMap<String, LocationMessage> locations` — almacena la última ubicación conocida por `userId`, en memoria, sin persistencia a disco ni base de datos.
- **Métodos públicos**:
  - `updateLocation(LocationMessage message)`: valida el mensaje (ver `validate`), completa el `timestamp` si viene en `0`, almacena/reemplaza la última ubicación del usuario en el mapa y devuelve el mensaje (posiblemente enriquecido con el `timestamp`).
  - `getLocation(String userId)`: devuelve la última `LocationMessage` del usuario, o `null` si no existe.
  - `getAllLocations()`: devuelve la colección completa de ubicaciones activas (todas las últimas ubicaciones conocidas de todos los usuarios). No está expuesto actualmente por ningún controlador, pero queda disponible como capacidad de negocio (posible uso futuro para un mapa en vivo de todos los usuarios).
- **Métodos privados**: `validate(LocationMessage message)` — reglas de validación: `userId` obligatorio y no vacío; `lat` debe estar en el rango `[-90, 90]`; `lng` debe estar en el rango `[-180, 180]`. Cualquier violación lanza `IllegalArgumentException` (con mensaje descriptivo), capturada por `GlobalExceptionHandler`.
- **Relaciones**: consumido por `LocationController`; usa el modelo `LocationMessage`.

#### `ZoneService` — paquete `eci.edu.dosw.alpha.service`
- **Responsabilidad**: lógica de negocio del flujo RF29 de selección de zona: validar catálogo, guardar preferencia, calcular zonas cercanas y aplicar la regla de privacidad (E2).
- **Anotaciones clave**: `@Service`.
- **Estado interno**: `ConcurrentHashMap<String, UserZonePreference> preferences` — almacena la preferencia de zona por `userId`, en memoria.
- **Métodos públicos**:
  - `saveZone(String userId, ZoneRequest request)`: convierte el `String` de zona a `CampusZone` (vía `parseZone`), construye un `UserZonePreference` con `updatedAt = System.currentTimeMillis()`, lo almacena y devuelve el `ZoneResponse` correspondiente (vía `buildResponse`). Este es el flujo principal descrito como "RF29" en el comentario del código: *valida catálogo → guarda zona → retorna currentZone + nearbyParches*.
  - `getZone(String userId)`: devuelve el `ZoneResponse` de la preferencia guardada del usuario, o `null` si el usuario nunca ha registrado una zona (lo que el controlador traduce a `404`).
- **Métodos privados**:
  - `parseZone(String raw)`: si `raw` es `null` o está en blanco, lanza `InvalidZoneException("<vacío>")`. En caso contrario intenta `CampusZone.valueOf(raw.toUpperCase().trim())` (comparación **insensible a mayúsculas/minúsculas** gracias al `toUpperCase()`); si el valor no corresponde a ningún enum, captura la `IllegalArgumentException` de Java y la relanza como `InvalidZoneException(raw)` (caso **E1**).
  - `buildResponse(UserZonePreference pref)`: si `geoLocationEnabled` es `false`, devuelve `ZoneResponse(null, List.of())` (caso **E2** — el usuario desactivó la geolocalización, así que no se expone ni la zona ni las cercanas). Si está habilitada, devuelve la zona actual (nombre del enum) y la lista de nombres de zonas cercanas obtenida de `CampusZone.getNearby()`.
- **Relaciones**: usa `CampusZone`, `UserZonePreference`, `InvalidZoneException`, `ZoneRequest`, `ZoneResponse`; consumido por `ZoneController`.

### 5.8 Utilidades (`util`)

#### `JwtUtil` — paquete `eci.edu.dosw.alpha.util`
- **Responsabilidad**: extraer el identificador de usuario (`claim "sub"`) desde un JWT recibido en el header HTTP `Authorization`, **sin verificar la firma criptográfica del token**.
- **Diseño**: clase `final`-como (constructor privado `JwtUtil()`), solo con métodos estáticos — patrón *utility class*. Usa exclusivamente `java.util.Base64` de la biblioteca estándar de Java, **sin ninguna dependencia externa** de JWT (no usa `jjwt`, `nimbus-jose-jwt`, ni Spring Security OAuth2 Resource Server).
- **Métodos públicos**: `extractUserId(String authHeader)` — valida que el header comience con `"Bearer "`; separa el token en sus 3 partes separadas por `.` (header, payload, firma) y exige exactamente 3 partes; decodifica en Base64 URL-safe (con relleno `=` calculado manualmente si falta *padding*) el segmento del *payload*; busca el claim `"sub"` mediante *parsing* manual de texto (sin una librería JSON completa) y lo devuelve. Lanza `IllegalArgumentException` en cualquier escenario de formato inválido, header ausente, o claim `sub` ausente/vacío.
- **Métodos privados**: `extractClaim(String json, String claim)` — *parser* JSON artesanal (búsqueda de substring) que localiza `"claim":"valor"` dentro del JSON del *payload* y extrae el valor entre comillas.
- **Relaciones**: usado exclusivamente por `ZoneController` para resolver el `userId` autenticado antes de delegar a `ZoneService`.

### 5.9 Clases de prueba (`test`)

| Clase de test | Paquete | Clase bajo prueba | Framework/técnica | Qué cubre |
|---|---|---|---|---|
| `AlphaApplicationTests` | `eci.edu.dosw.alpha` | `AlphaApplication` | `@SpringBootTest` (contexto completo) | Verifica que el contexto de Spring levanta sin errores (`contextLoads`). |
| `LocationControllerTest` | `...controller` | `LocationController` | JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) | Delegación correcta a `LocationService` en `receiveLocation`; obtención de ubicación existente y caso usuario inexistente (`null`). |
| `ZoneControllerTest` | `...controller` | `ZoneController` | JUnit 5 + Mockito + construcción manual de JWT falso vía `Base64` | `saveZone` con JWT válido delega correctamente; JWT inválido lanza `IllegalArgumentException`; `getMyZone` encontrado (200) y no encontrado (404); `getCatalog` devuelve el listado completo de zonas. |
| `GlobalExceptionHandlerTest` | `...exception` | `GlobalExceptionHandler`, `InvalidZoneException` | JUnit 5 + AssertJ, instanciación directa (sin contexto Spring) | Que `handleInvalidZone` devuelva el código `"E1"` y el mensaje con la zona; que `handleIllegalArgument` propague el mensaje; que el mensaje de `InvalidZoneException` contenga el nombre de la zona. |
| `LocationServiceTest` | `...service` | `LocationService` | JUnit 5 + AssertJ (sin mocks, servicio real) | Almacenamiento y recuperación de ubicación; autocompletado de `timestamp` en `0`; conservación de `timestamp` explícito; validaciones de `userId` nulo/vacío; validaciones de límites de latitud (`-90`/`90`, incluye casos frontera válidos) y longitud (`-180`/`180`, incluye casos frontera válidos); `getAllLocations` con múltiples usuarios. |
| `ZoneServiceTest` | `...service` | `ZoneService` | JUnit 5 + AssertJ (sin mocks, servicio real) | Guardado de zona válida con cercanas no vacías; caso E2 (geolocalización desactivada → `null`/vacío); zona inválida, en blanco y nula lanzan `InvalidZoneException`; insensibilidad a mayúsculas/minúsculas; consulta de zona inexistente; sobrescritura de preferencia previa; recorrido de las 12 zonas válidas sin excepción; verificación explícita del set de cercanas de `BLOQUE_A`. |
| `JwtUtilTest` | `...util` | `JwtUtil` | JUnit 5 + AssertJ, JWT construido a mano con `Base64` | Extracción correcta del `sub`; header nulo, sin prefijo `Bearer`, con solo 2 partes, sin claim `sub`, con `sub` en blanco, con *padding* Base64 no estándar, y valor `Bearer` vacío. |

Todas las pruebas de servicios y utilidades se hacen **sin mocks** (instanciando la clase real), mientras que las pruebas de controladores usan **Mockito** para aislar la capa de presentación de la lógica de negocio real. No hay pruebas de integración con `MockMvc`/`WebTestClient` ni con `Testcontainers` (coherente con la ausencia de base de datos externa).

## 6. Endpoints REST y WebSocket expuestos

| Método | Ruta | Controlador | Auth | Request body | Response body | Códigos de estado |
|---|---|---|---|---|---|---|
| `POST` | `/api/zone` | `ZoneController.saveZone` | Header `Authorization: Bearer <jwt>` (obligatorio) | `ZoneRequest` `{campusZone: string, geoLocationEnabled: boolean}` | `ZoneResponse` `{currentZone: string\|null, nearbyParches: string[]}` | `200 OK`; `400` (E1: zona inválida — vía `GlobalExceptionHandler`); `400` (JWT ausente/mal formado) |
| `GET` | `/api/zone/me` | `ZoneController.getMyZone` | Header `Authorization: Bearer <jwt>` (obligatorio) | — | `ZoneResponse` | `200 OK`; `404` (el usuario no ha registrado zona); `400` (JWT inválido) |
| `GET` | `/api/zone/catalog` | `ZoneController.getCatalog` | No requiere | — | `String[]` (12 nombres de `CampusZone`) | `200 OK` |
| `GET` | `/{userId}` | `LocationController.getLocation` | No aplica (sin validación de auth en este endpoint) | — (`userId` como *path variable*) | `LocationMessage` `{userId, lat, lng, timestamp}` o `null` | `200 OK` (incluso si no hay datos, devuelve cuerpo `null`) |
| `WS (STOMP SEND)` | `/app/location` (sobre endpoint `/ws-location`) | `LocationController.receiveLocation` | No hay autenticación a nivel de handshake WebSocket en el código revisado | `LocationMessage` `{userId, lat, lng, timestamp?}` | *Broadcast* a `/topic/locations` con el mismo `LocationMessage` (enriquecido con `timestamp` si faltaba) | Excepciones de validación (`IllegalArgumentException`) no tienen manejo STOMP explícito visible en el código (no hay `@MessageExceptionHandler`) |
| `WS (STOMP SUBSCRIBE)` | `/topic/locations` | — (broker simple) | — | `LocationMessage` retransmitido a todos los suscriptores | — |
| `WS (CONNECT)` | `/ws-location` (con *fallback* SockJS) | `WebSocketConfig` | CORS abierto (`setAllowedOriginPatterns("*")`) | Handshake STOMP/SockJS | — | — |
| `GET` | `/swagger-ui.html`, `/v3/api-docs` | (autogenerado por springdoc) | No requiere | — | Especificación OpenAPI / UI interactiva | `200 OK` |

**Observaciones de diseño relevantes para el documento técnico**:
- El endpoint `GET /{userId}` vive en la raíz del enrutamiento (sin prefijo `/api/...`), lo que podría entrar en conflicto con otras rutas de un *API Gateway* compartido si el microservicio se expone directamente sin *prefixing*.
- No existe un `@MessageExceptionHandler` para capturar errores de validación (`IllegalArgumentException` de `LocationService.validate`) lanzados dentro del flujo STOMP `receiveLocation`; el `GlobalExceptionHandler` (`@RestControllerAdvice`) solo aplica a controladores REST, no a *message-mapping* WebSocket.
- Ninguno de los endpoints de zona valida el formato del JWT más allá de decodificar su *payload*; no hay verificación de expiración (`exp`), emisor (`iss`) ni firma.

## 7. Modelo de datos

GeoService **no tiene un modelo de datos persistente en base de datos**; su "modelo de datos" son estructuras Java en memoria:

| Clase | Tipo | Campos | Cardinalidad / relación |
|---|---|---|---|
| `CampusZone` | Enum de dominio | 12 constantes (zonas físicas) | Cada zona tiene 0..N zonas "cercanas" definidas en un grafo de adyacencia estático (`NEARBY`), no simétrico garantizado por construcción (definido a mano por cada entrada, aunque en la práctica las relaciones parecen recíprocas). |
| `LocationMessage` | Mensaje/evento (no entidad persistente) | `userId: String`, `lat: double`, `lng: double`, `timestamp: long` | 1 `LocationMessage` vigente por `userId` (se sobrescribe en cada actualización; no hay histórico). |
| `UserZonePreference` | Estado en memoria por usuario | `userId: String`, `campusZone: CampusZone`, `geoLocationEnabled: boolean`, `updatedAt: long` | 1 `UserZonePreference` por `userId` (relación 1 a 1 lógica; se sobrescribe en cada `saveZone`, no hay histórico de cambios de zona). |

**Aspectos "geoespaciales"**: el único dato geoespacial real (coordenadas de punto, `lat`/`lng` en grados decimales, tipo `double`) vive en `LocationMessage`, validado con rangos estándar del sistema WGS84 (`lat ∈ [-90,90]`, `lng ∈ [-180,180]`). No hay tipos de dato geométricos especializados (`Point`, `Polygon`, `Geometry` de JTS/PostGIS), ni cálculo de distancia real entre coordenadas (fórmula de Haversine, distancia euclidiana, etc.). La "cercanía" entre zonas (`CampusZone.getNearby()`) es puramente lógica/topológica, no derivada de coordenadas.

No hay claves foráneas, joins, ni motor de persistencia relacional: toda relación entre `userId` y su estado (`LocationMessage` / `UserZonePreference`) se resuelve como una entrada de mapa (`Map<String, T>`) en memoria del proceso.

## 8. Configuración

Archivo único: `src/main/resources/application.properties` (no hay perfiles `application-dev.yml`, `application-prod.yml` ni `application-docker.yml`; es una configuración monolítica sin diferenciación por ambiente).

```properties
spring.application.name=geo-service
server.port=8082
logging.level.org.springframework.web=DEBUG
```

| Propiedad | Valor | Efecto |
|---|---|---|
| `spring.application.name` | `geo-service` | Nombre lógico de la aplicación (usado en logs, *actuator* si se agregara, y como nombre de servicio en un *service registry* si se integrara Eureka/Consul en el futuro — actualmente no hay tal integración). |
| `server.port` | `8082` | Puerto HTTP embebido de Tomcat. Coincide con el puerto expuesto (`EXPOSE 8082`) en el `Dockerfile` y con el mapeo de puertos usado en el *workflow* de despliegue SSH (`-p 8082:8082`). |
| `logging.level.org.springframework.web` | `DEBUG` | Nivel de log detallado para el paquete `org.springframework.web`, útil en desarrollo para depurar el enrutamiento de peticiones HTTP/WebSocket, pero ruidoso para producción. |

**Variables de entorno observadas**: en el *job* de despliegue por SSH del pipeline CI/CD se inyecta `SPRING_APPLICATION_NAME=geo-service` como variable de entorno al arrancar el contenedor Docker (redundante con la propiedad ya fijada en `application.properties`, pero permite sobreescritura externa gracias al mecanismo estándar de *property relaxed binding* de Spring Boot). No se detectaron otras variables de entorno (`SPRING_PROFILES_ACTIVE`, cadenas de conexión a base de datos, *secrets* de JWT, URLs de otros servicios, etc.) referenciadas en el código ni en la configuración — coherente con la ausencia de base de datos y de validación criptográfica de JWT.

En la raíz del repositorio existe además un archivo `token.txt`, que aparenta ser un archivo de configuración/credencial independiente del *build* de Spring Boot (no es referenciado desde ningún `.java` ni `.properties` del proyecto). Su contenido no se examina ni se documenta en este README por tratarse de material potencialmente sensible.

## 9. Persistencia

**GeoService no tiene una capa de persistencia en base de datos.** No hay `spring-boot-starter-data-jpa`, ni *drivers* JDBC/R2DBC, ni configuración de `spring.datasource.*`, ni entidades `@Entity`, ni repositorios `JpaRepository`/`CrudRepository`, ni scripts `schema.sql`/`data.sql`, ni herramientas de migración (Flyway/Liquibase).

Toda la "persistencia" es **estado en memoria del proceso**, implementada con `java.util.concurrent.ConcurrentHashMap` directamente dentro de las clases `@Service`:

- `LocationService.locations: ConcurrentHashMap<String, LocationMessage>`
- `ZoneService.preferences: ConcurrentHashMap<String, UserZonePreference>`

**Implicaciones arquitectónicas importantes** (relevantes para un documento técnico formal):

1. **No hay durabilidad**: cualquier reinicio del proceso/contenedor pierde todo el estado (ubicaciones y preferencias de zona de todos los usuarios).
2. **No hay escalabilidad horizontal consistente**: si se despliegan múltiples réplicas del servicio detrás de un balanceador, cada instancia mantiene su propio mapa en memoria; un usuario podría "perder" su última ubicación o preferencia de zona si sus peticiones son enrutadas a una instancia distinta a la que originalmente recibió la actualización. Esto es aceptable para un prototipo/demo académico centrado en el requisito funcional (RF29), pero sería un punto a resolver (p. ej. con Redis, una base de datos compartida, o *sticky sessions*) antes de un despliegue productivo con múltiples réplicas.
3. `ConcurrentHashMap` sí garantiza **seguridad ante concurrencia** dentro de una misma instancia (múltiples hilos HTTP/WebSocket pueden leer/escribir simultáneamente sin corromper el estado), lo cual es razonable para el volumen y la naturaleza de este servicio.

## 10. Seguridad

GeoService **no incluye Spring Security** como dependencia ni configura filtros de seguridad, `SecurityFilterChain`, roles o *scopes*. La única pieza relacionada con autenticación es la clase utilitaria `JwtUtil`:

- Extrae el `userId` (claim `sub`) del JWT recibido en el header `Authorization: Bearer <token>`.
- **No verifica la firma** del JWT (no valida contra una clave secreta o pública, no usa una librería como `jjwt` o el *resource server* de Spring Security OAuth2).
- No valida la fecha de expiración (`exp`), el emisor (`iss`) ni la audiencia (`aud`) del token.
- Solo valida la **forma** del token: que tenga 3 segmentos separados por `.` y que el segundo (el *payload*) sea un JSON Base64URL válido que contenga la clave `"sub"` con un valor no vacío.

**Interpretación arquitectónica**: este diseño es coherente con un microservicio que opera **detrás de un API Gateway o servicio de autenticación centralizado** en la arquitectura general "Alpha", el cual sería responsable de validar la firma y vigencia del JWT antes de reenviar la petición a GeoService. GeoService confía ciegamente en el contenido del token que recibe — es una elección de diseño válida en arquitecturas de microservicios con un perímetro de confianza (*trust boundary*) bien definido, pero representa un **riesgo de seguridad si el servicio llegara a exponerse directamente a la red pública** sin ese gateway delante, ya que cualquier cliente podría fabricar un JWT arbitrario (sin firma válida) y suplantar la identidad de cualquier `userId`.

Otros aspectos de seguridad observados:
- **CORS abierto** en el endpoint WebSocket (`setAllowedOriginPatterns("*")` en `WebSocketConfig`), permitiendo conexiones desde cualquier origen — adecuado para desarrollo/demo, pero a endurecer en producción.
- El endpoint `GET /{userId}` (obtención de última ubicación) **no exige ningún header de autenticación**, a diferencia de los endpoints de `ZoneController`; cualquier cliente que conozca o adivine un `userId` puede consultar su última ubicación conocida.
- No hay *rate limiting*, *CSRF protection* (no aplica tan directamente a una API *stateless*) ni cabeceras de seguridad HTTP (`Content-Security-Policy`, `X-Frame-Options`, etc.) configuradas explícitamente.

## 11. Manejo de errores / excepciones

Centralizado en `GlobalExceptionHandler` (`@RestControllerAdvice`), que traduce dos tipos de excepción a respuestas HTTP uniformes en formato `{"error": "...", "code": "..."}` (el campo `code` solo aparece en el caso E1):

| Excepción | Origen | HTTP Status | Cuerpo de respuesta | Caso de negocio |
|---|---|---|---|---|
| `InvalidZoneException` | `ZoneService.parseZone` (zona nula, en blanco o no reconocida) | `400 Bad Request` | `{"error": "Zona inválida: '<zona>'. Consulte GET /api/zone/catalog...", "code": "E1"}` | **E1** del RF29: zona no válida. |
| `IllegalArgumentException` | `LocationService.validate` (userId/lat/lng inválidos) **o** `JwtUtil.extractUserId` (header/JWT mal formado) | `400 Bad Request` | `{"error": "<mensaje específico>"}` | Validación genérica de entrada / autenticación mal formada. |

El caso **E2** (geolocalización desactivada) **no es un error**: se modela como una respuesta `200 OK` exitosa con `currentZone=null` y `nearbyParches=[]`, reflejando que es un estado válido del dominio, no una condición excepcional.

No hay manejo específico de excepciones no controladas (`Exception.class` genérico), por lo que cualquier error no cubierto (p. ej. `NullPointerException` inesperada) sería manejado por el comportamiento por defecto de Spring Boot (respuesta `500 Internal Server Error` con la página/JSON de error estándar de Whitelabel).

## 12. Comunicación con otros microservicios

No se encontró en el código ningún cliente HTTP saliente (`RestTemplate`, `WebClient`, `OpenFeign`/`@FeignClient`), ningún cliente de *service discovery* (Eureka, Consul), ni configuración de *circuit breaker* (Resilience4j, Hystrix). **GeoService es, tal como está implementado, un servicio autocontenido que no invoca a otros microservicios ni depende de que otros lo invoquen mediante un cliente generado.**

Su única forma de "comunicación con el exterior" es:
- **Entrante**: peticiones REST HTTP y conexiones WebSocket/STOMP de clientes (frontend u otros servicios que actúen como clientes HTTP/WS).
- El `pom.xml` no declara dependencias `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-openfeign` ni similares, por lo que, si el microservicio se integra a un *service registry* dentro del ecosistema "Alpha", esa integración se resolvería externamente (p. ej. mediante un *reverse proxy*/API Gateway que enruta por nombre de servicio), no desde el propio código de GeoService.

## 13. Contenerización (Dockerfile)

El `Dockerfile` implementa un **build multi-stage** de dos etapas, orientado a minimizar el tamaño final de la imagen:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Etapa 1 — `builder`**:
- Imagen base: `eclipse-temurin:21-jdk-alpine` (JDK 21 completo, distribución Alpine Linux — liviana, basada en `musl libc`).
- Copia todo el contexto del repositorio (`COPY . .`) al contenedor.
- Da permisos de ejecución al *Maven Wrapper* (`chmod +x mvnw`) y ejecuta `./mvnw -B package -DskipTests`: compila y empaqueta el JAR en modo *batch* (`-B`, sin interacción) **omitiendo la ejecución de pruebas** dentro de la imagen (las pruebas se ejecutan por separado en el *pipeline* de CI, no durante el build de la imagen de producción).

**Etapa 2 — imagen final**:
- Imagen base: `eclipse-temurin:21-jre-alpine` (solo *runtime* — JRE, no JDK — para una imagen final más pequeña y con menor superficie de ataque, ya que no necesita herramientas de compilación en producción).
- Copia únicamente el artefacto `*.jar` resultante desde la etapa `builder` (`COPY --from=builder`), descartando el código fuente, `.m2`, y cualquier archivo intermedio del build.
- `EXPOSE 8082`: documenta (a nivel de metadatos de la imagen) que el contenedor escucha en el puerto 8082, consistente con `server.port=8082`.
- `ENTRYPOINT ["java", "-jar", "app.jar"]`: comando de arranque del contenedor — ejecuta directamente el JAR ejecutable generado por `spring-boot-maven-plugin` (que empaqueta un *fat jar* con todas las dependencias).

Este patrón (*multi-stage build*) es una práctica estándar en Docker para aplicaciones Java: evita que la imagen final cargue con el JDK completo, herramientas de build y código fuente, reduciendo tamaño de imagen, superficie de ataque y tiempo de despliegue.

## 14. Integración continua / despliegue continuo

El repositorio incluye un *workflow* de GitHub Actions (`.github/workflows/ci-cd.yml`, nombrado `CI/CD — GeoService`) con cuatro *jobs* encadenados:

1. **`ci` (Build & Test)** — se ejecuta en cada `push` a cualquier rama y en cada `pull_request` hacia `main`. Configura Java 21 (Temurin) con caché de Maven, ejecuta `./mvnw -B verify` (lo que dispara compilación, pruebas unitarias y el *quality gate* de JaCoCo ≥80% de cobertura configurado en el `pom.xml`), y publica como artefactos el reporte JaCoCo y los reportes de Surefire (resultados de test), incluso si el *job* falla (`if: always()`).
2. **`build-and-push` (Build & Push Docker image)** — depende de `ci`, solo se ejecuta en `push` a `main`. Inicia sesión en GitHub Container Registry (GHCR), calcula etiquetas de imagen (`sha` corto y `latest`) con `docker/metadata-action`, configura Buildx y construye/publica la imagen Docker usando caché de GitHub Actions.
3. **`deploy` (Deploy to production, vía SSH)** — depende de `build-and-push`, condicionado a que exista el *secret* `DEPLOY_HOST`. Se conecta por SSH al servidor remoto, detiene/elimina el contenedor `geo-service` anterior si existe, hace *pull* de la imagen `:latest` desde GHCR y la arranca (`docker run -d --name geo-service --restart unless-stopped -p 8082:8082 -e SPRING_APPLICATION_NAME=geo-service ...`), y finalmente limpia imágenes Docker antiguas (`docker image prune -f`).
4. **`deploy-azure` (Deploy to Azure Container Apps)** — también depende de `build-and-push`. Se autentica en Azure con `azure/login` usando el *secret* `AZURE_CREDENTIALS` (*Service Principal*), y actualiza el recurso *Container App* `alpha-geoservice` (grupo de recursos `alpha-rf`, entorno `alpha-geoservice-env`) apuntando a la imagen con el *tag* exacto del *commit SHA* corto (no `:latest`), garantizando trazabilidad entre cada *push* a `main` y la revisión desplegada.

Esto confirma que GeoService forma parte de un despliegue **dual** (servidor propio vía SSH/Docker y Azure Container Apps), ambos alimentados desde la misma imagen publicada en GHCR.

## 15. Testing

- **Framework principal**: JUnit 5 (Jupiter), con aserciones AssertJ (`assertThat`, `assertThatThrownBy`, `assertThatNoException`) en absolutamente todas las clases de test.
- **Mocking**: Mockito, usado únicamente en las pruebas de controladores (`LocationControllerTest`, `ZoneControllerTest`) vía `@ExtendWith(MockitoExtension.class)`, `@Mock` y `@InjectMocks`, para aislar la capa de presentación de la implementación real de los servicios.
- **Pruebas de contexto**: `AlphaApplicationTests` usa `@SpringBootTest` para verificar que todo el contexto de Spring (todos los `@Bean`, `@Service`, `@Controller`, `@Configuration`) se ensambla correctamente sin errores de *wiring*.
- **Pruebas unitarias puras** (sin mocks ni contexto Spring): `LocationServiceTest`, `ZoneServiceTest`, `JwtUtilTest`, `GlobalExceptionHandlerTest` — instancian directamente la clase bajo prueba con `new`.
- **No hay**: pruebas de integración con `MockMvc`/`@WebMvcTest`, pruebas end-to-end con `WebTestClient`, ni `Testcontainers` (coherente con la ausencia de base de datos externa que "contenerizar" para pruebas). Tampoco hay pruebas específicas del canal WebSocket/STOMP (no se prueba `WebSocketConfig` ni una conexión STOMP real de extremo a extremo; solo se prueba el método `receiveLocation` como una llamada Java directa vía mock).
- **Cobertura**: exigida formalmente por el *build* mediante JaCoCo — mínimo **80% de cobertura de instrucciones a nivel de "BUNDLE"** (todo el proyecto), con exclusión explícita de `AlphaApplication`, `conf/**` (configuración, sin lógica de negocio testeable de forma significativa), `model/**` y `dto/**` (clases de datos generadas en gran parte por Lombok). Esto concentra el requisito de cobertura en las clases con lógica real: `controller`, `service`, `exception` y `util`.
- **Total de clases de test**: 7 (`AlphaApplicationTests`, `LocationControllerTest`, `ZoneControllerTest`, `GlobalExceptionHandlerTest`, `LocationServiceTest`, `ZoneServiceTest`, `JwtUtilTest`), cubriendo colectivamente decenas de casos (`@Test`) que incluyen explícitamente casos límite/frontera (p. ej. `lat=90`, `lng=180`, JWT con *padding* Base64 no trivial, todas las 12 zonas válidas recorridas en bucle).

## 16. Cómo ejecutar el proyecto localmente

**Requisitos previos**: JDK 21 instalado (o usar directamente el *Maven Wrapper*, que no requiere Maven instalado globalmente, solo el JDK).

```bash
# Clonar y ubicarse en la carpeta del microservicio
cd GeoService

# Ejecutar pruebas + build con verificación de cobertura JaCoCo (>=80%)
./mvnw -B verify        # Linux/macOS/Git Bash
mvnw.cmd -B verify      # Windows (cmd/PowerShell)

# Empaquetar sin ejecutar pruebas (equivalente a lo que hace el Dockerfile)
./mvnw -B package -DskipTests

# Ejecutar la aplicación directamente con el plugin de Spring Boot
./mvnw spring-boot:run

# O bien, tras empaquetar, ejecutar el JAR resultante
java -jar target/alpha-0.0.1-SNAPSHOT.jar
```

Una vez arrancado, el servicio queda disponible en `http://localhost:8082` (puerto definido en `application.properties`):

- Documentación interactiva Swagger UI: `http://localhost:8082/swagger-ui.html`
- Especificación OpenAPI JSON: `http://localhost:8082/v3/api-docs`
- Endpoint WebSocket STOMP: `ws://localhost:8082/ws-location` (con *fallback* SockJS)
- Catálogo de zonas (sin autenticación): `GET http://localhost:8082/api/zone/catalog`

**Con Docker**:

```bash
docker build -t geo-service .
docker run -d --name geo-service -p 8082:8082 geo-service
```

## 17. Justificación de decisiones tecnológicas

- **¿Por qué Spring Boot?** Es el framework Java empresarial de facto para construir microservicios: ofrece auto-configuración basada en convención, un servidor HTTP embebido (elimina la necesidad de desplegar un WAR en un contenedor de aplicaciones externo), un ecosistema maduro de *starters* que resuelven de forma consistente problemas transversales (web, WebSocket, testing), e inyección de dependencias nativa que facilita el diseño en capas desacopladas (`controller` → `service` → `model`) que se observa en este proyecto.
- **¿Por qué Java 21?** Es la versión LTS (*Long-Term Support*) más reciente al momento de creación del proyecto, lo que garantiza soporte extendido de seguridad y estabilidad; además da acceso a mejoras de rendimiento de la JVM y características modernas del lenguaje sin comprometer la estabilidad que exige un componente de infraestructura como un microservicio.
- **¿Por qué WebSocket/STOMP para la ubicación en tiempo real y no *polling* REST?** El caso de uso (ubicación de usuarios en movimiento dentro de un campus) exige baja latencia y actualizaciones *push* frecuentes; un *polling* HTTP tradicional generaría sobrecarga innecesaria de peticiones y latencia perceptible. STOMP sobre WebSocket, con el modelo *publish/subscribe* de Spring (`SimpMessagingTemplate`/`@SendTo`), permite retransmitir eficientemente un evento de ubicación a todos los interesados con una sola operación de *broadcast*, y el *fallback* SockJS asegura compatibilidad con clientes/redes restrictivas.
- **¿Por qué REST clásico para la gestión de zonas (`ZoneController`) y no WebSocket también?** La selección de zona es una operación de baja frecuencia, orientada a consulta/actualización puntual por parte del usuario (no un flujo continuo de eventos), por lo que un modelo síncrono petición/respuesta es más simple de razonar, cachear y documentar (vía OpenAPI) que un canal persistente.
- **¿Por qué no hay base de datos ni JPA?** El alcance del RF29 ("Geolocalización **Simplificada**") no exige persistencia durable ni consultas complejas: es información transitoria, de sesión/tiempo real, cuya pérdida ante un reinicio del proceso es tolerable en el contexto de un prototipo académico/RF acotado. Usar `ConcurrentHashMap` evita la complejidad operativa de aprovisionar, migrar y mantener un motor de base de datos para un volumen de datos y un caso de uso que no lo requieren, priorizando simplicidad y velocidad de desarrollo. En un escenario de evolución hacia producción con múltiples réplicas, este sería el primer punto a reemplazar (p. ej. por Redis para el estado efímero, o una base de datos relacional/documental si se requiriera histórico).
- **¿Por qué no se usó una base de datos con soporte geoespacial (PostGIS, MongoDB con índices `2dsphere`, etc.)?** Porque el propio dominio del microservicio evita deliberadamente el cálculo geoespacial real: no se calculan distancias entre coordenadas ni se hacen consultas de "puntos dentro de un polígono" o "vecinos más cercanos" sobre geometría real. La "cercanía" se resuelve con un grafo de adyacencia estático y pequeño (12 nodos), para el cual una estructura en memoria (`EnumMap`) es más simple, rápida (O(1) de acceso) y suficiente que un motor geoespacial de base de datos.
- **¿Por qué Lombok?** Reduce drásticamente el código repetitivo (getters, setters, constructores, `equals`/`hashCode`/`toString`) en las clases de modelo y DTO, mejorando la legibilidad y manteniendo el foco del código en la lógica de negocio real, sin sacrificar funcionalidad respecto a escribir esos métodos a mano.
- **¿Por qué springdoc-openapi (Swagger)?** Genera documentación de API siempre sincronizada con el código real (a partir de las anotaciones `@Operation`, `@ApiResponse`, `@Tag`), lo cual es especialmente valioso en un ecosistema de microservicios donde otros equipos/servicios consumidores necesitan un contrato claro y actualizado sin depender de documentación externa que se desactualiza.
- **¿Por qué JaCoCo con *quality gate* del 80%?** Automatiza y objetiva el estándar mínimo de calidad de pruebas directamente en el *pipeline* de CI (`mvnw verify` falla si no se cumple), evitando que la cobertura de pruebas dependa de la disciplina manual del equipo, y enfocando el umbral (mediante exclusiones) en las clases donde realmente hay lógica de negocio que vale la pena cubrir.
- **¿Por qué Docker y *multi-stage build*?** Docker garantiza un entorno de ejecución reproducible e independiente del sistema operativo/host, esencial en una arquitectura de microservicios donde cada servicio puede desplegarse y escalar de forma independiente. El *build* multi-stage separa las herramientas de compilación (JDK, Maven) del *runtime* final (solo JRE + JAR), reduciendo el tamaño y la superficie de ataque de la imagen que efectivamente corre en producción.
- **¿Por qué no se usa Spring Security ni una librería JWT completa?** Es una decisión de diseño que traslada la responsabilidad de autenticación/autorización fuerte a un componente perimetral común del ecosistema (API Gateway o servicio de identidad centralizado), evitando duplicar esa lógica en cada microservicio. `JwtUtil` asume ese contrato de confianza y se limita a **extraer** la identidad ya validada aguas arriba, minimizando dependencias y complejidad dentro de GeoService — a costa de que el servicio no sea seguro por sí mismo si se expusiera de forma aislada (ver sección 10).

## 18. Limitaciones y riesgos conocidos

Para que el documento técnico derivado de este README sea honesto y completo, se listan explícitamente las limitaciones detectadas en el código actual:

1. **Sin persistencia durable**: pérdida total de estado (ubicaciones y zonas) ante reinicio del proceso.
2. **Sin soporte multi-instancia consistente**: el estado en memoria no se comparte entre réplicas si el servicio se escala horizontalmente.
3. **JWT sin verificación de firma**: `JwtUtil` confía ciegamente en el contenido del token; no debe exponerse este servicio sin un componente perimetral que valide el JWT primero.
4. **Endpoint `GET /{userId}` sin autenticación** y en la raíz del enrutamiento (riesgo de colisión de rutas y de exposición no autenticada de ubicaciones).
5. **CORS totalmente abierto** (`*`) en el endpoint WebSocket.
6. **Sin manejo de excepciones específico para el canal STOMP** (`@MessageExceptionHandler` ausente); errores de validación en `receiveLocation` no tienen un contrato de respuesta de error definido hacia el cliente WebSocket.
7. **Sin perfiles de configuración por ambiente** (`dev`/`prod`/`docker`): una única configuración (`application.properties`) para todos los entornos, con `logging.level.org.springframework.web=DEBUG` fijo (nivel de log verboso incluso en un hipotético despliegue productivo).
8. **`getAllLocations()` no está expuesto** por ningún controlador — capacidad de negocio implementada pero sin consumidor actual en la API.
