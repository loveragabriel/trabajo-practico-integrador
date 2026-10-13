import { PageHeader } from '@/shared/components/PageHeader'

// Pantalla principal del producto.
// TODO: selector de zona (catálogo) + listado de GET /api/walks?zoneId=...
// Cada tarjeta: paseador y rating, fecha/hora, duración, punto de encuentro, precio por perro
// y cupos libres. El backend ya filtra PUBLISHED y futuros; la zona elegida puede ir en ?zoneId
// de la URL (useSearchParams) para que el filtro sobreviva a un refresh o a un link compartido.
export function WalkSearchPage() {
  return (
    <section>
      <PageHeader title="Paseos disponibles" description="Elegí tu zona y sumá a tu mascota a una salida." />
    </section>
  )
}
