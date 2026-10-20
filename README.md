# vg-ms-flota-mantenimiento (Cañete Seguro - PRS1)

Microservicio Transaccional de **Gestión de Siniestros y Mantenimiento de Flota de Emergencia** (patrulleros de serenazgo/policía, camiones de bomberos y ambulancias médicas).

Construido según los estándares de **Arquitectura Hexagonal**, **Backend**, **Database Storage** y **Security** definidos en el repositorio central `PRS241_standards`.

---

## 1. Contexto de Negocio y Transaccionalidad

### ¿Qué problema de negocio resuelve?
En el sistema de atención de emergencias **Cañete Seguro (PRS1)**, los vehículos operativos son el recurso más crítico. Cuando una patrulla o ambulancia sufre un choque durante un incidente o requiere mantenimiento preventivo:
1. No puede seguir figurando como `DISPONIBLE` en `vg-ms-recursos`.
2. Debe gestionarse formalmente una orden de trabajo con trazabilidad: reporte inicial, taller mecánico asignado, diagnóstico, presupuesto estimado, ejecución en taller, control de calidad y liquidación con costo final.
3. Al finalizar, el microservicio se encarga automáticamente de rehabilitar el vehículo para que vuelva a estar `DISPONIBLE` para la central de despacho.

### ¿Por qué es un Microservicio Transaccional Distribuido?
Implementa una orquestación distribuida (patrón Saga / Coordinación inter-servicios) mediante `WebClient` reactivo con `vg-ms-recursos`:
* **Apertura de Orden / Reporte de Siniestro:** Transaccionalmente invoca `vg-ms-recursos` (`PATCH /api/recursos-operativos/{id}/estado?estado=EN_MANTENIMIENTO`) para bloquear el vehículo.
* **Alta Técnica y Cierre de Orden:** Al emitirse el dictamen de aptitud técnica, invoca `vg-ms-recursos` (`PATCH /api/recursos-operativos/{id}/estado?estado=DISPONIBLE`) para reincorporar la unidad a la flota operativa.

---

## 2. Arquitectura Hexagonal DDD (Estándar PR241)

Estructura de paquetes base `pe.edu.vallegrande.flota` alineada con `PRS241_standards`:

```text
pe.edu.vallegrande.flota
├── FlotaMantenimientoApplication.java
├── domain/
│   ├── enums/                      # EstadoOrden, TipoMantenimiento, PrioridadOrden
│   ├── models/                     # OrdenMantenimiento, DetalleTaller, InformeAltaTecnica
│   └── exception/                  # DomainException (reglas de negocio e invariantes)
├── application/
│   └── services/
│       ├── OrdenMantenimientoService.java
│       └── impl/OrdenMantenimientoServiceImpl.java
└── infrastructure/
    ├── document/                   # BaseDocument (auditoría obligatoria), OrdenMantenimientoDocument
    ├── dto/
    │   ├── common/                 # ResponseDto<T>, ErrorMessage, ValidationError
    │   ├── request/                # CreateOrdenMantenimientoRequest, AsignarTallerRequest...
    │   └── response/               # OrdenMantenimientoResponse, OrdenMantenimientoDetailResponse
    ├── repository/                 # OrdenMantenimientoRepository (ReactiveMongoRepository)
    ├── mapper/                     # OrdenMantenimientoMapper
    ├── rest/client/                # OrdenMantenimientoRest (convención estándar *Rest)
    ├── client/external/            # RecursoOperativoClient (WebClient reactivo hacia vg-ms-recursos)
    ├── exception/                  # GlobalExceptionHandler, ValidationExceptionHandler
    ├── security/                   # SecurityConfig, RoleConstants, SecurityUtils
    └── config/                     # OpenApiConfig, WebClientConfig
```

---

## 3. Cumplimiento de Estándares PRS241

| Rama Estándar | Requisito Principal | Implementación en este Microservicio |
| :--- | :--- | :--- |
| **ProjectArchitecture** | Arquitectura Hexagonal y convención de nombres | Paquete `pe.edu.vallegrande.flota`, controlador `OrdenMantenimientoRest`, dominio desacoplado de MongoDB |
| **DatabaseStorage** | Database per Service en MongoDB Atlas | Base de datos aislada `db_flota`, `retryWrites=true&w=majority`, `auto-index-creation=true`, auditoría `BaseDocument` (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `status`) |
| **Security** | Keycloak IAM y RBAC | OAuth2 Resource Server con JWKS Keycloak (Realm `PRS`), `@PreAuthorize` con `RoleConstants`, sin secretos en repo |
| **Backend** | Buenas prácticas y resiliencia reactiva | Project Reactor (`Mono`/`Flux`) no bloqueante, inyección por constructor, DTOs de request/response, `.env.example`, pruebas unitarias y parametrizadas |

---

## 4. Endpoints Principales (`/api/flota-mantenimiento/ordenes`)

| Método | Endpoint | Descripción | Roles Autorizados |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/flota-mantenimiento/ordenes` | Listar todas las órdenes de mantenimiento | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/{id}` | Obtener orden por ID | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/codigo/{codigo}` | Buscar orden por código (ej: `ORD-2026-XXXX`) | READ_ROLES |
| `GET` | `/api/flota-mantenimiento/ordenes/recurso/{recursoId}` | Historial de taller de un vehículo específico | READ_ROLES |
| `POST` | `/api/flota-mantenimiento/ordenes` | Crear orden y pasar vehículo a `EN_MANTENIMIENTO` | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/taller` | Asignar taller mecánico y presupuesto estimado | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/iniciar-reparacion` | Pasar orden al estado `EN_TALLER` | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/control-calidad` | Pasar orden al estado `CONTROL_CALIDAD` | WRITE_ROLES |
| `POST` | `/api/flota-mantenimiento/ordenes/{id}/alta-operativa` | Emitir alta técnica, finalizar orden y rehabilitar a `DISPONIBLE` | WRITE_ROLES |
| `PATCH`| `/api/flota-mantenimiento/ordenes/{id}/cancelar` | Cancelar orden y rehabilitar vehículo | WRITE_ROLES |

---

## 5. Ejecución Local

```bash
# Compilar y ejecutar pruebas
mvn clean test

# Iniciar el servicio en el puerto 9012
mvn spring-boot:run
```

* **Swagger UI:** `http://localhost:9012/swagger-ui.html`
* **OpenAPI Docs:** `http://localhost:9012/v3/api-docs`
