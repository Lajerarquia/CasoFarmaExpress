# BFF

Spring Boot 4 y Java 21 o superior. Rama: `feature/bff`.
Es el punto de entrada HTTP de React; usa clientes `RestClient` para comunicarse
con prescriptions y catalog. No necesita una base de datos ni replica sus entidades.

## Variables de entorno

| Variable | Uso |
| --- | --- |
| `PRESCRIPTIONS_URL` | Origen HTTP(S) del servicio prescriptions, sin `/api` |
| `CATALOG_URL` | Origen HTTP(S) del servicio catalog, sin `/api` |
| `FRONTEND_ORIGIN` | Origen exacto de React permitido por CORS, sin barra final |
| `SERVER_PORT` | Puerto BFF; por defecto 8080 |
| `HTTP_CONNECT_TIMEOUT` | Tiempo máximo de conexión; por defecto `3s` |
| `HTTP_READ_TIMEOUT` | Tiempo máximo de lectura; por defecto `10s` |

Las tres primeras variables son obligatorias. No se necesitan credenciales Oracle
ni configuración Azure para ejecutar el BFF. Las URLs de los servicios deben contener
solo esquema, host y puerto opcional; no incluir usuario, contraseña, ruta ni query.
Maven/Spring no cargan archivos `.env` automáticamente.

## Arranque local

1. Iniciar Docker y el contenedor Oracle Free existente.
2. Abrir una terminal en cada microservicio de dominio, definir `DB_URL`,
   `DB_USERNAME` y `DB_PASSWORD` con sus credenciales locales y ejecutar
   `.\mvnw.cmd spring-boot:run`.
   Prescriptions usa por defecto 8081 y catalog 8082.
3. Abrir otra terminal en esta carpeta y ejecutar:

```powershell
$env:PRESCRIPTIONS_URL = 'http://localhost:8081'
$env:CATALOG_URL = 'http://localhost:8082'
$env:FRONTEND_ORIGIN = 'http://localhost:5173'
.\mvnw.cmd spring-boot:run
```

4. En la terminal del frontend React, configurar la URL del BFF y arrancar Vite:

```powershell
$env:VITE_API_BASE_URL = 'http://localhost:8080'
npm run dev
```

Los valores de localhost son ejemplos para ejecución local, no destinos fijados en Java.
Si Vite utiliza otro puerto, ajustar `FRONTEND_ORIGIN` y reiniciar el BFF.
Mantener las variables MSAL del frontend existentes para la posterior integración Azure.

Comprobación desde PowerShell, usando únicamente BFF:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/bff/prescriptions'
Invoke-RestMethod 'http://localhost:8080/api/bff/catalog/medicamentos'
```

## Rutas

| BFF | Servicio interno |
| --- | --- |
| POST /api/bff/prescriptions | POST /api/prescriptions |
| GET /api/bff/prescriptions | GET /api/prescriptions |
| GET /api/bff/prescriptions/{id} | GET /api/prescriptions/{id} |
| PUT /api/bff/prescriptions/{id}/status | PUT /api/prescriptions/{id}/status |
| GET /api/bff/catalog | GET /api/catalog/medicamentos |
| GET /api/bff/catalog/medicamentos | GET /api/catalog/medicamentos |
| GET /api/bff/catalog/medicamentos/{id} | GET /api/catalog/medicamentos/{id} |
| POST /api/bff/catalog/medicamentos | POST /api/catalog/medicamentos |
| PUT /api/bff/catalog/medicamentos/{id} | PUT /api/catalog/medicamentos/{id} |
| DELETE /api/bff/catalog/medicamentos/{id} | DELETE /api/catalog/medicamentos/{id} |

Los filtros de listado se reenvían, incluidos `status`, `from` y `to`.
POST/PUT reciben JSON. El BFF conserva cuerpo, código HTTP, Content-Type y los
headers Retry-After/WWW-Authenticate de las respuestas internas, incluidos 4xx/5xx.
Los fallos de conexión o tiempo de espera devuelven 502 con un Problem Detail,
sin exponer URLs internas ni excepciones. No se reintentan escrituras.

Ejemplo de creación de receta:

```json
{"patientId":"patient-1","pharmacyId":"pharmacy-1","imageUrl":"https://example.test/receta.png"}
```

Ejemplo de cambio de estado:

```json
{"status":"EN_PREPARACION"}
```

Ejemplo de medicamento:

```json
{"sku":"MED-001","nombre":"Paracetamol 500mg","precio":1990,"stock":120}
```

## Alcance de integración con React

Las rutas de `prescriptionsApi.js` y `catalogApi.js` ya apuntan a BFF y no
requieren llamadas directas a los microservicios.

El formulario actual de recetas usa `pacienteNombre`, `rut`, `email`, datos
de despacho y `archivoNombre`, mientras prescriptions recibe `patientId`,
`pharmacyId` e `imageUrl` y devuelve `createdAt` en lugar de `fechaCreacion`.
El BFF mantiene el contrato de prescriptions: ese formulario completo aún requiere
una decisión de mapeo/persistencia y una solución de adjuntos. No se inventan nombres
de pacientes ni se descartan esos datos silenciosamente mediante un mapeo parcial.

## Seguridad y cloud

Los endpoints están abiertos por decisión del equipo para la integración local.
No se implementan JWT, Azure AD ni AWS. OAuth2 queda como dependencia y
`config/SecurityConfig.java` contiene la política explícita para su futura sustitución.
Si llega un header Authorization, el cliente lo reenvía; esto no valida el token
ni aplica permisos. Al incorporar OAuth2, el equipo deberá definir validación,
audiencias, roles y el mecanismo de delegación entre servicios.

Solo BFF tiene CORS para React. El despliegue deberá hacer accesibles prescriptions
y catalog únicamente a los componentes internos; CORS no sustituye controles de red.

## Pruebas

```powershell
.\mvnw.cmd -B verify
```

Las pruebas cargan el BFF completo y usan servidores HTTP locales de prueba para
verificar destinos, métodos, cuerpos, filtros, headers, códigos de error, timeouts
y CORS. No requieren Azure, Docker ni una base de datos. La comprobación con Oracle
real se ejecuta aparte siguiendo el arranque local.

## Carpetas de trabajo sin commits

En este workspace, prescriptions permanece en la carpeta principal y las otras ramas
tienen carpetas Git de trabajo independientes:

- `CasoFarmaExpress`: `feature/prescriptions`.
- `CasoFarmaExpress/.worktrees/catalog`: `feature/catalog`.
- `CasoFarmaExpress/.worktrees/bff`: `feature/bff`.

Ejecutar `git status` desde cada carpeta para revisar sus cambios. Los archivos
nuevos y modificados siguen sin commit y no están publicados en GitHub.
