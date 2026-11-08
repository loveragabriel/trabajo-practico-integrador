# Publicación de paseos

La pantalla **Publicar paseo** está en `/my-walks/new`. Cargamos las zonas de servicio y los valores predeterminados con `GET /api/walkers/me`, del módulo de cobertura. El barrio de referencia del perfil no cuenta por sí solo como cobertura. Si todavía no hay zonas de servicio, la pantalla explica qué falta y no muestra el formulario.

## Contrato

`POST /api/walks` requiere un JWT válido de un paseador activo. El paseador se obtiene de la sesión. El cuerpo es:

```json
{
  "zoneId": "UUID-de-un-barrio-de-servicio",
  "startTime": "2030-01-01T11:00",
  "durationMinutes": 45,
  "capacity": 3,
  "pricePerPet": 2500.50,
  "meetingPoint": "Entrada de la plaza"
}
```

El horario se interpreta en Buenos Aires. La salida requiere al menos dos horas de anticipación; la duración mínima es de 30 minutos y debe ser múltiplo de 15. El cupo es entero positivo, el precio es no negativo con hasta dos decimales y el punto de encuentro no puede estar vacío.

La respuesta es `201 Created`, con `{"id":"UUID-del-paseo"}` y `Location: /api/walks/{id}`. El paseo queda `PUBLISHED` y puede consultarse en la agenda, el detalle y la búsqueda pública. Respondemos `400` ante campos inválidos, `401` sin sesión, `403` si la cuenta no es un paseador activo y `409` si hay superposición.

Comprobamos el intervalo `[inicio, fin)`: dos paseos consecutivos pueden tocarse en sus extremos. Bloquean el horario los paseos publicados, completos de cupo y en curso; los cancelados y finalizados no lo bloquean. Un bloqueo transaccional de PostgreSQL por paseador serializa la comprobación y el alta, incluso cuando publica su primer paseo. Conservamos las migraciones y el repositorio de reservas; si en el futuro se permite reprogramar, esa operación tendrá que usar el mismo bloqueo de agenda.

## Probar el recorrido

1. Levantamos la base, el backend y el frontend como indica [Ejecución local](ejecucion-local.md).
2. Registramos una cuenta de paseador e iniciamos sesión. Hasta integrar el formulario de cobertura del perfil, configuramos al menos una zona mediante `PUT /api/walkers/me` con el encabezado `Authorization: Bearer <JWT>` en nuestro cliente HTTP. Por ejemplo, enviamos `{"defaultPrice":2500,"defaultCapacity":3,"zoneIds":["UUID-del-barrio"]}`, usando un ID del catálogo `GET /api/zones`.
3. Abrimos **Publicar paseo**. Elegimos un barrio disponible, fecha futura, duración, cupo, precio y punto de encuentro.
4. Confirmamos que el alta abre el detalle. Verificamos que el mismo paseo aparece en **Mis paseos** y en **Buscar paseos**, con todos los cupos libres.
5. Intentamos publicar otra salida del mismo paseador que se superponga; debe aparecer el aviso y no debe crearse otra fila. Un paseo que empiece exactamente al terminar el anterior sí es válido.

Las pruebas de servicio y HTTP se ejecutan con `mvnw test`. La clase `WalkPublicationDatabaseIntegrationTest` requiere `TPI_DB_TESTS=true` y una base de pruebas PostgreSQL. Incluye dos publicaciones simultáneas del mismo paseador. Es una comprobación diferente de dos clientes intentando reservar la última vacante, que depende del módulo de reservas.

## Qué queda pendiente

La publicación no modifica la cobertura ni crea reservas. Faltan la interfaz de cobertura y perfil público, las reservas y su historial, y la cancelación y los cambios de estado del paseo con sus efectos sobre reservas. Tampoco estamos dando por cerradas la edición de paseos, las reseñas ni la prueba del flujo final completo.
