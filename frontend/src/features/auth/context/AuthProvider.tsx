import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { AuthContext, type Session } from './AuthContext'

const STORAGE_KEY = 'session'

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

// Guarda la sesión (JWT + datos mínimos del usuario) para sobrevivir a un refresh.
// El rol sirve solo para decidir qué mostrar: la autorización real la hace el backend.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(readStoredSession)

  const signIn = useCallback((next: Session) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setSession(null)
  }, [])

  const value = useMemo(() => ({ session, signIn, signOut }), [session, signIn, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
