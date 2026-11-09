import { apiClient } from '@/shared/api/client'
import type { Zone } from '@/features/map'

export interface Coverage {
  defaultPrice: number | null
  defaultCapacity: number | null
  zones: Zone[]
}

export interface CoverageUpdatePayload {
  defaultPrice: number | null
  defaultCapacity: number | null
  zoneIds: string[]
}

export async function getCoverage(signal?: AbortSignal): Promise<Coverage> {
  const { data } = await apiClient.get<Coverage>('/walkers/me', { signal })
  return data
}

export async function updateCoverage(payload: CoverageUpdatePayload): Promise<Coverage> {
  const { data } = await apiClient.put<Coverage>('/walkers/me', payload)
  return data
}
