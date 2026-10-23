# Catálogo de zonas y respuestas de error

Compartimos el catálogo de barrios entre el registro, los perfiles, la cobertura y la búsqueda. Los identificadores vienen de la base de datos; no mantenemos una segunda lista de UUID en el frontend.

## Consulta de barrios

| Método y ruta | Respuesta |
| --- | --- |
| `GET /api/zones` | Lista de `{id, name, city}` ordenada por nombre. Devuelve `[]` si no hay barrios. |
| `GET /api/zones/{id}` | Devuelve el barrio solicitado. Un UUID inexistente devuelve `404`; un formato inválido devuelve `400`. |

Ambas consultas son públicas y no necesitan un token. El catálogo es de solo lectura: no ofrecemos operaciones para crear, editar ni eliminar barrios desde la API. Con una sesión válida, un método no implementado sobre una ruta existente devuelve `405`.

Las respuestas no incorporan coordenadas ni polígonos. Para comprobar la existencia de un barrio podemos usar `ZoneService.findById` o `ZoneRepository.findById`.

## Errores

Mantenemos el formato común `ApiError` para errores de validación, formato, autenticación y reglas del negocio. `RequestFormatExceptionHandler` se ocupa de los errores de formato y de métodos HTTP; los demás casos siguen en los manejadores compartidos.

| Situación | Estado HTTP |
| --- | --- |
| UUID, número o fecha con formato inválido | `400`. |
| Parámetro obligatorio ausente | `400`. |
| JSON ilegible | `400`. |
| Método no implementado para una ruta existente | `405`, con encabezado `Allow`. |
| Tipo de contenido no admitido | `415`. |
| Sesión requerida ausente o inválida | `401`. |
| Usuario autenticado sin permiso | `403`. |

Los mensajes no devuelven trazas, contraseñas ni detalles internos del servidor.

## Componentes de React

Los componentes están en `frontend/src/features/map`.

- `loadZones(token?, signal?)` consulta el catálogo público, valida su formato y permite cancelar la petición. El token es opcional para mantener la compatibilidad con las pantallas que ya lo envían.
- `useZones(token?)` gestiona la carga, los errores y el reintento. Descarta respuestas que pertenecen a una consulta anterior.
- `ZoneSelect` recibe el barrio elegido y los barrios disponibles.
- `ZoneCatalog` combina el selector con los estados de carga, error y lista vacía.
- `ZoneCoverageMap` representa los barrios mediante referencias geográficas aproximadas.

La integración con los filtros está explicada en [Búsqueda de paseos y mapa](avance-busqueda.md). Los comandos para levantar el proyecto y ejecutar las pruebas están en [Ejecución local](ejecucion-local.md).
