# Arquitectura y decisiones de diseño

## Organización

El frontend (React + TypeScript con Vite y Tailwind CSS) consume una API REST Spring Boot. El backend utiliza un monolito por capas y PostgreSQL. Dentro del backend, los paquetes se organizan por dominio y cada módulo separa controlador, servicio, repositorio, entidad y DTO cuando corresponda.

| Componente/capa | Responsabilidad |
| --- | --- |
| React | Páginas, formularios, navegación, agenda y mapa. Consume DTOs de la API. Organizado por feature (ver abajo). |
| Controller | Recibe HTTP, valida formato de entrada y devuelve respuestas. |
| Service | Ejecuta casos de uso, autoriza operaciones y coordina transacciones. |
| Repository | Consulta y persiste entidades con Spring Data JPA. |
| PostgreSQL | Guarda los datos y aplica PK, FK, UNIQUE, CHECK e índices. |
| Shared | Manejo uniforme de errores, configuración y validaciones comunes. |

No se incluyen microservicios: el dominio acotado y el plazo favorecen una sola API.

### Organización del frontend

El frontend se organiza por feature, en espejo con los módulos del backend:

| Carpeta | Contenido |
| --- | --- |
| `app/` | Arranque de la aplicación: router, providers, layout, rutas (`paths.ts`), inicio y 404. |
| `features/<módulo>/` | Todo lo de un módulo: `pages/` (pantallas de sus rutas), componentes, llamadas a la API y hooks. |
| `shared/` | Código sin lógica de negocio: cliente HTTP, componentes de UI genéricos, tipos y utilidades. |

Reglas de dependencia: una feature puede usar `shared/`. Desde fuera de una feature solo se importa
lo que expone su `index.ts` (su API pública); por ejemplo, la sesión se usa con
`import { useAuth } from '@/features/auth'`, sin conocer cómo está organizada por dentro.
`app/router.tsx` es el único lugar que importa páginas directamente, para armar el ruteo. Los
imports usan el alias `@/` (`src/`).

## Decisiones

1. **Una tabla `users`, dos roles fijos y excluyentes.** Evita duplicar identidad y autenticación. Los campos de tarifa y capacidad aplican a paseadores; `OWNER` los mantiene nulos. Los roles no se cambian una vez que existen relaciones de negocio. No se agregan tabla de roles ni tabla `perfil_paseador`.
2. **El paseador publica; el dueño reserva directamente.** La disponibilidad es el conjunto de paseos publicados y futuros. No hay solicitud/aceptación ni tabla de disponibilidad independiente.
3. **`walk_pets` es una entidad de reserva.** Tiene ID, estado y fecha, además de FKs. Debe implementarse como entidad JPA propia, no como un `@ManyToMany` sin atributos. Si se reactiva una reserva cancelada se actualiza la misma fila, respetando su unicidad.
4. **Barrios predefinidos.** El mapa muestra cobertura de forma orientativa. La tabla `zones` no guarda polígonos ni coordenadas; Leaflet necesitará un catálogo estático de referencias por barrio. El texto del punto de encuentro tampoco implica navegación GPS.
5. **Precio y cupo propios del paseo.** Los valores del perfil son sugerencias al publicar; `walks` conserva los valores pactados, aunque el paseador modifique luego sus predeterminados.
6. **Historial sin borrado en cascada.** Desactivar usuarios y cancelar paseos/reservas preserva relaciones. El esquema no implementa borrado de mascotas con reservas; esa operación no es parte del MVP.
7. **SQL canónico en `/database`.** Cumple la estructura pedida. Flyway es una integración futura; no se afirma que esté instalado.
8. **Reseñas por dueño y paseo.** Un dueño puede valorar una vez un paseo aunque haya reservado varias mascotas. `rating_avg` y `review_count` son agregados del paseador y se actualizan en la misma transacción al crear una reseña.

## Seguridad y consistencia planificadas

- Spring Security valida JWT; el ID y rol del actor surgen del contexto autenticado. No confiar en `owner_id` o `author_id` enviados por el navegador para autorizar acciones.
- Guardar contraseñas mediante hash BCrypt; no devolver `password_hash` en DTOs. Dirección y teléfono no se incluyen automáticamente en perfiles públicos.
- Los servicios validan usuario activo, rol, propiedad de mascotas y titularidad de paseos. El frontend mejora la experiencia, pero no sustituye la autorización del backend.
- Para reservar/cancelar, bloquear la fila del paseo en la transacción antes de comprobar cupo y modificar reservas. Para publicar/cambiar horarios, serializar por paseador bloqueando su fila de usuario antes de comprobar solapamientos. Todos los caminos de escritura deberán respetar esas convenciones.
- Un error de validación o autorización no deja escrituras parciales. Las respuestas de error usan un formato uniforme, sin exponer credenciales.
- Las fechas `timestamp` del esquema representan hora local de CABA. Backend, conexión y frontend deben usar consistentemente `America/Argentina/Buenos_Aires`. Una expansión a otras zonas horarias exigirá revisar este diseño.
