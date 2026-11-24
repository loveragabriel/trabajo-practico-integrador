# Revisión de la etapa 3

Revisamos la devolución del 19 de noviembre y ampliamos el informe para que pueda leerse sin depender de las entregas anteriores. Conservamos el formato en negro, el logo de UTN, las capturas reales y el recorrido de ambos roles. El código de la aplicación y su diseño no cambian en esta revisión.

El [informe actualizado en PDF](Informe_Final_Proyecto.pdf) es el documento para la presentación. También dejamos la [versión editable en Word](Informe_Final_Proyecto.docx) y el [contenido consultable desde GitHub](Informe_Final_Proyecto.md). Las referencias a secciones de esta página enlazan a ese contenido.

## Observaciones de la devolución

| Punto | Qué cambiamos | Dónde se puede revisar |
|---|---|---|
| 1 Portada | Agregamos la materia Proyecto Final y el Grupo 137. Reemplazamos la fecha de entrega por el año de cursado 2026 y conservamos los nombres del equipo y el logo. | Portada del PDF y del Word. |
| 2 Evidencia del problema | Incorporamos el informe oficial de la Encuesta Anual de Hogares 2022 y antecedentes documentales del sector. Indicamos el año del dato, su fuente y sus límites. No presentamos entrevistas o encuestas propias que no realizamos. | [Problema y evidencia](Informe_Final_Proyecto.md#1-el-problema-que-quisimos-resolver) y [referencias](Informe_Final_Proyecto.md#referencias). |
| 3 Funcionalidades fuera del alcance | Separamos las restricciones vigentes de las posibles ampliaciones. Para pagos, GPS, chat, notificaciones, app nativa, recurrencias y polígonos explicamos el motivo del recorte y el trabajo adicional que requieren. | [Restricciones y trabajo futuro](Informe_Final_Proyecto.md#4-restricciones-y-trabajo-futuro). |
| 4 Implementado y proyectado | Los requisitos describen lo que existe en el MVP. La tabla de ampliaciones aclara que esas opciones no están implementadas ni probadas. CABA, ARS y la hora local se presentan como restricciones actuales. | [Alcance](Informe_Final_Proyecto.md#3-alcance-y-requisitos), [restricciones](Informe_Final_Proyecto.md#4-restricciones-y-trabajo-futuro) y [resultados](Informe_Final_Proyecto.md#13-resultados-y-estado-de-la-validación). |
| 5 Cierre respecto de la necesidad inicial | Reescribimos la conclusión para explicar qué cambió: información centralizada, cupos controlados, reservas con historial y reseñas vinculadas a la participación. Delimitamos la validación funcional frente a una evaluación de adopción que aún no hicimos. | [Conclusiones](Informe_Final_Proyecto.md#18-conclusiones). |
| 6 Casos de uso relevantes | Agrupamos las funcionalidades y sus capturas bajo una sección explícita de casos de uso. Cada caso identifica actores, condiciones previas, pasos y resultado observado. | [Casos de uso relevantes](Informe_Final_Proyecto.md#14-casos-de-uso-relevantes), CU01 a CU05. |
| 7 Dificultades técnicas | Explicamos el último cupo, la primera publicación simultánea de una agenda, la conservación del historial, la integración entre servicios y la instalación. Relacionamos cada dificultad con su solución y las pruebas disponibles. | [Dificultades y resolución](Informe_Final_Proyecto.md#15-dificultades-técnicas-y-resolución). |
| 8 Entidades y relaciones | Describimos User, Zone, la cobertura walker_zones, Pet, Walk, WalkPet y Review antes del diagrama. Explicamos qué representa cada una y las relaciones uno a muchos y muchos a muchos. Aclaramos qué reglas protegen los servicios y cuáles conserva PostgreSQL. | [Entidades principales](Informe_Final_Proyecto.md#8-entidades-principales) y [relaciones e integridad](Informe_Final_Proyecto.md#9-relaciones-e-integridad-de-los-datos). |
| 9 Video y despliegue | Incorporamos en el informe los enlaces al video y a la aplicación que ya estaban publicados en el README. Corregimos los enlaces rotos a PDFs anteriores. La comprobación final de acceso anónimo y persistencia del despliegue sigue pendiente antes de mesa. | [Acceso al proyecto](Informe_Final_Proyecto.md#resumen-ejecutivo), [instalación y demostración](Informe_Final_Proyecto.md#16-instalación-y-demostración) y [Final Report and Demo](../README.md#final-report-and-demo). |

## Sugerencias que incorporamos

| Sugerencia | Cómo quedó |
|---|---|
| Dar más contexto al problema | La sección 1 presenta la coordinación inicial, las dificultades y la motivación. La implementación se desarrolla después, sin reemplazar la evidencia por una afirmación de impacto. |
| Definir a los actores | La sección 2 diferencia dueño y paseador, sus objetivos, la información que consultan y las acciones que pueden realizar. |
| Identificar los módulos | La sección 6 enumera los módulos y explica sus responsabilidades, sus servicios y su ubicación en frontend y backend. |
| Desarrollar las decisiones de implementación | La sección 7 explica el monolito, las capas, la publicación como disponibilidad, el uso de DTO y la reserva como entidad propia. |
| Explicar las tecnologías | La sección 5 reúne el stack y justifica qué responsabilidad cumple cada componente. La sección 11 muestra cómo intervienen en una solicitud. |
| Presentar el stack de forma fácil de encontrar | La tabla de la sección 5 distingue interfaz, API, persistencia, base de datos, migraciones, seguridad, mapas y distribución. |
| Ampliar la descripción de pruebas | Las secciones 12 y 13 explican niveles, motivos y límites de las pruebas, incluido por qué se revisó el ancho de 390 píxeles y qué verifican las solicitudes concurrentes. |
| Documentar el uso de IA | La sección 17 describe el apoyo de Stitch, ChatGPT y Codex, y deja la responsabilidad de las decisiones y validaciones en el equipo. Esa declaración no agrega autores o coautores a los commits. |
| Mejorar la presentación | Reordenamos títulos y espacios, separamos reglas e integridad en listas y mantuvimos las leyendas de figuras en negro con un tamaño menor al del cuerpo. El índice de Word y PDF permite navegar a las secciones. |

## Verificaciones de esta revisión

- El ZIP recibido coincide con `main` al comenzar la revisión. Trabajamos sobre la rama `docs/correcciones-etapa-3` para que el equipo revise antes de integrar.
- Volvimos a ejecutar las 51 pruebas del frontend: todas aprobadas, sin omisiones. El build y el lint también terminaron correctamente. Vite mantiene una advertencia de tamaño de bundle; no impide compilar y queda como mejora de rendimiento.
- Consultamos las últimas ejecuciones de [backend](https://github.com/loveragabriel/trabajo-practico-integrador/actions/runs/43103149189) y [frontend](https://github.com/loveragabriel/trabajo-practico-integrador/actions/runs/43099890827) en GitHub Actions: ambas aprobadas. El workflow de backend no ejecuta las pruebas optativas con PostgreSQL.
- El informe anterior registraba 211 pruebas de backend con PostgreSQL 17. Conservamos ese resultado como registro previo, no como ejecución nueva. En esta revisión Maven no pudo descargar el POM de Spring Boot por un problema de resolución de Maven Central, antes de compilar.
- Conservamos las capturas del informe anterior como evidencia del recorrido ya realizado. No las presentamos como capturas nuevas del despliegue público.
- Revisamos el índice, las referencias internas, las imágenes y los enlaces relativos de la documentación. El PDF y el Word contienen los enlaces de aplicación, video y repositorio.

## Antes de presentarnos en mesa

- [x] Corregir portada, jerarquía y referencias del informe.
- [x] Desarrollar contexto, fuentes, actores, módulos, entidades y decisiones técnicas.
- [x] Separar reglas actuales, funcionalidades implementadas y ampliaciones posibles.
- [x] Documentar dificultades técnicas, pruebas y uso de herramientas con IA.
- [x] Incorporar los enlaces de video y despliegue al informe.
- [ ] Revisar y aprobar estas correcciones entre los tres integrantes e integrar el PR.
- [ ] Confirmar acceso al video desde una ventana sin sesión o permisos especiales.
- [ ] Repetir ingreso, publicación, reserva, cancelación y reseña desde la aplicación pública.
- [ ] Confirmar persistencia de PostgreSQL al reiniciar el servicio.
- [ ] Repetir las pruebas de integración en una base separada y conservar el resultado para la defensa.

La documentación responde a la devolución. La comprobación pública y la revisión conjunta se mantienen como pendientes para no confundir cambios de informe con nuevas validaciones de funcionamiento.
