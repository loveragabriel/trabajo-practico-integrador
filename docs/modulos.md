# Módulos a desarrollar y responsabilidades


Los nombres de paquete y de carpeta están en inglés, igual que el código. Cada módulo funcional
se ubica en el paquete del backend (`com.tpi.paseadores.<paquete>`) y en la carpeta del frontend
(`frontend/src/<carpeta>`) indicados en la tabla. Algunos módulos funcionales comparten paquete
porque operan sobre las mismas entidades: la cobertura del paseador vive en `user`, y las
reservas y la búsqueda viven en `walk` junto con los paseos.

En el frontend cada carpeta `features/<módulo>` contiene todo lo de ese módulo: sus páginas
(`pages/`), componentes, llamadas a la API y hooks. Las reservas tienen carpeta propia
(`features/bookings`) para la agenda del cliente, aunque en el backend compartan el paquete `walk`.
Lo que no pertenece a un módulo va en `app` (ruteo, layout, inicio y 404) o en `shared`
(código sin lógica de negocio).

| Módulo | Paquete backend | Carpeta frontend | Datos principales | Funcionalidades y responsabilidad |
| --- | --- | --- | --- | --- |
| Autenticación | `auth` | `features/auth` | `users` | Registro de dueño/paseador, login, hash de contraseñas, emisión y validación JWT, acceso según rol. |
| Usuarios y perfiles | `user` | `features/profile` | `users`, `zones` | Consultar/editar perfil propio, barrio de referencia y contacto, consultar usuario activo. |
| Paseadores y cobertura | `user` | `features/profile`, `features/map` | `users`, `walker_zones`, `zones` | Publicar perfil de paseador, tarifa/cupo orientativos y zonas de cobertura; consulta pública sin datos privados. |
| Mascotas | `pet` | `features/pets` | `pets`, `users` | Alta, consulta y edición de mascotas propias; tamaño, raza y observaciones para el paseo. |
| Zonas y mapa | `zone` | `features/map` | `zones`, `walker_zones` | Catálogo de barrios, filtros y visualización orientativa de cobertura; sin editor de polígonos ni GPS. |
| Paseos y agenda | `walk` | `features/walks` | `walks`, `users`, `zones` | Publicar/consultar paseos grupales, cancelar o marcar completado; agenda e historial por rol. |
| Reservas por mascota | `walk` | `features/walks`, `features/bookings` | `walk_pets`, `pets`, `walks` | Incorporar mascotas propias a un paseo, controlar cupo, cancelar/reactivar una reserva; sin aceptación por paseador. |
| Búsqueda de paseos | `walk` | `features/walks` | `walks`, `walk_pets`, `zones`, `users` | Filtrar paseos publicados y futuros por zona/horario; devolver información del paseador y cupo restante. |
| Reseñas | `review` | `features/reviews`, `features/profile` | `reviews`, `walks`, `walk_pets`, `pets`, `users` | Calificar un paseo completado, mostrar reseñas y actualizar promedio/cantidad del paseador. |
| Componentes transversales | `shared` | `app`, `shared` | Sin tabla propia | Ruteo y layout (`app`); configuración, seguridad, validaciones comunes, errores de API, componentes UI, cliente HTTP y tipos. |

## Casos de uso y límites por módulo

### 1. Autenticación

- Registra un usuario con email único, rol fijo y barrio existente; genera el hash en el backend.
- Autentica credenciales y devuelve un token para acceder a recursos protegidos.
- Rechaza credenciales inválidas, usuarios inactivos y operaciones no autorizadas. No expone el hash en respuestas.
- Depende de usuarios. Criterio de aceptación: dueño y paseador pueden iniciar sesión y acceder solo a operaciones permitidas a su rol.

### 2. Usuarios y perfiles

- Mantiene identidad y datos de contacto del usuario autenticado. Los perfiles públicos del paseador usan un DTO separado.
- No permite editar el rol desde el formulario de perfil. La zona de referencia es obligatoria para ambos roles.
- Depende de autenticación y zonas. Criterio de aceptación: un usuario modifica su perfil y no el de otro; una zona inexistente se rechaza.

### 3. Paseadores y cobertura

