# Monas que dependen de geolocalización

Fuente: `GamificationService/src/main/java/.../infrastructure/persistence/seed/MonaCatalogSeeder.java`

## Edificios (`ActivityType.BUILDING_CHECKIN`) — 10 monas

| Mona | Nombre | `locationConstraint` |
|---|---|---|
| EDIFICIO_A | Edificio A | `EDIFICIO_A` |
| EDIFICIO_B | Edificio B | `EDIFICIO_B` |
| EDIFICIO_C | Edificio C | `EDIFICIO_C` |
| EDIFICIO_D | Edificio D | `EDIFICIO_D` |
| EDIFICIO_E | Edificio E | `EDIFICIO_E` |
| EDIFICIO_F | Edificio F | `EDIFICIO_F` |
| EDIFICIO_G | Edificio G | `EDIFICIO_G` |
| EDIFICIO_H | Edificio H | `EDIFICIO_H` |
| EDIFICIO_I | Edificio I | `EDIFICIO_I` |
| TOUR_CAMPUS | Tour Campus | compuesta: requiere los 9 edificios anteriores desbloqueados |

## Cafeterías (`ActivityType.CAFETERIA_CHECKIN`) — 4 monas

| Mona | Nombre | `locationConstraint` | Necesita |
|---|---|---|---|
| FAN_REGIO | Fan del Regio | `CAFETERIA_REGIO` | 1 código específico, 5 check-ins |
| EXPLORADOR_CAFETERIAS | Explorador de Cafeterias | `ALL_4_CAFETERIAS` | 4 cafeterías (códigos aún sin definir, además de Regio) |
| RUTA_CAFE | Ruta del Cafe | `UNIQUE_4_CAFETERIAS` | 4 cafeterías únicas |
| CLIENTE_FRECUENTE | Cliente Frecuente | *(sin filtro)* | cualquier cafetería, 15 veces |

## Zonas de campus (`ActivityType.CAMPUS_ZONE_CHECKIN`) — 2 monas

| Mona | Nombre | `locationConstraint` | Necesita |
|---|---|---|---|
| ZEN_MASTER | Zen Master | `LAGO_O_REFLEXION` | zonas `LAGO` y `REFLEXION` |
| MARATON_UNIVERSITARIA | Maraton Universitaria | `5_ZONAS_MISMO_DIA` | 5 zonas distintas en un mismo día (cualquiera) |

## Check-in + horario (requieren `locationCode` aunque no filtran por zona específica) — 2 monas

| Mona | Nombre | Activity | `locationConstraint` |
|---|---|---|---|
| NOCTAMBULO_ACADEMICO | Noctambulo Academico | `LATE_NIGHT_CHECKIN` | `AFTER_19_30` |
| AMANECER_PRODUCTIVO | Amanecer Productivo | `EARLY_MORNING_CHECKIN` | `5_DIAS_SEGUIDOS` |

## Legendaria compuesta — 1 mona

| Mona | Nombre | Requiere |
|---|---|---|
| CONQUISTADOR_CAMPUS | Conquistador del Campus | los 9 edificios + ZEN_MASTER |

**Total: 19 monas dependen de geolocalización.**
**Códigos de ubicación distintos requeridos: 15** → `EDIFICIO_A`…`EDIFICIO_I` (9) + al menos 4 cafeterías (una debe ser `CAFETERIA_REGIO`) + `LAGO` + `REFLEXION`.

---

## Comparación con lo que existe hoy en GeoService

El enum `CampusZone` de GeoService solo define 12 valores:
`BLOQUE_A, BLOQUE_B, BLOQUE_C, BLOQUE_D, BLOQUE_E, BLOQUE_F, BIBLIOTECA, CAFETERIA, CANCHA, AUDITORIO, ENTRADA_PRINCIPAL, PARQUEADERO`

**Ninguno coincide** con los 15 códigos que espera GamificationService: faltan `LAGO`, `REFLEXION`, `CAFETERIA_REGIO`, y los edificios están nombrados distinto (`BLOQUE_A..F` en vez de `EDIFICIO_A..I`, y solo hay 6 en vez de 9). Además `CAFETERIA` es un único valor, no 4 cafeterías distintas.

---

## Sobre las coordenadas GPS

Actualmente **no existe ningún sistema de coordenadas** en el proyecto:

