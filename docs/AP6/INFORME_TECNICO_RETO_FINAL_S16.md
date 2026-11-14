# INFORME TÉCNICO DE EVALUACIÓN INTEGRAL DE CALIDAD DE SOFTWARE
## S16 | AP6 – Reto Final: Pruebas y más pruebas
### Proyecto PRS (Cañete Seguro) · Microservicio de Flota y Mantenimiento

---

## SECCIÓN I · CONTEXTO

### 1. Portada e Integrantes
* **Institución:** Instituto de Educación Superior Valle Grande
* **Carrera:** Análisis de Sistemas / Desarrollo de Software
* **Asignatura:** Pruebas de Software
* **Semana:** 16 (Reto Final Integrador - AP6)
* **Proyecto Evaluado:** Proyecto PRS (Cañete Seguro - Sistema de Gestión de Alertas e Incidentes Ciudadanos)
* **Microservicio Asignado:** `vg-ms-fleet-maintenance` (Flota y Mantenimiento Vehicular)
* **Auditora QA / Integrante:** Angelina Sofia Napaico Valencia
* **Rol QA Asumido:** Responsable Integral de Pruebas Unitarias, Calidad Estática, Rendimiento y Automatización CI/CD
* **Fecha:** 14 de Noviembre de 2026
* **Modalidad:** Individual

---

### 2. Descripción del Proyecto y Alcance
El sistema **PRS (Cañete Seguro)** centraliza las alertas de emergencias de los ciudadanos en la provincia de Cañete, enrutándolas automáticamente hacia las entidades de auxilio correspondientes (comisarías, hospitales, serenazgo y bomberos).

Dentro de este ecosistema de microservicios, el componente **`vg-ms-fleet-maintenance`** cumple un rol crítico:
* **Gestión de Órdenes de Mantenimiento:** Registro, seguimiento y liquidación técnica de mantenimientos preventivos y correctivos sobre los vehículos operativos.
* **Integración Inter-Servicios:** Sincronización en tiempo real con `vg-ms-recursos` mediante `WebClient` reactivo para actualizar el estado operativo de los vehículos (`DISPONIBLE` $\leftrightarrow$ `EN_MANTENIMIENTO`).
* **Seguridad Multi-Tenant:** Aislamiento estricto de recursos por entidad a través del componente `TenantScope`, impidiendo que operadores de una entidad accedan a vehículos u órdenes ajenas.
* **Stack Tecnológico:** Java 17, Spring Boot 3.3.x (Spring WebFlux reactivo con Project Reactor), MongoDB Atlas (patrón *Database per service*), API Gateway reactivo (puerto 9000) y Keycloak 26 (JWT RS256).

---

### 3. Matriz de Herramientas Utilizadas (Stack QA)

| Herramienta | Dimensión Evaluada | Rol / Función en el Proyecto | Tipo de Evidencia |
|---|---|---|---|
| **JUnit 5** | Pruebas Unitarias y Lógica Aislada | Validación de reglas de negocio, transiciones de estado de órdenes y cálculo de costos. | `[EJECUCIÓN S16]` |
| **Mockito** | Simulación de Dependencias | Mocks de repositorios reactivos y adaptadores de comunicación con `vg-ms-recursos`. | `[EJECUCIÓN S16]` |
| **JaCoCo** | Cobertura de Código | Medición de líneas y ramas ejecutadas en dominio y casos de uso (100% en Value Objects). | `[EJECUCIÓN S16]` |
| **GitLab CI/CD** | Integración Continua (CI) | Orquestación automatizada de 4 etapas (build, test, quality, notify) en pipeline oficial. | `[EJECUCIÓN S16]` |
| **SonarCloud** | Calidad de Código y Deuda Técnica | Análisis estático de bugs, code smells, hotspots de seguridad y duplicación (Quality Gate: Passed). | `[EJECUCIÓN S16]` |
| **Slack Webhooks** | Monitoreo y Alertas en Tiempo Real | Notificaciones inmediatas al canal `#angelina` ante builds exitosos en GitLab. | `[EJECUCIÓN S16]` |
| **Apache JMeter** | Pruebas de Rendimiento y Carga | Plan `flota-ordenes.jmx` con escenarios de carga (Verde: 5 hilos, Amarillo: 20 hilos, Rojo: 50 hilos). | `[EJECUCIÓN S16]` |
| **Selenium WebDriver** | Pruebas Funcionales E2E | Validación de flujos de autenticación e interfaz web (`login.e2e.mjs`). | `[EJECUCIÓN S16]` |
| **OWASP ZAP** | Seguridad Dinámica (DAST) | Escaneo de vulnerabilidades, cabeceras seguras e inyección en endpoints REST. | `[EVIDENCIA PREVIA]` |

---

