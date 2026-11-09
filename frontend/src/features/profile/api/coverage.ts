import type { Zone } from '@/features/map'
import type { WalkSummary } from '@/features/walks/api/search'
import { getWalkerReviews } from '@/features/reviews/api/reviews'
import { apiClient } from '@/shared/api/client'

export interface Coverage { defaultPrice: number | null; defaultCapacity: number | null; zones: Zone[] }
export interface PublicWalker extends Coverage { id: string; fullName: string; ratingAvg: number; reviewCount: number }

export async function getCoverage(signal: AbortSignal) {
  return (await apiClient.get<Coverage>('/walkers/me', { signal })).data
}
export async function saveCoverage(payload: { defaultPrice: number | null; defaultCapacity: number | null; zoneIds: string[] }) {
  return (await apiClient.put<Coverage>('/walkers/me', payload)).data
}
export async function loadWalkerProfile(id: string, signal: AbortSignal) {
  const [profile, walks, reviews] = await Promise.all([
    apiClient.get<PublicWalker>(`/walkers/${id}`, { signal }),
    apiClient.get<WalkSummary[]>(`/walks/by-walker/${id}`, { signal }),
    getWalkerReviews(id, signal),
  ])
  return { profile: profile.data, walks: walks.data, reviews }
}
