# Frontend FarmaExpress

React + JavaScript + Vite. El frontend llama exclusivamente al BFF; no contiene
URLs de prescriptions (8081) ni catalog (8082).

## Configuración local

Requiere Node compatible con la versión Vite del proyecto y dependencias del lockfile:

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Si ya existe `.env.local`, editarlo y conservar su configuración anterior en lugar
de reemplazarlo. Configuración de esta integración:

```dotenv
VITE_API_URL=http://localhost:8080
```

Axios usa `VITE_API_URL`, después la variable anterior `VITE_API_BASE_URL` por
compatibilidad y, si ambas faltan, `http://localhost:8080`. Reiniciar Vite después
de cambiar variables. Las variables Vite son públicas: no colocar secretos.

Vite utiliza el puerto 5173 y avisa si está ocupado. El BFF debe estar ejecutándose
con el origen permitido `FRONTEND_ORIGIN=http://localhost:5173` para ese acceso.
CORS es una política del servidor; React no puede habilitarla con headers propios.
No se modificó el backend ni se agregó un proxy hacia los microservicios internos.

La integración funciona con la sesión local existente. Las dependencias y archivos
MSAL se conservan; si Azure no está configurado, Axios no intenta obtener tokens.
No se agregó integración Azure, AWS ni JWT.

## Rutas utilizadas

| Acción | Ruta BFF |
| --- | --- |
| Listar recetas | GET /api/bff/prescriptions |
| Crear receta | POST /api/bff/prescriptions |
| Consultar detalle | GET /api/bff/prescriptions/{id} |
| Cambiar estado | PUT /api/bff/prescriptions/{id}/status |
| Listar medicamentos | GET /api/bff/catalog/medicamentos |
| Crear medicamento | POST /api/bff/catalog/medicamentos |
| Actualizar medicamento | PUT /api/bff/catalog/medicamentos/{id} |
| Eliminar medicamento | DELETE /api/bff/catalog/medicamentos/{id} |

## Recetas

El formulario de Mis recetas envía solo:

```json
{"patientId":"paciente@example.test","pharmacyId":"pharmacy-1","imageUrl":"https://example.test/receta.png"}
```

El identificador del paciente se propone a partir del correo de la sesión local y
puede editarse. Usar el mismo identificador para consultar el historial tras recargar.
La farmacia se indica por su identificador; todavía no hay una API de farmacias.
La URL es opcional. No se convierten nombres de archivo en URLs ni se simulan subidas.

El backend no guarda nombre del paciente, dirección, despacho, comentarios ni archivos.
La pantalla se ajusta a ese contrato y ofrece una URL de documento existente.
El contexto mantiene los campos originales de la API y adapta `patientId` para la
etiqueta de paciente y `createdAt` para la fecha mostrada. Los mocks antiguos siguen
mostrando sus nombres y fechas.

Se conservan los seis estados y las transiciones actuales del backend.
El detalle de la pantalla Recetas usa GET por ID antes de mostrar las acciones.
Los errores de creación o cambio de estado se muestran; una escritura fallida no se
convierte en éxito de ejemplo.

El filtrado del historial por identificador es solo presentación local. La separación
de datos y autorización por usuario corresponde a la futura integración del equipo.

## Catálogo

Permite listar, crear, actualizar y eliminar. Las acciones de administración usan los
roles locales existentes. La eliminación pide confirmación y quita el producto del
carrito después de que el servidor responde correctamente.

POST y PUT envían `sku`, `nombre`, `precio` y `stock`; PUT incluye todos los campos,
aunque se edite solo precio o stock. Los valores numéricos se validan antes de enviar.
La interfaz espera la respuesta y muestra errores como SKU duplicado (409).

El carrito conserva el flujo existente de actualización de stock. El backend aún no
dispone de transacciones de compra/pago; las actualizaciones de varios productos son
independientes.

## Fallback de ejemplo

Se mantienen `src/mocks/catalogMock.js` y `src/mocks/prescriptionsMock.js`.
Si falla la carga por red, timeout o error 5xx, cada contexto usa sus mocks guardados
en el navegador y muestra un aviso explícito. Las operaciones de ejemplo no llaman
al backend y no se sincronizan posteriormente.

Los errores 4xx no activan mocks. El botón Actualizar conexión vuelve a intentar el
BFF y reemplaza los ejemplos con los datos reales cuando está disponible.
Los fallos de almacenamiento local no impiden usar los ejemplos en memoria.

## Verificación

```powershell
npm test
npm run build
```

Las pruebas de Node cubren el contrato de recetas, adaptación de mocks, estados,
validaciones de catálogo y criterios de fallback. El build genera `dist/`.

También se comprobó en Edge headless el formulario de recetas, detalle, estados,
CRUD de catálogo, errores 409, fallback y reconexión usando respuestas BFF simuladas.
Esa prueba no modifica datos de Oracle ni sustituye una verificación con los servicios
reales activos.

`npm run lint` detecta incidencias previas de Fast Refresh (componentes y hooks
exportados desde el mismo archivo) y efectos React en el proyecto; no se desactivaron
esas reglas para ocultarlas.
