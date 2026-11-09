import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import type { Role } from '@/shared/types/user'
import { paths } from './paths'

interface NavItem {
  to: string
  label: string
}

// cada rol ve solo lo que puede usar; el backend vuelve a validar todo
const navByRole: Record<Role, NavItem[]> = {
  CLIENT: [
    { to: paths.walks, label: 'Buscar paseos' },
    { to: paths.bookings, label: 'Mis reservas' },
    { to: paths.pets, label: 'Mis mascotas' },
  ],
  WALKER: [
    { to: paths.myWalks, label: 'Mis paseos' },
    { to: paths.publishWalk, label: 'Publicar paseo' },
  ],
}

const guestNav: NavItem[] = [{ to: paths.walks, label: 'Buscar paseos' }]

function navLinkClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'font-semibold text-emerald-800 underline decoration-2 underline-offset-4' : 'text-slate-600 hover:text-slate-900'
}

export function AppLayout() {
  const { session, signOut } = useAuth()
  const items = session ? navByRole[session.user.role] : guestNav

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <a href="#contenido" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-white focus:p-3">Ir al contenido</a>
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <Link to={paths.home} className="text-lg font-bold text-emerald-700">
            Paseadores
          </Link>

          <ul className="order-last flex w-full flex-wrap gap-x-4 gap-y-2 text-sm sm:order-none sm:w-auto">
            {items.map((item) => (
              <li key={item.to}>
                {/* end: que "Mis paseos" no quede activo dentro de "Publicar paseo" */}
                <NavLink to={item.to} end className={navLinkClass}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>

          <div className="ml-auto flex items-center gap-3 text-sm">
            {session ? (
              <>
                <NavLink to={paths.profile} className={navLinkClass}>
                  {session.user.name}
                </NavLink>
                <button type="button" onClick={signOut} className="text-slate-600 hover:text-slate-900">
                  Salir
                </button>
              </>
            ) : (
              <>
                <NavLink to={paths.login} className={navLinkClass}>
                  Ingresar
                </NavLink>
                <Link
                  to={paths.register}
                  className="rounded-md bg-emerald-600 px-3 py-1.5 font-medium text-white hover:bg-emerald-700"
                >
                  Registrarse
                </Link>
              </>
            )}
          </div>
        </nav>
      </header>

      <main id="contenido" className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
