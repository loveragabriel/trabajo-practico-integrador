import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'

export function HomePage() {
  const { session } = useAuth()
  const isWalker = session?.user.role === 'WALKER'

  return (
    <section className="py-12 text-center">
      <h1 className="text-4xl font-bold">Paseos grupales para tu perro</h1>
      <p className="mx-auto mt-4 max-w-xl text-slate-600">
        Los paseadores publican sus salidas por zona; vos elegís una y sumás a tu mascota mientras haya lugar.
      </p>
      <Link
        to={isWalker ? paths.publishWalk : paths.walks}
        className="mt-8 inline-block rounded-md bg-emerald-600 px-5 py-2.5 font-medium text-white hover:bg-emerald-700"
      >
        {isWalker ? 'Publicar un paseo' : 'Buscar paseos en mi zona'}
      </Link>
    </section>
  )
}