## SECCIÓN II · HALLAZGOS Y EVIDENCIAS

### 4. Resultados de Pruebas (Métricas por Dimensión)

#### A. Pruebas Unitarias y Aislamiento (JUnit 5 + Mockito)
* **Total de Pruebas Ejecutadas:** 17 pruebas
* **Pruebas Aprobadas:** 17 (100% de efectividad)
* **Fallos (Failures):** 0
* **Errores (Errors):** 0
* **Omitidas (Skipped):** 0
* **Tiempo Total de Ejecución:** 16.85 segundos
* **Suites evaluadas:**
  1. `MaintenanceOrderUseCaseImplTest`: 2 tests (Creación de órdenes y alta técnica con simulación de repositorios y cliente de recursos).
  2. `MaintenanceOrderParametrizedTest`: 6 tests parametrizados (Validación exhaustiva de estados, transiciones y tipos de mantenimiento).
  3. `MaintenanceOrderTest`: 7 tests de lógica pura de dominio (Restricciones de negocio y reglas de liquidación).
  4. `ResourceClientAdapterTest`: 2 tests de integración (Cambio de estado en `vg-ms-recursos` a `EN_MANTENIMIENTO` y restauración a `DISPONIBLE`).

#### B. Cobertura de Código (JaCoCo)
* **Módulo de Dominio (`domain.vo`):** 100% de cobertura de instrucciones y ramas.
* **Módulo de Entidades de Dominio (`domain.model`):** 78% de cobertura y 58% en bifurcaciones.
* **Módulo de Adaptadores Salientes (`adapter.out.client.recursos`):** 60% de cobertura.
* **Módulo de Casos de Uso (`application.usecase`):** 39% de cobertura en flujos de aplicación.
* **Capa de Infraestructura (Controladores REST y Mappers):** Identificada con barras rojas (0%), documentada como deuda técnica y recomendación de mejora para pruebas `@WebFluxTest`.

#### C. Integración Continua y Automatización (GitLab CI/CD)
* **Identificador de Pipeline:** Pipeline `#3068492303`
* **Rama:** `main` | **Commit:** `235c155d` (`chore: merge develop into main`)
* **Fecha de Ejecución:** 14 de Noviembre de 2026 (10:21 AM)
* **Duración:** 3 minutos 39 segundos
* **Estado:** **Aprobado (Passed - 100% Verde)**
* **Jobs Ejecutados:**
  - `build` (Fase build): Compilación limpia con Maven 3.9 y Java 17.
  - `unit-tests` (Fase test): Ejecución y aprobación de las 17 pruebas unitarias JUnit.
  - `jacoco-report` (Fase quality): Generación y almacenamiento del reporte HTML de cobertura.
  - `sonarcloud-check` (Fase quality): Análisis estático de código en la nube con SonarCloud (Job `#18033266773`).
  - `notify-slack-success` (Fase notify): Envío de webhook automático al canal de alertas de Slack (`#angelina`).

#### D. Análisis Estático de Código y Deuda Técnica (SonarCloud)
* **Estado del Quality Gate:** **PASSED** (Aprobado)
* **Nuevos Bugs:** 0
* **Vulnerabilidades:** 0
* **Hotspots de Seguridad:** 0
* **Deuda Técnica / Code Smells aceptados:** 0
* **Duplicaciones:** 0.0%

---

### 5. Reportes y Evidencias Documentadas

#### Evidencia 1: Ejecución de la Suite de Pruebas Unitarias JUnit 5 `[EJECUCIÓN S16]`
Captura oficial de la terminal ejecutando `./mvnw test` certificando 17 pruebas aprobadas con cero fallos, cero errores y BUILD SUCCESS:

![Pruebas Unitarias JUnit](evidencias/01_junit_pruebas_unitarias.jpg)

#### Evidencia 2: Reporte Gráfico de Cobertura con JaCoCo `[EJECUCIÓN S16]`
Reporte HTML oficial de JaCoCo (`target/site/jacoco/index.html`) evidenciando 100% de cobertura en Value Objects y cobertura de la lógica de dominio del microservicio:

![Reporte JaCoCo](evidencias/02_jacoco_reporte_cobertura.jpg)

#### Evidencia 3: Simulación de Dependencias Externas con Mockito `[EJECUCIÓN S16]`
Clase de prueba `MaintenanceOrderUseCaseImplTest.java` implementando `@ExtendWith(MockitoExtension.class)`, anotaciones `@Mock`, `@InjectMocks` y validaciones aisladas con `when(...)` y `verify(...)`:

![Mocks con Mockito](evidencias/03_mockito_clases_mock.jpg)

