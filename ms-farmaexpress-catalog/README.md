# Catalog

Spring Boot 4, Java 21 o superior y Oracle. Rama: `feature/catalog`.
Estructura: `model`, `dto`, `repository`, `service`, `controller` y `config`.

## Configuración y ejecución

Definir las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en el proceso.
Ejemplo de URL local: `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`.
Usar el usuario/esquema Oracle asignado a catalog; las credenciales no se guardan en Git.
Spring/Maven no cargan archivos `.env` automáticamente.

- `SERVER_PORT`: puerto HTTP, por defecto 8082.
- `DB_DDL_AUTO`: por defecto `update`, para desarrollo.
- `SHOW_SQL`: por defecto `false`.

Desde esta carpeta:

```powershell
.\mvnw.cmd spring-boot:run
```

La entidad utiliza la tabla `MEDICAMENTOS` y la secuencia `MEDICAMENTO_SEQ`.
Hibernate puede crearlas con `DB_DDL_AUTO=update` si el usuario Oracle tiene permisos.

## Contrato

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/catalog/medicamentos` | 200, arreglo ordenado por id |
| GET | `/api/catalog/medicamentos/{id}` | 200 o 404 |
| POST | `/api/catalog/medicamentos` | 201, 400 o 409 |
| PUT | `/api/catalog/medicamentos/{id}` | 200, 400, 404 o 409 |
| DELETE | `/api/catalog/medicamentos/{id}` | 204 o 404 |

POST y PUT reciben:

```json
{"sku":"MED-001","nombre":"Paracetamol 500mg","precio":1990,"stock":120}
```

Las respuestas agregan el `id` generado por Oracle. Todos los campos del cuerpo
son obligatorios; PUT reemplaza sus valores. SKU es único, distingue mayúsculas
y admite hasta 100 caracteres; nombre admite hasta 200. Se eliminan espacios
iniciales/finales de SKU y nombre. Precio debe ser no negativo, con hasta 10 dígitos
enteros y 2 decimales; stock debe ser un entero no negativo.

Los conflictos de SKU devuelven 409. Los errores usan respuestas Problem Details.
El catálogo no implementa pedidos ni reservas de inventario: cambiar stock mediante
PUT no equivale a una operación de compra transaccional.

React consume `/api/bff/catalog/medicamentos`; solo el BFF llama a este servicio.
No se habilita CORS para acceso directo desde React.

## Autenticación y cloud

En esta etapa local no se exige autenticación. La dependencia OAuth2 y
`config/SecurityConfig.java` dejan un punto explícito para la futura integración
del equipo. No se requieren variables Azure ni se implementan servicios AWS.

## Pruebas

```powershell
.\mvnw.cmd -B verify
```

Las pruebas HTTP cargan Spring, su cadena de seguridad y JPA sobre H2 en modo Oracle.
Comprueban CRUD, validaciones, persistencia, conflictos SKU y registros inexistentes.
H2 está limitado al alcance de tests; la ejecución normal utiliza Oracle.
