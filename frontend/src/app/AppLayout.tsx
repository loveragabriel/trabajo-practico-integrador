import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { Icon } from '@/shared/components/Icon'
import type { Role } from '@/shared/types/user'
import { paths } from './paths'

interface NavItem {
  to: string
  label: string
  icon: string
}

// cada rol ve solo lo que puede usar; el backend vuelve a validar todo
const navByRole: Record<Role, NavItem[]> = {
  CLIENT: [
    { to: paths.walks, label: 'Buscar', icon: 'explore' },
    { to: paths.bookings, label: 'Reservas', icon: 'calendar_today' },
    { to: paths.pets, label: 'Mascotas', icon: 'pets' },
  ],
  WALKER: [
    { to: paths.myWalks, label: 'Mis paseos', icon: 'pets' },
    { to: paths.publishWalk, label: 'Publicar', icon: 'add_circle' },
  ],
}

function initials(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || '?'
}

export function AppLayout() {
  const { session, signOut } = useAuth()
  const items = session ? navByRole[session.user.role] : []

  return (
    <div className="min-h-screen w-full bg-surface pt-safe pb-safe">
      <header className="fixed inset-x-0 top-0 z-50 border-b border-outline-variant/30 bg-surface/90 pt-safe shadow-sm backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between gap-4 px-5">
          <Link to={paths.home} className="flex items-center gap-1.5">
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary text-on-primary">
              <Icon name="pets" filled className="text-[16px]" />
            </div>
            <span className="text-title-sm font-bold text-primary">Paseadores</span>
          </Link>

          {/* mismos items que el bottom nav, visibles solo en desktop (ver <nav> más abajo) */}
          {session && (
            <div className="hidden flex-1 items-center justify-center gap-2 md:flex">
              {items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end
                  className={({ isActive }) =>
                    `flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-label-md transition-colors ${
                      isActive ? 'bg-primary-container text-on-primary' : 'text-on-surface-variant hover:bg-surface-container-low'
                    }`
                  }
                >
                  <Icon name={item.icon} className="text-[18px]" />
                  {item.label}
                </NavLink>
              ))}
            </div>
          )}

          {session ? (
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={signOut}
                className="text-label-md text-on-surface-variant hover:text-on-surface"
              >
                Salir
              </button>
              <Link
                to={paths.profile}
                aria-label="Mi perfil"
                className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-container text-label-md font-semibold text-on-primary-container"
              >
                {initials(session.user.name)}
              </Link>
            </div>
          ) : (
            <div className="flex items-center gap-4">
              <NavLink to={paths.login} className="text-label-md text-on-surface-variant hover:text-primary">
                Ingresar
              </NavLink>
              <Link
                to={paths.register}
                className="rounded-full bg-primary-container px-3.5 py-1.5 text-label-md text-on-primary shadow-sm"
              >
                Registrarse
              </Link>
            </div>
          )}
        </div>
      </header>

      <main className={`mx-auto max-w-5xl px-5 pt-20 ${session ? 'pb-24 md:pb-8' : 'pb-8'}`}>
        <Outlet />
      </main>

      {session && (
        <nav className="fixed inset-x-0 bottom-0 z-50 border-t border-outline-variant/30 bg-surface/90 pb-safe shadow-[0_-4px_20px_rgba(47,93,80,0.06)] backdrop-blur-xl md:hidden">
          <div className="mx-auto flex h-16 max-w-5xl items-center justify-around px-2">
            {items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end
                className={({ isActive }) =>
                  `flex min-w-[64px] flex-col items-center justify-center gap-0.5 rounded-xl py-1.5 transition-colors ${
                    isActive ? 'text-primary' : 'text-on-surface-variant'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon name={item.icon} filled={isActive} className="text-[22px]" />
                    <span className={`text-label-sm ${isActive ? 'font-semibold' : ''}`}>{item.label}</span>
                  </>
                )}
              </NavLink>
            ))}
          </div>
        </nav>
      )}
    </div>
  )
}
