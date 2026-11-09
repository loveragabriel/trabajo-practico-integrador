import { apiClient, ApiRequestError } from '@/shared/api/client'

export interface Review {
  id: string
  walkId: string
  authorId: string
  rating: number
  comment: string | null
  createdAt: string
}

export interface ReviewCreatePayload {
  rating: number
  comment?: string
}

export async function loadWalkReviews(walkId: string, signal?: AbortSignal): Promise<Review[]> {
  const { data } = await apiClient.get<Review[]>(`/walks/${walkId}/reviews`, { signal })
  return data
}

// RN: un dueño que participó califica un paseo finalizado, una sola vez; el backend valida todo
export async function createReview(walkId: string, payload: ReviewCreatePayload): Promise<Review> {
  const { data } = await apiClient.post<Review>(`/walks/${walkId}/reviews`, payload)
  return data
}

// el backend ya devuelve el mensaje de negocio en español; no hace falta traducirlo
export function reviewError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 401) return 'Ingresá nuevamente para continuar.'
    return error.message
  }
  return error instanceof Error ? error.message : 'No pudimos completar la operación.'
}
