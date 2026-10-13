import { Navigate, Outlet } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth/hooks/useAuth'

// Login y registro no tienen sentido con una sesión abierta.
export function GuestOnly() {
  const { session } = useAuth()
  return session ? <Navigate to={paths.home} replace /> : <Outlet />
}
