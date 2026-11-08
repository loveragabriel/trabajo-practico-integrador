import type { Zone } from '@/features/map'
import { apiClient, ApiRequestError } from '@/shared/api/client'
import { validWalkId } from './walkReadContract'

export interface WalkPublicationOptions {
  defaultPrice: number | null
  defaultCapacity: number | null
  zones: Zone[]
}

export interface WalkPublicationRequest {
  zoneId: string
  startTime: string
  durationMinutes: number
  capacity: number
  pricePerPet: number
  meetingPoint: string
}

export async function loadWalkPublicationOptions(signal: AbortSignal): Promise<WalkPublicationOptions> {
  const { data } = await apiClient.get<WalkPublicationOptions>('/walkers/me', { signal }).catch((error: unknown) => {
    if (signal.aborted) throw error
    throw new Error(error instanceof ApiRequestError && error.status === 403
      ? 'Solo un paseador activo puede publicar.'
      : 'No pudimos cargar tus barrios de servicio. Intentá nuevamente.')
  })
  if (!data || !Array.isArray(data.zones) || !data.zones.every(zone => zone && validWalkId(zone.id)
    && typeof zone.name === 'string' && typeof zone.city === 'string')
    || (data.defaultPrice !== null && (!Number.isFinite(data.defaultPrice) || data.defaultPrice < 0))
    || (data.defaultCapacity !== null && (!Number.isInteger(data.defaultCapacity) || data.defaultCapacity < 1))) {
    throw new Error('No pudimos leer tu configuración de publicación.')
  }
  return data
}

export async function publishWalk(request: WalkPublicationRequest): Promise<string> {
  const { data } = await apiClient.post<{ id: string }>('/walks', request)
  if (!data || !validWalkId(data.id)) {
    throw new Error('No pudimos confirmar la publicación. Revisá Mis paseos antes de volver a enviarla.')
  }
  return data.id
}
