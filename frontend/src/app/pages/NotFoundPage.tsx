import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { PageHeader } from '@/shared/components/PageHeader'

export function NotFoundPage() {
  return (
    <section className="py-12 text-center">
      <PageHeader title="Página no encontrada" description="La dirección no existe o fue movida." />
      <Link to={paths.home} className="text-emerald-700 underline">
        Volver al inicio
      </Link>
    </section>
  )
}
