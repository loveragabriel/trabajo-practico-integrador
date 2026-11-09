# Agenda y detalle de paseos

Agregamos la consulta de la agenda del paseador y el detalle de los paseos. Conservamos la entidad `Walk`, sus estados, el repositorio con bloqueo y la separación de la búsqueda en `walk/search`. Las nuevas consultas están en `walk/query` y no modifican paseos ni reservas.

## API

| Consulta | Acceso y resultado |
| --- | --- |
| `GET /api/walks` | Conserva la búsqueda pública por barrio y horario. |
| `GET /api/walks/mine` | Devuelve los paseos del paseador autenticado. Requiere una cuenta activa con rol `WALKER`. |
| `GET /api/walks/{id}` | Requiere sesión y una cuenta activa. Devuelve el detalle permitido para ese usuario. |

La agenda admite `period=UPCOMING` o `period=HISTORY`, `page` desde cero y `size` entre 1 y 50. Los valores predeterminados son `UPCOMING`, `0` y `12`. La respuesta contiene `items`, `page`, `size` y `hasNext`.

En próximas salidas incluimos los estados `PUBLISHED`, `FULL` e `IN_PROGRESS` cuyo intervalo todavía no terminó, ordenados por inicio ascendente. En el historial incluimos los cancelados, los completados y los intervalos finalizados, ordenados por inicio descendente. Consultar el historial no cambia automáticamente el estado de un paseo.

Obtenemos la identidad de la agenda del token, no de un identificador enviado como filtro. Un cliente no puede consultar la agenda de un paseador. La falta de sesión devuelve `401`; una cuenta sin permiso devuelve `403`.

El detalle de un paseo `PUBLISHED` o `FULL` futuro se puede consultar con una cuenta activa. Para los demás casos, mostramos el detalle al paseador propietario o a un cliente que tenga una reserva `JOINED` de una mascota propia. Un paseo inexistente o un historial no accesible devuelve `404`. Tampoco mostramos paseos de un paseador inactivo.

La respuesta incluye barrio, inicio y fin, duración, encuentro, precio, estado, cupo total, lugares ocupados y libres, y un resumen público del paseador. Los cupos ocupados cuentan solo reservas `JOINED`. No devolvemos datos privados de contacto ni una lista de participantes.

## Interfaz

En `/my-walks` el paseador puede alternar entre próximas salidas e historial. Conservamos el período y la página en la URL, e incluimos carga, resultado vacío, errores y reintento.

Las tarjetas de búsqueda y agenda permiten abrir `/walks/{id}`. Esa pantalla muestra los datos del paseo y la referencia del barrio en el mapa. Mantenemos el requisito de autenticación que ya tenía el backend para el detalle: un visitante recibe una invitación a ingresar.

No agregamos botones de reserva, cancelación ni cambio de estado mientras esas operaciones sigan pendientes. La consulta de cupos no garantiza una reserva; la operación de incorporación tendrá que volver a comprobarlos dentro de la transacción con bloqueo.

El alta de paseos se describe en [Publicación de paseos](publicacion-paseos.md).

## Dependencias para continuar

| Pendiente | Qué necesitamos acordar |
| --- | --- |
| Configuración de cobertura en el perfil | El backend de cobertura ya permite cargar las zonas de servicio y los valores predeterminados. Falta integrar su edición en el perfil para completar el recorrido desde la interfaz. |
| Cancelar y cambiar estados | Integración con reservas y transiciones de `WalkStatus`. La cancelación debe actualizar las reservas correspondientes en la misma transacción. |
| Historial del cliente | Contrato de reservas propias y conexión con `features/bookings`. Esta entrega agrega la agenda del paseador. |
| Edición de un paseo | Confirmar las reglas de horario, precio y cupo con las que usa el módulo de reservas antes de agregar escrituras. |
| Reseñas | El servicio actual comprueba que el cliente esté activo. Falta verificar paseo completado y participación, y actualizar promedio y cantidad del paseador, como requieren los criterios del módulo. Conservamos ese código para coordinar la integración. |

## Verificación local

Con el backend y el frontend levantados, usamos una cuenta registrada e ingresamos con email y contraseña. Los accesos de desarrollo con token `dev` solo sirven para recorrer pantallas: no autentican las consultas protegidas.

Desde `frontend`:

```bash
pnpm install --frozen-lockfile
pnpm build
pnpm lint
node --import ./tests/register-hooks.mjs --test tests/*.test.ts
```

Desde `backend`, con Java 21:

```bash
bash mvnw test
```

En Git Bash de Windows:

```bash
MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd test"
```

Las pruebas con base de datos necesitan `TPI_DB_TESTS=true`, `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET`, apuntando a una base exclusiva de pruebas. En Linux o macOS:

```bash
TPI_DB_TESTS=true bash mvnw test
```

En Git Bash de Windows:

```bash
TPI_DB_TESTS=true MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd test"
```

La base actual usa el rol `CLIENT` y los cinco estados de `WalkStatus`. Se modificó V1 en los avances previos del equipo; una base local inicializada con una versión anterior puede tener otro esquema o un checksum de Flyway distinto. Para validar desde cero usamos una base nueva con otro nombre, sin borrar los datos existentes ni cambiar las migraciones recibidas.

## Casos de integración detectados

Al iniciar sesión desde el detalle, la guarda `GuestOnly` puede redirigir al inicio aunque la pantalla de login haya recibido un destino. Con el comportamiento actual podemos ingresar y abrir el paseo desde la búsqueda o desde Mis paseos. Tenemos que coordinar la corrección del retorno automático.

También observamos un error de Leaflet al salir de una pantalla mientras el mapa está animando un cambio de zoom. La propuesta separada desactiva esas animaciones de zoom y de reencuadre. Conservamos los archivos compartidos de autenticación y mapa en este avance; el paquete incluye una propuesta independiente para revisarlos sin aplicar esos cambios automáticamente.

El resultado de las pruebas y los límites del entorno temporal se detallan en el informe de verificación incluido en el paquete.
