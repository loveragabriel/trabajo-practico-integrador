import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { panel, primary, secondary } from '@/shared/components/formStyles'

export function HomePage() {
  const { session } = useAuth()
  const walker = session?.user.role === 'WALKER'
  const steps = walker
    ? [['Configurá tu cobertura', 'Elegí los barrios donde trabajás y definí un precio y cupo de referencia.'],
      ['Publicá un paseo', 'Indicá horario, punto de encuentro y lugares disponibles.'],
      ['Organizá tu agenda', 'Consultá tus salidas, sus cupos y el historial de paseos.']]
    : [['Buscá por barrio', 'Encontrá paseos con lugares disponibles y revisá el horario y el punto de encuentro.'],
      ['Reservá por mascota', 'Sumá a tu perro al grupo y encontrá la confirmación en Mis reservas.'],
      ['Contá cómo te fue', 'Calificá la salida cuando el paseador marque el paseo como finalizado.']]

  return <section className="space-y-8">
    <div className="overflow-hidden rounded-3xl bg-emerald-950 px-6 py-10 text-white sm:px-10 sm:py-14">
      <p className="mb-3 text-sm font-semibold text-emerald-200">Paseos grupales en Buenos Aires</p>
      <h1 className="max-w-2xl text-4xl font-bold tracking-tight sm:text-5xl">{walker ? 'Organizá tus próximos paseos' : 'Encontrá un paseo para tu perro'}</h1>
      <p className="mt-4 max-w-xl text-emerald-100">{walker
        ? 'Publicá tus salidas en los barrios donde trabajás y llevá tu agenda en un solo lugar.'
        : 'Elegí una salida en tu barrio, conocé al paseador y reservá un lugar para tu mascota.'}</p>
      <div className="mt-7 flex flex-wrap gap-3">
        <Link to={walker ? paths.publishWalk : paths.walks} className={primary}>{walker ? 'Publicar un paseo' : 'Buscar paseos'}</Link>
        <Link to={walker ? paths.myWalks : session ? paths.bookings : paths.register} className={secondary}>
          {walker ? 'Ver mi agenda' : session ? 'Ver mis reservas' : 'Crear cuenta'}
        </Link>
      </div>
    </div>
    <div className="grid gap-4 sm:grid-cols-3">
      {steps.map(([title, body], index) => <article key={title} className={panel}>
        <span className="inline-flex size-8 items-center justify-center rounded-full bg-emerald-100 text-sm font-bold text-emerald-900">{index + 1}</span>
        <h2 className="mb-2 mt-3 text-lg font-bold">{title}</h2>
        <p className="text-sm text-slate-600">{body}</p>
      </article>)}
    </div>
  </section>
}
