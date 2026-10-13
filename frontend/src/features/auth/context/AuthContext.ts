import { createContext } from 'react'
import type { Role } from '@/shared/types/user'

export interface AuthUser {
  id: string
  name: string
  role: Role
}

export interface Session {
  token: string
  user: AuthUser
}

export interface AuthContextValue {
  session: Session | null
  signIn: (session: Session) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
