# Project Proposal and Scope

Capstone project for UTN's distance learning program in Computer Programming.

Team: Gastón Lell, Juan Cruz Leal and Gabriel Lovera.

## Problem

Owners in Buenos Aires need to find walkers who cover their neighborhood and agree on a time. When they arrange everything through messages, it is easy to lose track of where to meet or how many spots are left. We built a web app where both roles can see the same information and check their history.

## What We Included

- Registration, sign-in and profiles for owners (`CLIENT`) and walkers (`WALKER`).
- A walker profile with service neighborhoods, a suggested price and a default capacity.
- Pet records and a neighborhood catalog displayed with Leaflet and OpenStreetMap.
- Group walks with a date, duration, meeting point, capacity and price per pet.
- Search by neighborhood, date or time and direct booking for one pet per request.
- Cancellation within the allowed window, schedules and booking history.
- Reviews of completed walks by owners who participated.

We left out online payments, live tracking, native mobile apps, custom map polygons and recurring availability. Owners book directly, so there is no approval queue for walkers.

## Technology

React, TypeScript, Vite and Tailwind CSS power the interface. The REST API uses Java and Spring Boot, with Spring Security and JWT for authentication. PostgreSQL stores the data, and Flyway applies the migrations. The Docker image packages the web app with the API.

For more detail, see the [architecture](arquitectura.md), [modules](modulos.md) and [data model](modelo-datos.md).
