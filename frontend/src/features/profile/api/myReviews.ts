import { apiClient } from '@/shared/api/client'

export interface ReceivedReview {
  id: string
  walkId: string
  rating: number
  comment: string | null
  createdAt: string
  authorName: string
}

// reseñas recibidas por el paseador autenticado, en todos sus paseos
export async function loadMyReviews(signal?: AbortSignal): Promise<ReceivedReview[]> {
  const { data } = await apiClient.get<ReceivedReview[]>('/reviews', { signal })
  if (!Array.isArray(data)) throw new Error('No pudimos cargar tus reseñas.')
  return data
}
