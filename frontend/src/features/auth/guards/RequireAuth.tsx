import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { paths } from '@/app/paths'
import type { Role } from '@/shared/types/user'
import { useAuth } from '@/features/auth/hooks/useAuth'

interface RequireAuthProps {
  // si se omite, alcanza con estar autenticado
  roles?: Role[]
}

// Guarda de rutas: sin sesión manda al login (recordando a dónde quería ir);
// con un rol no permitido manda al inicio.
export function RequireAuth({ roles }: RequireAuthProps) {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to={paths.login} replace state={{ from: location }} />
  }
  if (roles && !roles.includes(session.user.role)) {
    return <Navigate to={paths.home} replace />
  }
  return <Outlet />
}