- `ZoneRequest` (`POST /api/zone`) solo recibe un string `campusZone` — el usuario **elige manualmente** su zona de un catálogo fijo (`GET /api/zone/catalog`), no hay detección automática por GPS.
- `LocationMessage` (WebSocket de ubicación en tiempo real, `/app/location` → `/topic/locations`) sí tiene `lat`/`lng`, pero solo se usa para mostrar la posición de otros usuarios en un mapa — no está enlazado a `CampusZone` en ningún punto del código.
- No se encontró ningún archivo `.geojson`, tabla o configuración con coordenadas reales de edificios/cafeterías/lago en ningún repositorio. Los únicos `lat/lng` que aparecen en el código son valores arbitrarios de pruebas unitarias en `GeoService` (no representan ubicaciones reales).

**Para que la asignación de monas por geolocalización funcione automáticamente por GPS**, hay que construir esto desde cero:

1. Definir, para cada uno de los 15 códigos de ubicación, un centro (`lat`, `lng`) + radio de geocerca (metros).
2. Esos valores deben salir de un mapa real del campus (Google Maps / GPS in situ) — no hay datos verificados en el repo para derivarlos, deben ser aportados por alguien con conocimiento físico del campus.
3. Implementar la lógica de geofencing en GeoService: comparar la posición recibida en `LocationMessage` contra las geocercas y determinar en qué `CampusZone` está el usuario.
4. Conectar GeoService → GamificationService: al detectar entrada a una zona, llamar `POST /api/v1/gamification/activity` (o publicar a la cola RabbitMQ `gamification.activity.queue`) con `activityType` = `BUILDING_CHECKIN` / `CAFETERIA_CHECKIN` / `CAMPUS_ZONE_CHECKIN` y `metadata.locationCode` = el código correspondiente.
5. Unificar el catálogo de zonas entre ambos servicios (mismos nombres/códigos en `CampusZone` de GeoService y en los `locationConstraint` sembrados en `MonaCatalogSeeder` de GamificationService).

---

## Estado actual de la integración (confirmado)

- **No existe ningún punto de integración, ni backend ni frontend**, entre GeoService y GamificationService.
- `ZoneService.saveZone()` en GeoService solo guarda la preferencia en memoria (`ConcurrentHashMap`); no llama a Gamification vía HTTP ni publica a RabbitMQ.
- El frontend (`MONAS IMAGES/PATRICI.A-FRONTEND-main`) usa `campusZone` únicamente como filtro de Parches (feed), sin relación con Gamification.
- La lógica de desbloqueo (`UnlockRuleEngine.evaluateCampusZone`, `evaluateBuildingCheckin`, `evaluateCafeteriaCheckin`) sí está implementada correctamente del lado de GamificationService y funcionaría si recibiera los eventos — pero hoy nadie los envía.

| Zona / Punto de Interés | Latitud | Longitud | Radio de Geocerca |
| :--- | :---: | :---: | :---: |
| **Edificio A** | `4.782699855684707` | `-74.04264918379` | 20m |
| **Edificio B** | `4.7830399485075255` | `-74.04270964402168` | 20m |
| **Edificio C** | `4.782389074571274` | `-74.04247753949225` | 20m |
| **Edificio D** | `4.783130774183006` | `-74.04354547558216` | 20m |
| **Edificio E** | `4.782681035143461` | `-74.04385407917442` | 20m |
| **Edificio F** | `4.783590080825507` | `-74.04332219644444` | 20m |
| **Edificio G** | `4.78349313377599` | `-74.04288042271563` | 20m |
| **Edificio H** | `4.781775204352766` | `-74.04493800488173` | 20m |
| **Edificio I** | `4.781918241337216` | `-74.04435269456233` | 20m |
| **Cafeteria Regio** | `4.782980848558539` | `-74.04400996864427` | 15m |
| **Cafeteria 2** | `4.78323330585694` | `-74.0446541217452` | 20m |
| **Cafeteria 3** | `4.783855976183257` | `-74.04583457286611` | 15m |
| **Cafeteria 4** | `4.7823632440346735` | `-74.04299435757378` | 15m |
| **Lago Universitario** | `4.783042637971193` | `-74.04428261122634` | 30m |
| **Zona de reflexion** | `4.78303796045251` | `-74.04448109469203` | 15m |