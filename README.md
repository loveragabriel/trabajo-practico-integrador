# TP 🐾

Aplicación web que conecta dueños de mascotas con paseadores de perros en la Ciudad Autónoma de Buenos Aires, permitiendo publicar la disponibilidad de paseadores por zona, gestionar una agenda de paseos y visualizar la cobertura geográfica de cada paseador.

**Trabajo Final Integrador — Tecnicatura Universitaria en Programación a Distancia (TUPaD), UTN**

## Integrantes

- Gastón Lell
- Juan Cruz Leal
- Gabriel Lovera

## Índice

- [Propósito](#propósito)
- [Alcance del MVP](#alcance-del-mvp)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Modelo de datos](#modelo-de-datos)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Roadmap de entregas](#roadmap-de-entregas)

## Propósito

La Ciudad Autónoma de Buenos Aires atraviesa un cambio demográfico estructural con una población de mascotas que supera ampliamente a la de niños y adolescentes (493.676 perros y 368.176 gatos frente a 460.696 niños y niñas menores de 14 años, según INDEC 2022). Esto ha generado una demanda creciente de paseadores de perros, cuya coordinación con los dueños hoy se resuelve de forma informal (WhatsApp, redes sociales, boca en boca), sin pocas opciones de un sistema que centralice disponibilidad, zona de cobertura y agenda.

Este proyecto busca reemplazar esa coordinación informal por una plataforma centralizada y confiable.

## Alcance del MVP

**Incluye:**
- Registro y perfil de dos roles: Dueño y Paseador
- Publicación de perfil de paseador con zonas de cobertura (barrios predefinidos), disponibilidad horaria y tarifa referencial
- Visualización de zonas de cobertura en un mapa (Leaflet + OpenStreetMap)
- Búsqueda y matching de paseadores por zona y disponibilidad
- Agenda de paseos: solicitar, aceptar/rechazar, ver horarios pactados
- Historial básico de paseos agendados

**Fuera de alcance:**
- Pagos online integrados
- Seguimiento GPS en tiempo real durante el paseo
- Aplicación móvil nativa (la web será responsive)
- Sistema de calificaciones/reviews (mejora futura)
- Dibujo libre de zonas en el mapa (se usan barrios predefinidos, no polígonos geoespaciales)

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | TypeScript + React |
| Backend | Java + Spring Boot |
| Base de datos | PostgreSQL |
| Mapa | Leaflet + OpenStreetMap |
| Autenticación | Spring Security + JWT |
| Despliegue Frontend | Vercel / Netlify |
| Despliegue Backend | Render / Railway |
| Despliegue Base de Datos | Railway / Supabase |

## Arquitectura

Arquitectura monolítica en capas (controller → service → repository) con Spring Boot. Se descartó una arquitectura de microservicios porque el dominio del proyecto (dueños, paseadores, paseos, zonas) no presenta módulos con ciclos de vida ni equipos independientes que justifiquen esa complejidad operativa; un monolito en capas es ejecutable dentro del plazo de la cursada y no introduce sobre-ingeniería.

## Modelo de datos

Diagrama entidad-relación completo:


El script DDL completo se encuentra en [`/database/schema.sql`](./database/schema.sql).

## Decisiones de diseño

### Tabla única de usuarios (`USERS`) con campo `role`

Se optó por una única tabla `USERS` con un campo `role`, en lugar de tablas separadas desde el inicio (`OWNERS` y `WALKERS`), porque ambos roles comparten el mismo núcleo de datos de identidad —nombre, email, contraseña, DNI— y el sistema tiene solo dos roles fijos y mutuamente excluyentes, sin necesidad de que un usuario acumule varios roles a la vez ni de agregar roles nuevos dinámicamente.

El trade-off es consciente: se evita la complejidad de una tabla de roles normalizada (`roles` + tabla puente `user_roles`), que solo aportaría valor si el sistema necesitara roles configurables o múltiples por usuario — un caso fuera del alcance del MVP. Los atributos que sí son exclusivos del Paseador (tarifa, descripción, zonas, disponibilidad) se aíslan en `WALKER_PROFILES` mediante una relación 1:1 (equivalente relacional a herencia por tabla de subtipo), evitando columnas vacías en los Dueños sin necesidad de fragmentar también los datos de identidad que ambos roles comparten por igual.

### `WALKER_PROFILES` como tabla separada

Contiene únicamente los atributos propios del rol Paseador (`description`, `hourly_rate`). Se mapea en Java con una relación `@OneToOne` (o herencia `JOINED` de JPA) hacia `User`, permitiendo que el resto del dominio (`WALKS`, `WALKER_ZONES`, `WALKER_AVAILABILITY`) referencie al perfil de paseador sin acoplarse a la tabla genérica de usuarios.

### Relación N:M entre paseadores y zonas

Un paseador puede cubrir varias zonas y una zona puede tener varios paseadores, por lo que la relación se modela con una tabla intermedia (`WALKER_ZONES`) en lugar de una FK directa en `WALKER_PROFILES`.

### Sin arquitectura de microservicios

Ver sección [Arquitectura](#arquitectura).

## Instalación y ejecución

> ⚠️ Sección a completar a medida que se desarrollen el backend y el frontend.

```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

Variables de entorno necesarias (backend): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`.

## Estructura del repositorio

```
pawmatch/
├── backend/          # API REST — Java + Spring Boot
├── frontend/         # Aplicación web — TypeScript + React
├── database/
│   └── schema.sql    # Script DDL — PostgreSQL
├── docs/
│   └── propuesta.md  # Propuesta de proyecto (1ª Entrega)
└── README.md
```

## Roadmap de entregas

| Hito | Fecha máxima | Estado |
|---|---|---|
| 1ª Entrega — Propuesta + Repositorio | 30/08/2026 | ✅ |
| 2ª Entrega — Diseño de BD y módulos | 27/09/2026 | 🔄 En curso |
| Entrega Final — Informe, video y despliegue | 14/11/2026 | ⏳ Pendiente |
| Defensa Oral | En mesa de examen | ⏳ Pendiente |