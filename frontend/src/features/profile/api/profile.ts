import { apiClient } from '@/shared/api/client'
import type { UserProfile } from '../types'

export interface ProfileUpdatePayload {
  fullName: string
  phone?: string
  zoneId: string
}

export async function getMyProfile(signal?: AbortSignal): Promise<UserProfile> {
  const { data } = await apiClient.get<UserProfile>('/users/me', { signal })
  return data
}

export async function updateMyProfile(payload: ProfileUpdatePayload): Promise<UserProfile> {
  const { data } = await apiClient.put<UserProfile>('/users/me', payload)
  return data
}
