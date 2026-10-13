# Propuesta y alcance vigente

Trabajo Final Integrador — Tecnicatura Universitaria en Programación a Distancia, UTN.

Integrantes: Gastón Lell, Juan Cruz Leal y Gabriel Lovera.

## Problema y propósito

Los dueños de perros en CABA necesitan encontrar paseadores que trabajen en su barrio y coordinar horarios. La coordinación mediante mensajes y redes sociales dispersa información sobre cobertura, disponibilidad y reservas. La propuesta es una aplicación web que centralice esa información y permita gestionar paseos e historial.

El objetivo académico es desarrollar una solución acotada, con persistencia relacional, separación de responsabilidades y una interfaz web adaptable a dispositivos móviles.

## Alcance vigente del MVP

- Registro, inicio de sesión y perfiles con dos roles excluyentes: dueño (`OWNER`) o paseador (`WALKER`).
- Perfil del paseador con zonas de cobertura, tarifa orientativa y capacidad predeterminada.
- Gestión de las mascotas del dueño, necesaria para reservar paseos.
- Zonas de CABA predefinidas y visualización de cobertura mediante Leaflet y OpenStreetMap.
- Publicación de paseos grupales: zona, fecha/hora, duración, punto de encuentro, cupo y precio por mascota.
- Búsqueda de paseos publicados por zona y horario, con cupo disponible.
- Reserva directa de una o varias mascotas propias, una reserva por mascota; cancelación de reservas antes del inicio.
- Agenda e historial para ambos roles.
- Calificación y reseña del paseo completado por dueños participantes.

## Fuera del MVP

- Pagos online integrados.
- Seguimiento GPS en tiempo real.
- Aplicación móvil nativa.
- Dibujo libre de zonas o modelado con polígonos geoespaciales.
- Calendario recurrente de disponibilidad independiente de los paseos publicados.
- Flujo de solicitud y aceptación/rechazo por el paseador.
- Roles múltiples por usuario y administración de roles configurables.


## Stack y estrategia

| Capa | Tecnología planificada |
| --- | --- |
| Interfaz | React + TypeScript (Vite) + Tailwind CSS, con pnpm |
| API REST | Java + Spring Boot |
| Persistencia | PostgreSQL + Spring Data JPA |
| Autenticación | Spring Security + JWT |
| Mapa | Leaflet + OpenStreetMap |
| Despliegue | Frontend Vercel/Netlify; backend Render/Railway; PostgreSQL Railway/Supabase, a definir en implementación |

La arquitectura es un monolito por capas, organizado por módulos de dominio. Las responsabilidades y decisiones se detallan en [arquitectura](arquitectura.md), [módulos](modulos.md) y [modelo de datos](modelo-datos.md).
