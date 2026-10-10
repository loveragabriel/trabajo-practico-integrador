# Modelo de datos relacional

Motor: PostgreSQL. Fuente ejecutable del diseño: [01_schema.sql](../database/01_schema.sql). Fuente editable del diagrama: [schema.dbml](../database/schema.dbml).

La imagen [er-diagram.png](er-diagram.png) recibida con el proyecto representa estas siete tablas. SQL/DBML precisan además los ENUM, índices y restricciones que no se muestran completamente en esa imagen. No es una base documental.

## Entidades

| Tabla | Clave primaria | Datos y responsabilidad |
| --- | --- | --- |
| `zones` | `id` UUID | Nombre de barrio y ciudad; combinación única. Catálogo inicial parcial de CABA. |
| `users` | `id` UUID | Identidad, email, hash de contraseña, contacto, rol, zona de referencia y estado activo. Para paseadores, precio/cupo predeterminados y agregados de reseñas. |
| `walker_zones` | `(walker_id, zone_id)` | Relación entre paseadores y zonas cubiertas; evita pares duplicados. |
| `pets` | `id` UUID | Mascota con dueño, nombre, raza/tamaño opcionales y notas. |
| `walks` | `id` UUID | Paseo grupal de un paseador: zona, fecha/hora, duración, cupo, precio por mascota, encuentro y estado. |
| `walk_pets` | `id` UUID | Reserva de una mascota en un paseo, estado, fecha de incorporación y notas. Par paseo/mascota único. |
| `reviews` | `id` UUID | Valoración de un paseo por un dueño: autor, puntuación, comentario y fecha. Par paseo/autor único. |

## Relaciones y cardinalidades

| Relación | Cardinalidad y significado |
| --- | --- |
| `zones` → `users` | Una zona tiene cero o muchos usuarios; cada usuario tiene una zona de referencia. |
| `users` ↔ `zones`, vía `walker_zones` | Un paseador puede cubrir varias zonas; una zona puede tener varios paseadores. El rol se valida en el servicio. |
| `users` → `pets` | Un dueño tiene cero o muchas mascotas; cada mascota tiene un dueño. |
| `users` → `walks` | Un paseador publica cero o muchos paseos; cada paseo tiene un paseador. |
| `zones` → `walks` | Una zona tiene cero o muchos paseos; cada paseo pertenece a una zona. |
| `walks` ↔ `pets`, vía `walk_pets` | Un paseo tiene cero o muchas reservas; una mascota puede participar en distintos paseos. |
| `walks` → `reviews` | Un paseo tiene cero o muchas reseñas; cada reseña corresponde a un paseo. |
| `users` → `reviews` | Un dueño escribe cero o muchas reseñas; cada reseña tiene un autor. |

## Tipos, estados y transiciones

| Tipo | Valores |
| --- | --- |
| `user_role` | `OWNER`, `WALKER` |
| `pet_size` | `SMALL`, `MEDIUM`, `LARGE`; dato opcional |
| `walk_status` | `PUBLISHED`, `COMPLETED`, `CANCELLED` |
| `booking_status` | `JOINED`, `CANCELLED` |

Paseos: `PUBLISHED` puede pasar a `CANCELLED` o `COMPLETED`; los estados finales no vuelven a publicarse. Se completa después de finalizar el intervalo. Reservas: `JOINED` puede pasar a `CANCELLED` antes del inicio. Puede reactivarse como `JOINED` si el paseo sigue publicado, futuro y con cupo; se reutiliza la fila. No hay transiciones después del inicio salvo la cancelación del paseo por su paseador, que cancela sus reservas activas.

Los ENUM restringen valores posibles, pero no controlan por sí mismos las transiciones: eso corresponde al backend.

## Restricciones implementadas en SQL

- PK UUID con generación predeterminada; todas las relaciones usan FKs.
- Email único mediante índice sobre `LOWER(email)`; además el servicio normaliza espacios y formato al registrar.
- Barrio/ciudad, cobertura, paseo/mascota y paseo/autor sin duplicados.
- Duración y cupo mayores que cero; precios no negativos; puntuación entera entre 1 y 5.
- Promedio entre 0 y 5 y cantidad de reseñas no negativa. Sin reseñas, ambos agregados comienzan en cero.
- Un dueño no tiene valores de precio/cupo predeterminados del paseador.
- Fechas de creación, estado inicial y usuario activo con valores por defecto.
- Sin borrado en cascada para preservar el historial.
- Índices para búsquedas por zona/estado/fecha y consultas por propietario/paseador.

## Reglas a implementar en los servicios

| Regla | Control planificado |
| --- | --- |
| Rol correcto | `pets.owner_id` y `reviews.author_id` deben apuntar a `OWNER`; `walks.walker_id` y `walker_zones.walker_id` a `WALKER` activo. Una FK a `users` no garantiza el rol. |
| Autorización | Actor autenticado administra solo sus mascotas, su cobertura y sus paseos; reserva/reseña como dueño. |
| Cobertura | La zona de un paseo debe estar en `walker_zones` del paseador al publicar. |
| Horarios | Publicar solo fechas futuras y evitar solapamiento del mismo paseador: los intervalos `[inicio, fin)` no se intersectan. Bloqueo por paseador durante validación/escritura. |
| Cupo | Contar filas `JOINED`; no superar `walks.capacity`. Bloquear el paseo antes de comprobar e insertar/reactivar. |
| Reserva múltiple | Validar todas las mascotas y lugares, y confirmar todo en una única transacción. |
| Cancelación | Validar estado/horario; cancelar el paseo cancela sus reservas `JOINED` en la misma transacción. |
| Reseña elegible | Paseo completado y participación efectiva del dueño; luego recalcular agregados del paseador en la misma transacción. |


## Criterios de representación

Los importes usan `NUMERIC`, evitando errores de coma flotante. Para el MVP se interpretan en pesos argentinos (ARS); no existe soporte multimoneda. `timestamp` representa hora local de CABA y requiere configuración consistente en toda la aplicación. Las zonas no almacenan geometrías; `meeting_point` es texto.

