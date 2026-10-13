// API pública de la feature: el resto de la app importa solo desde '@/features/auth'.
// Lo que no se exporta acá es interno y puede reorganizarse sin romper a nadie.
export { AuthProvider } from './context/AuthProvider'
export type { AuthUser, Session } from './context/AuthContext'
export { GuestOnly } from './guards/GuestOnly'
export { RequireAuth } from './guards/RequireAuth'
export { useAuth } from './hooks/useAuth'
