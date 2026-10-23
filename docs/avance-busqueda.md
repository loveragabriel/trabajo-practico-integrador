# Búsqueda de paseos y mapa

Integramos la búsqueda con el catálogo de barrios y el mapa para consultar las salidas disponibles por zona y horario. Usamos los datos de la base y mostramos un resultado vacío cuando todavía no hay paseos que cumplan los filtros.

## Funcionamiento

Consultamos únicamente paseos publicados, futuros, con lugares libres y asociados a un paseador activo con rol `WALKER`. Calculamos los lugares libres restando de `capacity` las reservas `JOINED`. Las canceladas no ocupan lugar.

Cada tarjeta muestra el barrio, la fecha y hora, la duración, el punto de encuentro, el precio por mascota y los lugares disponibles. Del paseador mostramos su nombre, promedio y cantidad de reseñas. No exponemos teléfono, email ni contraseña.

En el frontend conservamos los filtros en la URL para poder compartir la búsqueda, recargarla y volver a un resultado anterior. Incluimos paginación, carga, error, reintento y lista vacía.

## Contrato de la API

La consulta es pública: `GET /api/walks`.

| Parámetro | Uso |
| --- | --- |
| `zoneId` | UUID del barrio. Sin este parámetro consultamos todas las zonas. Un UUID inexistente devuelve una lista vacía. |
| `startsFrom` | Inicio del rango, incluido. Fecha y hora local de CABA, por ejemplo `2030-01-01T10:00:00`. |
| `startsTo` | Fin del rango, sin incluir. Cuando se envían ambos límites, debe ser posterior a `startsFrom`. |
| `page` | Página desde cero. El valor predeterminado es `0`. |
| `size` | Tamaño de página entre 1 y 50. El valor predeterminado es `12`, que también usa la pantalla. |

La respuesta contiene `items`, `page`, `size` y `hasNext`. Ordenamos los paseos por horario de inicio y luego por UUID. Con los mismos datos, ese orden permite recorrer las páginas de manera estable.

Un filtro mal formado o un rango inválido devuelve `400` con el formato `ApiError`. El acceso público alcanza solo a esta consulta: las escrituras y las otras rutas mantienen sus permisos.

La consulta usa JDBC y parámetros SQL sobre las tablas actuales. No genera paseos de ejemplo ni modifica reservas. Al implementar la publicación, los paseos guardados en `walks` aparecerán en esta búsqueda si cumplen sus condiciones.

## Mapa y presentación

El mapa enfoca el barrio seleccionado. Sin un filtro de barrio, muestra las referencias del catálogo. Las ubicaciones son aproximadas y sirven para orientar la búsqueda; el punto de encuentro es el publicado en cada paseo.

El diseño de la búsqueda usa fondos claros, tarjetas redondeadas y botones azules. El mapa no muestra recorridos ni la posición de personas o mascotas. Las reseñas solo se presentan cuando existen datos; si el paseador todavía no tiene reseñas, lo indicamos en la tarjeta.

## Lo que falta integrar

Para continuar la publicación necesitamos completar la cobertura del paseador y comprobar las superposiciones de horarios. La agenda, la cancelación, el completado y el historial deben integrarse con las reservas.

El cupo que muestra una búsqueda puede cambiar. La futura operación de reserva tendrá que comprobarlo nuevamente dentro de una transacción con bloqueo. La consulta por sí sola no garantiza un lugar.

También tenemos pendiente unificar el diseño del inicio, el registro y el encabezado con la referencia visual. Mantendremos los formularios y los contratos que ya funcionan al trabajar en esas pantallas.

## Verificación

Podemos abrir `/walks` sin iniciar sesión, elegir un barrio y un horario, recargar el enlace y comprobar que los filtros se conservan. Una fecha Hasta igual o anterior a Desde debe mostrar un mensaje. Los paseos cancelados, pasados o completos no deben aparecer en los resultados.

Los comandos de arranque, las consultas curl y las pruebas con una base separada están en [Ejecución local](ejecucion-local.md).
