# Despliegue de Paseadores

El contenedor construye el frontend y lo sirve junto con Spring Boot. La interfaz, la API y Swagger usan el mismo dominio. Al recibir una ruta del frontend directamente, Spring sirve el HTML y React decide si necesita iniciar sesión. PostgreSQL se mantiene por separado.

## Levantar la versión para el equipo

En una terminal, desde la raíz:

```bash
cp .env.example .env
# Completar DB_PASSWORD y JWT_SECRET en .env antes de continuar.
docker compose up --build
```

Abrir `http://localhost:8080`. Swagger queda en `http://localhost:8080/swagger-ui/index.html`. El catálogo se crea con Flyway. Para detener los servicios sin borrar datos usamos `docker compose down`.

## Publicar una instancia de demostración

1. Crear una base PostgreSQL vacía con almacenamiento persistente y copiar su host, puerto, nombre y usuario. Configurar el acceso según las instrucciones del proveedor.
2. Crear un servicio web que construya el `Dockerfile` de la raíz a partir de la rama aprobada. Configurar `DB_URL` en formato JDBC, `DB_USER`, `DB_PASSWORD` y una `JWT_SECRET` privada. Exponer el puerto 8080 o configurar `SERVER_PORT`. No guardar secretos en el repositorio.
3. Usar `/api/zones` para comprobar que la aplicación y la base respondan. Probar ingreso, búsqueda, reserva y una navegación directa con HTTPS.
4. Registrar el enlace probado en el README y en el informe. Crear las cuentas de demo con datos ficticios.

El contenedor y las instrucciones preparan el despliegue. La URL pública, las credenciales y el enlace al video se incorporan cuando estén disponibles.