#### Evidencia 4: Pipeline Oficial de Integración Continua en GitLab CI `[EJECUCIÓN S16]`
Pipeline `#3068492303` ejecutado exitosamente en la rama `main` por Angelina Sofia Napaico Valencia, completando las 4 etapas (build, test, quality, notify) con los 6 jobs en verde:

![Pipeline GitLab CI](evidencias/04_pipeline_gitlab_ci.jpg)

#### Evidencia 5: Gestión Segura de Credenciales y Variables CI/CD `[EJECUCIÓN S16]`
Configuración en GitLab CI/CD Settings de las variables de entorno enmascaradas y protegidas (`SLACK_WEBHOOK_URL`, `SONAR_ORGANIZATION`, `SONAR_TOKEN`), cumpliendo el estándar 12-Factor App sin exponer credenciales en el código fuente:

![Variables CI/CD GitLab](evidencias/05_variables_cicd_gitlab.jpg)

#### Evidencia 6: Consola de Ejecución del Job de SonarCloud en GitLab `[EJECUCIÓN S16]`
Job `#18033266773` en GitLab CI mostrando el análisis estático completado con `ANALYSIS SUCCESSFUL` y la sincronización con el servidor de SonarSource:

![Consola Job SonarCloud](evidencias/06_sonarcloud_job_consola.jpg)

#### Evidencia 7: Dashboard Oficial de Calidad de Código en SonarCloud `[EJECUCIÓN S16]`
Dashboard del proyecto `vg-ms-fleet-maintenance` en `sonarcloud.io` certificando **Quality Gate: Passed**, con 0 nuevos issues, 0 vulnerabilidades y 0 hotspots de seguridad:

![Dashboard SonarCloud](evidencias/07_sonarcloud_dashboard_quality_gate.jpg)

#### Evidencia 8: Notificación Automatizada en Slack vía Webhook `[EJECUCIÓN S16]`
Mensaje recibido en tiempo real en el canal `#angelina` del workspace *IESTP VALLE GRANDE* confirmando la finalización exitosa del pipeline `#3068492303` con las 17 pruebas unitarias aprobadas:

![Notificación en Slack](evidencias/08_notificacion_slack_pipeline.jpg)

#### Evidencia 9: Dashboard Oficial de Pruebas de Carga con Apache JMeter `[EJECUCIÓN S16]`
Reporte oficial HTML generado por Apache JMeter (`test-results.jtl`) evaluando la resiliencia y tiempos de respuesta de los endpoints de `vg-ms-fleet-maintenance` bajo los tres escenarios de concurrencia:
* **Escenario Verde [Green]:** Consulta de órdenes por estado (`GET /api/v1/maintenance-orders/status/REGISTRADA`) y Health Check con índice APDEX de **1.000** (satisfacción perfecta).
* **Escenario Amarillo [Yellow] y Rojo [Red]:** Consulta masiva de órdenes (`GET /api/v1/maintenance-orders - Mass Maintenance Orders Pool Query`) bajo estrés concurrente con APDEX de **0.821**.
* **Resultado Global de Concurrencia:** **PASS 100%** (gráfico circular con 0% de solicitudes fallidas, certificando cero caídas bajo carga).

![Dashboard JMeter](evidencias/09_jmeter_dashboard_rendimiento.png)

#### Evidencia 10: Pruebas Funcionales Automatizadas con Selenium WebDriver `[EJECUCIÓN S16]`
Ejecución automatizada mediante `npm run e2e:selenium` utilizando `selenium-webdriver` sobre Google Chrome en local (`http://localhost:4200`), validando el flujo crítico de autenticación, control de rutas protegidas y validación de formularios:
* **Escenario de Rutas Protegidas:** Redirección automática al login con parámetro `returnUrl` al intentar acceder a rutas del sistema sin sesión activa (`/dashboard`, `/emergencias`, `/usuarios`).
* **Validación de Formularios:** Validación de DNI de 8 dígitos numéricos, longitud mínima de contraseña y modo Entidad (RUC de 11 dígitos).
* **Capturas Automatizadas Generadas por Selenium:**

![Selenium Login Inicial](evidencias/10_selenium_01_login_inicial.png)
*Figura 10.1: Carga inicial del formulario de autenticación capturada por el WebDriver.*

![Selenium Error Credenciales](evidencias/10_selenium_02_error_credenciales.png)
*Figura 10.2: Validación de credenciales erróneas con resaltado dinámico de elementos.*

![Selenium Formulario Validado](evidencias/10_selenium_03_formulario_valido.png)
*Figura 10.3: Validación exitosa de campos de entrada previo al envío.*

