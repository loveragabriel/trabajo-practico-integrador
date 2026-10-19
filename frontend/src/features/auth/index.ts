// Configuración para la API pública sin autenticación
export { AuthProvider } from './context/AuthProvider'
export type { AuthUser, Session } from '@/shared/api/session'
export { GuestOnly } from './guards/GuestOnly'
export { RequireAuth } from './guards/RequireAuth'
export { useAuth } from './hooks/useAuth'
