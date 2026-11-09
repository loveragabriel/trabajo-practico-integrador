# Validación de la entrega

Usamos cuentas y mascotas de prueba en una base separada. Al iniciar la aplicación sobre una base vacía, Flyway crea las tablas y carga los barrios de CABA.

| Recorrido | Cómo reproducirlo |
| --- | --- |
| Paseador | Registrarse, configurar Palermo y valores de referencia en Mi perfil. Publicar una salida para el día siguiente y verificar que aparezca en Mis paseos. Intentar publicar otra que se superponga: debe rechazarse. |
| Cliente | Registrarse como dueño, crear una mascota y buscar el paseo en Palermo. Reservar, revisar Mis reservas, cancelar con más de dos horas de anticipación y volver a reservar. El cupo se libera al cancelar. |
| Último cupo | Ejecutar las pruebas de integración contra PostgreSQL. Dos clientes con mascotas distintas intentan tomar el mismo último lugar; solo uno lo consigue. |
| Estados y reseñas | Con datos de prueba de un paseo cuyo horario ya terminó, marcarlo finalizado como paseador. Como cliente participante, calificarlo una vez y comprobar el promedio y el total en el perfil. Un cliente sin reserva no puede calificarlo. |
| Permisos | Probar sin token una operación privada. Probar con un cliente una operación de paseador y con otro dueño la cancelación de una reserva ajena. |
| Pantalla pequeña | Abrir el perfil del paseador y el historial desde un móvil. Los datos de prueba se identifican como tales en las capturas. |

En la prueba de navegador adelantamos el escenario de finalización con un cambio de hora **solo en la base de prueba**; usamos la interfaz para finalizar y reseñar. No se modifica la hora de un paseo publicado desde la aplicación. Las teselas de OpenStreetMap requieren conexión; la lista de barrios y el punto de encuentro siguen visibles.

## Pruebas automatizadas

Desde la raíz del proyecto, con una base PostgreSQL exclusiva para pruebas y las variables que figuran en [Ejecución local](ejecucion-local.md):

```bash
(cd backend && TPI_DB_TESTS=true ./mvnw -B test)
(cd frontend && pnpm install --frozen-lockfile && pnpm build && pnpm lint)
node --test frontend/tests/*.test.ts
```

En Git Bash de Windows se puede usar `MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd -B test"` desde `backend`, con `TPI_DB_TESTS=true` exportada antes. Sin esa variable se omiten los casos que necesitan PostgreSQL.
