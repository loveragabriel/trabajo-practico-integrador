# Operaciones de paseos y reservas

Swagger está disponible en `/swagger-ui/index.html`. El catálogo de barrios y el registro e ingreso están abiertos. Los endpoints de paseos, mascotas y perfiles requieren `Authorization: Bearer <token>`. El perfil visible a otros usuarios omite teléfono y correo.

| Método y ruta | Uso |
| --- | --- |
| `GET/PUT /api/walkers/me` | Consultar o reemplazar precio/cupo de referencia y zonas propias. |
| `GET /api/walkers/{id}` | Consultar cobertura y valoración del paseador. |
| `GET /api/walks/by-walker/{id}` | Ver hasta seis próximos paseos de ese paseador con lugares. |
| `GET /api/walkers/{id}/reviews?page=0` | Ver hasta diez reseñas por página, sin identificadores de autores. |
| `POST /api/walks` | Publicar un paseo de un paseador activo en su cobertura. |
| `GET /api/walks` | Buscar paseos futuros con cupo. Sin barrio explícito, se usa el barrio de la cuenta. |
| `GET /api/walks/mine` y `GET /api/walks/{id}` | Agenda del paseador y detalle autorizado. |
| `POST /api/walks/{id}/bookings` | Reservar con `{"petIds":["uuid"]}`. La lista se confirma completa o se rechaza completa. |
| `GET /api/walks/{id}/bookings` | Consultar únicamente reservas propias para ese paseo. |
| `GET /api/walks/bookings/mine?page=0` | Consultar reservas propias, hasta 25 por página. |
| `DELETE /api/walks/{id}/bookings/{bookingId}` | Cancelar una reserva propia hasta dos horas antes del inicio. Se conserva el historial. |
| `POST /api/walks/{id}/cancel` | El paseador cancela antes del inicio y libera todas las reservas del paseo. |
| `POST /api/walks/{id}/start` y `POST /api/walks/{id}/complete` | El paseador puede iniciar durante el horario publicado y finalizar después de su término. |
| `GET/POST /api/walks/{id}/reviews` | Listar con permiso de lectura del paseo o calificar como cliente participante una vez finalizado, con `{"rating":5,"comment":"Buen paseo"}`. |

Las fechas de paseos se ingresan en hora de Buenos Aires. La API devuelve 400 para datos inválidos, 401 sin sesión válida, 403 si la operación no está permitida, 404 si el recurso no existe o no es visible y 409 si una regla de negocio impide la operación.
