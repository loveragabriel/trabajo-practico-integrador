import { apiClient } from '@/shared/api/client'

export interface PublicReview { id: string; rating: number; comment: string | null; createdAt: string }
export interface WalkReview extends PublicReview { authorId: string; walkId: string }

export async function getWalkerReviews(walkerId: string, signal: AbortSignal) {
  return (await apiClient.get<PublicReview[]>(`/walkers/${walkerId}/reviews`, { signal })).data
}
export async function getWalkReviews(walkId: string, signal: AbortSignal) {
  return (await apiClient.get<WalkReview[]>(`/walks/${walkId}/reviews`, { signal })).data
}
export async function createReview(walkId: string, rating: number, comment: string) {
  await apiClient.post(`/walks/${walkId}/reviews`, { rating, comment: comment.trim() || null })
}
