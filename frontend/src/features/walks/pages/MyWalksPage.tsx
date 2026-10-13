import { PageHeader } from '@/shared/components/PageHeader'

// Agenda e historial del paseador: sus salidas publicadas, con cupos ocupados
// y acceso al detalle para iniciar, completar o cancelar (RN-29 a RN-33).
export function MyWalksPage() {
  return (
    <section>
      <PageHeader title="Mis paseos" description="Tus salidas publicadas y el historial." />
    </section>
  )
}
