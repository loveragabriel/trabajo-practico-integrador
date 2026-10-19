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

const STORAGE_KEY = 'session'

export function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

export function writeSession(session: Session) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY)
}
