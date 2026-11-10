import { apiClient } from '@/shared/api/client'
import type { Zone } from '@/features/map'

export interface WalkerPublicProfile {
  id: string
  fullName: string
  defaultPrice: number | null
  defaultCapacity: number | null
  ratingAvg: number
  reviewCount: number
  zones: Zone[]
}

export async function getWalkerPublicProfile(walkerId: string, signal?: AbortSignal): Promise<WalkerPublicProfile> {
  const { data } = await apiClient.get<WalkerPublicProfile>(`/walkers/${walkerId}`, { signal })
  return data
}