- Usa `users` con rol `WALKER`; no introduce otra tabla de perfil. Gestiona `walker_zones` sin duplicar relaciones.
- Permite configurar precio/cupo predeterminados y consultar perfiles con cobertura y valoración.
- No duplica la agenda ni la disponibilidad: esos datos provienen de paseos.
- Depende de usuarios y zonas. Criterio de aceptación: solo un paseador puede editar su cobertura; un dueño no puede publicarse como paseador.

### 4. Mascotas

- Registra y edita nombre, raza, tamaño y observaciones de mascotas del dueño autenticado.
- No permite transferir mascotas a otro dueño ni eliminar mascotas con historial en el MVP.
- Depende de usuarios y autenticación. Criterio de aceptación: listar devuelve las mascotas propias y reservar una ajena se rechaza.

### 5. Zonas y mapa

- Expone un catálogo de barrios predefinidos, usado por perfiles, cobertura y búsquedas.
- React representa barrios con un catálogo estático de referencias geográficas, sin atribuir coordenadas a la tabla actual.
- No incluye ABM público de barrios. Criterio de aceptación: elegir un barrio filtra paseos y la cobertura mostrada corresponde a la selección.

### 6. Paseos y agenda

- Un paseador activo publica fecha/hora futura, duración, zona cubierta, encuentro, cupo y precio; el mismo paseador administra el paseo.
- Evita superponer intervalos del mismo paseador. Tras la primera reserva, el MVP no edita horario, zona, precio ni cupo: se cancela y se publica otro paseo si hace falta.
- Cancela todas las reservas activas al cancelar el paseo. Completa un paseo solo después de su hora de finalización.
- Muestra agenda e historial del paseador; para el dueño los obtiene a través de reservas propias.
- Depende de cobertura, zonas y reservas. Criterio de aceptación: un paseo ajeno no puede cancelarse y un horario solapado se rechaza.

### 7. Reservas

- Cada mascota ocupa un cupo; un dueño con varias mascotas genera varias filas. Solo se reserva en un paseo publicado, futuro y con lugar.
- Comprueba propiedad de la mascota y capacidad dentro de una transacción con bloqueo del paseo. Para varias mascotas, la operación es atómica.
- La combinación paseo/mascota es única. Reactivar una cancelación actualiza la misma fila y vuelve a comprobar disponibilidad.
- Un dueño cancela su reserva antes del inicio; al cancelar se libera cupo. No existe estado «pendiente de aceptación».
- Depende de paseos, mascotas y autenticación. Criterio de aceptación: con un único lugar libre, dos reservas concurrentes no pueden confirmarse ambas.

### 8. Búsqueda

- Consulta `walks` publicados con fecha futura, zona solicitada y lugares disponibles; incluye información pública del paseador.
- El cupo restante es `capacity` menos reservas `JOINED`; no cuenta canceladas. Los filtros horarios trabajan con los horarios publicados.
- No implementa recomendaciones inteligentes, notificaciones ni matching automático.
- Depende de paseos, reservas, usuarios y zonas. Criterio de aceptación: un paseo cancelado, pasado o completo no aparece como reservable.

### 9. Reseñas

- Un dueño con al menos una reserva participante `JOINED` valora un paseo `COMPLETED` con puntuación de 1 a 5 y comentario opcional.
- Una reseña por dueño y paseo, independientemente de la cantidad de mascotas. Las reservas no pueden cancelarse después del inicio.
- Actualiza el promedio y cantidad del paseador en la misma transacción, serializando las actualizaciones de ese usuario.
- No incluye edición/eliminación de reseñas ni moderación en el MVP.
- Depende de paseos, reservas, mascotas y usuarios. Criterio de aceptación: no se puede reseñar sin participar, antes de completar ni dos veces.

### 10. Transversal

- Centraliza configuración, validación de entradas y errores homogéneos; el cliente HTTP adjunta el JWT cuando corresponde.
- Los servicios del dominio conservan sus reglas específicas. No se colocan todos los casos de uso en un servicio genérico.
- Criterio de aceptación: errores de formato, autorización y negocio se representan de manera consistente y no filtran datos sensibles.