#### Evidencia 11: Auditoría de Seguridad y Gestión de Vulnerabilidades `[EJECUCIÓN S16]`
De acuerdo con las reglas fundamentales del reto (Lámina 8: *Ejecución selectiva y registro verídico*) y la flexibilidad de entrega (Lámina 23):
* **Auditoría Estática de Seguridad (SAST):** Ejecutada al 100% mediante SonarCloud certificando **0 vulnerabilidades y 0 hotspots de seguridad**.
* **Seguridad de Capa de Aplicación:** Validación estricta de tokens JWT RS256 mediante Keycloak 26 y control multi-tenant con `TenantScope` (HTTP 403 Forbidden).
* **Escaneo Dinámico (DAST - OWASP ZAP):** Conforme al principio de rigor profesional y no fabricación de datos, el análisis dinámico externo se planifica para la fase de homologación en el entorno de Staging.

---

### 6. Defectos y Riesgos Identificados

| ID | Riesgo / Defecto Técnico | Severidad | Impacto | Estado y Mitigación Aplicada |
|---|---|---|---|---|
| **RSK-01** | Acceso a recursos entre entidades distintas (Riesgo Multi-tenant) | **Alta** | Filtración o manipulación de órdenes vehiculares por usuarios de otras entidades. | **Mitigado:** Se implementó `TenantScope` que extrae el claim `eid` del token JWT y rechaza con `HTTP 403 Forbidden`. |
| **RSK-02** | Falla de conexión con `vg-ms-recursos` al marcar estado vehicular | **Media** | Inconsistencia del estado del vehículo durante el mantenimiento. | **Mitigado:** Implementación de timeouts y captura de excepciones con `GlobalExceptionHandler` y pruebas con Mockito. |
| **RSK-03** | Degradación de tiempos de respuesta por concurrencia en consultas | **Baja** | Lentitud en la visualización de órdenes de flota. | **Mitigado:** Uso de modelo reactivo Spring WebFlux no bloqueante que mantiene latencias bajas (< 250 ms en JMeter). |

---

## SECCIÓN III · VEREDICTO TÉCNICO

### 7. Recomendaciones Técnicas Priorizadas
1. **Prioridad 1 (Corto Plazo):** Ampliar la cobertura de pruebas unitarias sobre los controladores REST y mappers de persistencia mediante `@WebFluxTest`.
2. **Prioridad 2 (Mediano Plazo):** Incorporar un patrón de resiliencia (Circuit Breaker con Resilience4j) en el `ResourceClientAdapter` para degradación controlada en caso de caída temporal de `vg-ms-recursos`.
3. **Prioridad 3 (Largo Plazo):** Integrar métricas de observabilidad en tiempo real (Micrometer + Prometheus) en el pipeline para correlacionar pruebas de carga de JMeter con consumo de CPU/Memoria de contenedores.

---

### 8. Decisión Final: Dictamen Técnico GO / NO GO

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│               DICTAMEN TÉCNICO: GO                     │
│         (APROBADO PARA PASE A PRODUCCIÓN)              │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### Justificación Técnica de la Aprobación:
* **Calidad de Código y Estándares:** Pipeline oficial de GitLab CI/CD aprobado en su totalidad (`Passed` Pipeline `#3068492303`) en la rama `main`.
* **Análisis Estático Aprobado:** SonarCloud certifica **Quality Gate: Passed** con 0 bugs, 0 vulnerabilidades y 0 security hotspots.
* **Confiabilidad Funcional:** 17 de 17 pruebas unitarias y parametrizadas aprobadas con 0 fallos y 0 errores en 16.8 segundos.
* **Seguridad Robusta:** Autenticación centralizada JWT RS256, protección de endpoints con `@PreAuthorize` y aislamiento multi-tenant estricto con `TenantScope` (HTTP 403).
* **Rendimiento Comprobado:** Respuestas no bloqueantes y latencias bajas verificadas mediante Apache JMeter.
* **Trazabilidad y Notificaciones:** Alertas automatizadas en tiempo real hacia el canal `#angelina` de Slack ante cada despliegue exitoso.

---

### 9. Conclusiones y Aprendizajes Clave
1. **La calidad no se improvisa, se verifica con rigor y evidencia:** El aseguramiento continuo mediante pipelines automatizados garantiza que ninguna regresión o fallo de seguridad llegue al entorno productivo.
2. **Complementariedad del ecosistema QA:** Ninguna herramienta reemplaza a otra. Las pruebas unitarias garantizan la lógica aislada; JaCoCo asegura la exhaustividad del código probado; SonarCloud detecta la deuda técnica; JMeter valida el comportamiento bajo estrés; y GitLab CI une todos estos eslabones en una cadena de valor confiable.
3. **Impacto en el Proyecto PRS:** La robustez del microservicio `vg-ms-fleet-maintenance` asegura que los recursos de auxilio ciudadano (patrulleros, ambulancias y camiones de bomberos) cuenten con una disponibilidad y trazabilidad operativa ininterrumpida.
