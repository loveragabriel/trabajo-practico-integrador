## Estado de las tareas

| Tarea de ClickUp | Estado en este avance | Evidencia o dependencia |
| --- | --- | --- |
| Eliminar placeholders obsoletos y PDF | Ya resuelta en la base | Commit `6e77659`. |
| Manejo global de errores y validaciones comunes | Base existente más complemento | Implementación de `ApiError`, `GlobalExceptionHandler` y respuestas de seguridad en `196f230`. Se agrega `RequestFormatExceptionHandler` para parámetros mal formados, JSON inválido y HTTP 405/415, preservando lo anterior. |
| Backend: zonas | Implementado | Entidad de solo lectura, repositorio, servicio, DTO y GETs de catálogo/detalle en `com.tpi.paseadores.zone`. Usa la tabla y el catálogo Flyway existentes. |
| Backend: paseos y agenda | Pendiente por dependencias | Faltan usuarios/paseador/cobertura; cancelación e historial necesitan coordinar reservas. No se crean entidades duplicadas ni otra interpretación del ciclo del paseo. |
| Backend: búsqueda de paseos | Pendiente por dependencias | Requiere paseos, reservas y contrato de consulta. Debe excluir paseos cancelados, pasados y completos contando solo reservas `JOINED`. |
| Frontend: zonas y mapa | Parcial | Cliente del catálogo, hook y selector reutilizable implementados. Falta configurar Leaflet y referencias geográficas; falta incorporar los componentes en las pantallas y conectar la cobertura del paseador. No hay aún un mapa terminado. |
| Frontend: paseos y agenda | Pendiente por dependencias | Faltan API de paseos, sesión real y perfil/cobertura del paseador. |
| Frontend: búsqueda de paseos | Pendiente por dependencias | Faltan API de búsqueda y mapa. Se mantiene `WalkSearchPage` sin reemplazar sus TODO por un listado simulado. |



## API de zonas

| Método y ruta | Respuesta |
| --- | --- |
| `GET /api/zones` | `200` con una lista de `{id, name, city}` ordenada por nombre; `[]` si no hay datos. |
| `GET /api/zones/{id}` | `200` con el barrio; `404` si el UUID no existe; `400` si está mal formado. |
| Sin JWT válido | `401`, según la configuración de seguridad vigente. |
| Intento de POST/PUT/DELETE sobre una ruta existente | `405` para usuarios autenticados; no existe ABM público. |

La respuesta no expone entidades JPA ni incorpora coordenadas, polígonos o nuevos barrios. Los UUID se obtienen de la base: no están fijados en el frontend, ya que el seed existente los genera.

Dejamos el catálogo de barrios como una consulta de solo lectura. Podemos usar `ZoneService.findById` para comprobar si un barrio existe y `ZoneRepository.findById` cuando necesitemos relacionarlo con otra entidad.

## Complemento de errores

El advice nuevo captura únicamente excepciones de formato/HTTP conocidas, con precedencia sobre el handler genérico. No reemplaza los handlers de negocio, 404, autorización ni el fallback 500 de Gabriel. Todos conservan el formato `ApiError`.

- UUID/número con formato inválido: 400, sin repetir el valor recibido.
- Parámetro requerido ausente: 400 con el nombre del campo.
- JSON ilegible o valor incompatible con el DTO: 400 genérico, sin filtrar contenido interno.
- Método no soportado: 405 y encabezado `Allow`.
- Content-Type no soportado: 415.

## Componentes del catálogo en React

Los componentes están en `frontend/src/features/map`, sin agregar dependencias ni tocar el cliente HTTP compartido que todavía está vacío.

- `loadZones(token, signal)`: consulta `/api/zones` por el proxy existente, valida la respuesta y no muestra mensajes internos del servidor.
- `useZones(token)`: carga, reintento y cancelación de peticiones; evita que una respuesta anterior sobrescriba una sesión nueva.
- `ZoneSelect`: selector controlado, etiqueta accesible y valores UUID del catálogo.
- `ZoneCatalog`: combina carga, selector, sesión requerida, errores, vacío y reintento.




## Verificación

Con Java 21, desde `backend`:

```bash
bash mvnw test
```

En Windows también puede usarse `mvnw.cmd test`. Desde `frontend`:

```bash
pnpm install --frozen-lockfile
pnpm build
pnpm lint
node --test tests/zones.test.ts
```
