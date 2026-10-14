# vg-ms-flota-mantenimiento (Cañete Seguro PRS)

Microservicio Transaccional encargado de la **Gestión de Siniestros y Mantenimiento de Flota Vehicular de Emergencia** (patrulleros de serenazgo/policía, camiones de bomberos y ambulancias médicas).

---

## Contexto de Negocio y Transaccionalidad

### ¿Qué problema de negocio resuelve?
En el sistema de atención de emergencias **Cañete Seguro**, los vehículos operativos son el recurso más crítico. Cuando una patrulla o ambulancia sufre un choque durante un incidente o requiere mantenimiento preventivo:
1. No puede seguir figurando como `DISPONIBLE` en `vg-ms-recursos`.
2. Debe gestionarse formalmente una orden de trabajo con trazabilidad: reporte inicial, presupuesto de taller, reparación, control de calidad y dictamen técnico.
3. Al finalizar, debe rehabilitarse automáticamente para volver a estar `DISPONIBLE` para despacho.

### ¿Por qué es un Microservicio Transaccional?
Implementa una orquestación distribuida (patrón Saga) coordinada mediante `WebClient` reactivo con `vg-ms-recursos`:
* **Apertura de Orden / Reporte de Siniestro:** Transaccionalmente invoca `vg-ms-recursos` (`PATCH /api/recursos-operativos/{id}/estado?estado=EN_MANTENIMIENTO`) para bloquear el vehículo.
* **Alta Técnica y Cierre de Orden:** Al emitirse el dictamen de aptitud técnica, invoca `vg-ms-recursos` (`PATCH /api/recursos-operativos/{id}/estado?estado=DISPONIBLE`) para reincorporar la unidad a la flota operativa.

---

## Arquitectura Hexagonal DDD (Ports & Adapters)

Siguiendo el estándar de referencia de `vg-ms-auth-ddd`:

```
vg_ms.flota
├── orden
│   ├── domain
│   │   ├── aggregate          # Aggregate Root (OrdenMantenimiento) con invariantes y lógica pura
│   │   ├── vo                 # Value Objects (EstadoOrden, TipoMantenimiento, DetalleTaller, InformeAltaTecnica)
│   │   ├── event              # Eventos de dominio (OrdenMantenimientoCreadaEvent, AltaOperativaEmitidaEvent)
│   │   └── repository         # Output Port (OrdenMantenimientoRepository - interfaz pura)
│   ├── application
│   │   ├── port               # Output Ports externos (RecursoOperativoPort)
│   │   ├── OrdenMantenimientoService       # Input Port de los casos de uso
│   │   └── OrdenMantenimientoServiceImpl  # Implementación reactiva (Mono/Flux)
│   ├── infrastructure
│   │   ├── persistence        # Documento MongoDB, SpringDataReactiveRepository, Mapper y Adaptador Mongo
│   │   └── client             # Adaptador WebClient hacia vg-ms-recursos inyectando Bearer JWT
│   └── interfaces
│       ├── rest               # Controlador REST reactivo WebFlux (@RestController)
│       └── dto                # Request y Response DTOs
└── shared
    ├── config                 # OpenApiConfig, SecurityConfig, WebClientConfig
    ├── exception              # DomainException, GlobalExceptionHandler estándar
    └── security               # RoleConstants, JwtUtils, JwtAuthenticationFilter
```

El controlador depende del puerto `OrdenMantenimientoService`; la implementación de los casos de uso y los adaptadores de infraestructura pueden cambiarse sin acoplar la interfaz REST a sus detalles.

---

## Cómo Ejecutar el Microservicio

### Requisitos Previos
* **Java 17**
* **Maven 3.9+** (o el wrapper `mvnw`)
* **MongoDB** (Local en `localhost:27017` o Atlas mediante variable de entorno)

### 1. Ejecutar localmente con Maven
```bash
mvn clean spring-boot:run
```

O asignando variables de entorno:
```bash
$env:PORT="9012"
$env:MONGODB_URI="mongodb://localhost:27017/canete_flota_db"
$env:SERVICES_RECURSOS_URL="http://localhost:9004"
mvn spring-boot:run
```

### 2. Documentación Swagger / OpenAPI
Una vez levantado el servicio, accede a:
* **Swagger UI:** [http://localhost:9012/swagger-ui.html](http://localhost:9012/swagger-ui.html)
* **OpenAPI Docs:** [http://localhost:9012/v3/api-docs](http://localhost:9012/v3/api-docs)

---

## Endpoints Principales

| Método | Endpoint | Descripción | Roles Requeridos |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/flota-mantenimiento/ordenes` | Apertura de orden / siniestro (inhabilita recurso en `vg-ms-recursos`) | ADMIN, ENTIDAD_ADMIN, OPERADOR_ENTIDAD |
| `GET` | `/api/flota-mantenimiento/ordenes` | Listar todas las órdenes | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/{id}` | Consultar orden por ID | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/codigo/{codigo}` | Consultar orden por código (ej: ORD-2026-XXXX) | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/recurso/{recursoId}` | Historial de mantenimiento de un vehículo | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/entidad/{entidadId}` | Órdenes asociadas a una entidad | READ_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/taller` | Asignar taller mecánico y presupuesto inicial | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/iniciar-reparacion`| Marcar inicio de reparación (`EN_TALLER`) | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/control-calidad` | Pasar a control de calidad y pruebas técnicas | WRITE_ROLES |
| `POST` | `/api/flota-mantenimiento/ordenes/{id}/alta-operativa` | Emitir alta técnica y habilitar vehículo a `DISPONIBLE` | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/cancelar` | Cancelar orden y rehabilitar vehículo | WRITE_ROLES |
