# Ejecución local

Para trabajar en la aplicación levantamos dos procesos: la API en el puerto 8080 y el frontend en el 5173. PostgreSQL debe estar iniciado antes de ejecutar el backend.

## Requisitos

Usamos Java 21, PostgreSQL 14 o superior, Node.js 24 y pnpm. El proyecto incluye el wrapper de Maven. Podemos comprobar las herramientas con:

```bash
java -version
node --version
pnpm --version
```

Si pnpm no está instalado:

```bash
npm install -g pnpm
```

Los comandos siguientes parten de la raíz de `trabajo-practico-integrador` y están preparados para Git Bash en Windows.

## Base de datos

Si todavía no existe la base, la creamos una sola vez:

```bash
createdb -h localhost -p 5432 -U postgres paseadores
```

El comando solicita la contraseña de PostgreSQL. Si `createdb` no está en el PATH, podemos crear la base desde pgAdmin: conectarnos a la base `postgres` y ejecutar `CREATE DATABASE paseadores;`.

Si ya estamos usando una base del proyecto, conservamos su nombre y sus datos. Flyway aplica las migraciones al iniciar el backend; en una base nueva crea las tablas y carga los barrios. No ejecutamos además los scripts de creación manual sobre esa misma base.

## Terminal 1: backend

Desde la raíz del proyecto, ajustamos el nombre de la base, el puerto y el usuario a nuestra instalación:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/paseadores'
export DB_USER='postgres'
read -r -s -p 'Contraseña de PostgreSQL: ' DB_PASSWORD
echo
export DB_PASSWORD
export JWT_SECRET='paseadores-clave-de-desarrollo-local-2026'
cd backend
MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd spring-boot:run"
```

La clave del ejemplo es solo para desarrollo local. En un despliegue configuramos una clave privada propia. Las variables se exportan en la misma terminal donde ejecutamos el backend; el proyecto no carga un archivo `.env` por sí solo.

En Linux o macOS usamos las mismas variables y reemplazamos el último comando por:

```bash
bash mvnw spring-boot:run
```

Dejamos esa terminal abierta. La API queda disponible en `http://localhost:8080` y Swagger en `http://localhost:8080/swagger-ui/index.html`.

## Terminal 2: frontend

Abrimos otra terminal en la raíz del proyecto:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Abrimos `http://localhost:5173`. Para revisar directamente la búsqueda, usamos `http://localhost:5173/walks`.

Vite reenvía las consultas `/api` al backend del puerto 8080. La búsqueda puede abrirse sin sesión. Si no hay paseos publicados en la base, veremos una lista vacía. La publicación, las reservas y otras pantallas todavía están pendientes de completar.

## Comprobar la API

Con ambos procesos en ejecución, desde otra terminal:

```bash
curl --fail-with-body -sS 'http://localhost:8080/api/zones'
curl --fail-with-body -sS 'http://localhost:8080/api/walks?size=12'
curl -i 'http://localhost:8080/api/walks?zoneId=no-es-un-uuid'
curl -i 'http://localhost:8080/api/walks?startsFrom=2030-01-02T10%3A00&startsTo=2030-01-01T10%3A00'
curl -i -X POST 'http://localhost:8080/api/walks'
```

Esperamos `200` en las dos primeras consultas, `400` en los filtros inválidos y `401` en el POST sin sesión.

En la interfaz comprobamos el selector de barrio, los filtros de horario, la recarga del enlace, la limpieza y el resultado vacío. El fondo del mapa requiere conexión a OpenStreetMap.

## Pruebas

Frontend, desde la raíz:

```bash
cd frontend
pnpm build
pnpm lint
node --test tests/*.test.ts
```

Backend, desde la raíz en Git Bash de Windows:

```bash
cd backend
MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd test"
```

En Linux o macOS usamos `bash mvnw test` desde `backend`.

Para ejecutar también las pruebas de integración, configuramos `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` hacia una base exclusiva de pruebas. Desde `backend`, en Git Bash:

```bash
TPI_DB_TESTS=true MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd test"
```

En Linux o macOS:

```bash
TPI_DB_TESTS=true bash mvnw test
```

La prueba de autenticación guarda un usuario de prueba; las de búsqueda revierten sus datos al terminar. Usamos una base separada para no agregar datos de prueba a la que compartimos durante el desarrollo.

Para detener el backend o el frontend, usamos `Ctrl+C` en su terminal.
