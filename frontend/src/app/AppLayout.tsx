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
  return isActive ? 'font-semibold text-emerald-700' : 'text-slate-600 hover:text-slate-900'
}

export function AppLayout() {
  const { session, signOut } = useAuth()
  const items = session ? navByRole[session.user.role] : guestNav

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <Link to={paths.home} className="text-lg font-bold text-emerald-700">
            Paseadores
          </Link>

          <ul className="flex flex-wrap gap-4">
            {items.map((item) => (
              <li key={item.to}>
                {/* end: que "Mis paseos" no quede activo dentro de "Publicar paseo" */}
                <NavLink to={item.to} end className={navLinkClass}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>

          <div className="ml-auto flex items-center gap-4">
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

      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
