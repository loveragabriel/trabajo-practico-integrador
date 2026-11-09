import { useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth/hooks/useAuth'

export function GuestOnly() {
  const { session } = useAuth()
  const [enteredWithSession] = useState(() => !!session)
  return enteredWithSession ? <Navigate to={paths.home} replace /> : <Outlet />
}
