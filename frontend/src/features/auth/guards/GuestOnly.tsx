import { Navigate, Outlet, useLocation, type Location } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth/hooks/useAuth'

export function GuestOnly() {
  const { session } = useAuth()
  const { state } = useLocation()
  const from = (state as { from?: Location } | null)?.from
  const target = from ?? (session?.user.role === 'CLIENT' ? paths.walks : paths.home)
  return session ? <Navigate to={target} state={from?.state} replace /> : <Outlet />
}
