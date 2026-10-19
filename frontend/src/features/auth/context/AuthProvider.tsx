import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { onUnauthorized } from '@/shared/api/client'
import { clearSession, readSession, writeSession, type Session } from '@/shared/api/session'
import { AuthContext } from './AuthContext'

// Guarda la sesión (JWT + datos mínimos del usuario) para sobrevivir a un refresh.
// El rol sirve solo para decidir qué mostrar: la autorización real la hace el backend.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(readSession)

  const signIn = useCallback((next: Session) => {
    writeSession(next)
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  // si el cliente HTTP detecta un 401 con sesión activa, el token dejó de ser válido
  useEffect(() => onUnauthorized(signOut), [signOut])

  const value = useMemo(() => ({ session, signIn, signOut }), [session, signIn, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
