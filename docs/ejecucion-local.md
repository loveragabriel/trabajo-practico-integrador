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
npm install -g pnpm@11.25.0
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

La clave del ejemplo es solo para desarrollo local. En un despliegue configuramos una clave privada propia. Podemos exportar las variables en la terminal o usar un `.env` local en `backend` con formato `CLAVE=valor`: Spring lo importa opcionalmente al arrancar desde esa carpeta. Conservamos ese archivo fuera del repositorio.

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

Vite reenvía las consultas `/api` al backend del puerto 8080. La búsqueda requiere una cuenta. Si no hay paseos publicados, la lista aparece vacía. Para crear uno seguimos [Publicación de paseos](publicacion-paseos.md).

## Probar el recorrido completo

1. Entrá como paseador, configurá un barrio en **Mi perfil** y publicá una salida futura con un solo lugar.
2. En otro perfil del navegador, registrate como dueño, agregá una mascota y buscá la salida por barrio y día. Abrí el detalle y reservá para esa mascota.
3. En **Mis reservas → Próximas**, cancelá si faltan más de dos horas, verificá la cancelación en **Historial** y volvé a reservar.
4. Como paseador, iniciá la salida. Como dueño, comprobá que aparezca en **En curso**. Finalizala como paseador, volvé a **Historial** y elegí **Calificar el paseo**.

El fondo del mapa requiere conexión a OpenStreetMap. El mapa indica el barrio, no el seguimiento de la salida.

El catálogo de barrios (`GET /api/zones`) responde sin token. Para consultar paseos, agenda o reservas desde Swagger, iniciá sesión con una cuenta de prueba y autorizá el token; las rutas protegidas responden `401` sin una sesión válida.

## Pruebas

Frontend, desde la raíz:

```bash
cd frontend
pnpm build
pnpm lint
node --import ./tests/register-hooks.mjs --test tests/*.test.ts
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
