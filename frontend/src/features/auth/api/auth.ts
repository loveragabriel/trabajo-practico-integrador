import { apiClient } from '@/shared/api/client'
import type { Session } from '@/shared/api/session'
import type { Role } from '@/shared/types/user'

export interface RegisterPayload {
  fullName: string
  email: string
  password: string
  phone?: string
  role: Role
  zoneId: string
  // solo aplican si role es WALKER (RN-04); el backend los ignora para un dueño
  defaultPrice?: number
  defaultCapacity?: number
}

export interface LoginPayload {
  email: string
  password: string
}

export async function register(payload: RegisterPayload): Promise<Session> {
  const { data } = await apiClient.post<Session>('/auth/register', payload)
  return data
}

export async function login(payload: LoginPayload): Promise<Session> {
  const { data } = await apiClient.post<Session>('/auth/login', payload)
  return data
}
