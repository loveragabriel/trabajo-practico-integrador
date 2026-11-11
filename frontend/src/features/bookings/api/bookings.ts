import { apiClient, ApiRequestError } from '@/shared/api/client'
import { walkStatusLabels, type WalkStatus } from '@/features/walks/api/walkReadContract'

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export type BookingStatus = 'JOINED' | 'CANCELLED'
export type ClientBookingPeriod = 'UPCOMING' | 'IN_PROGRESS' | 'HISTORY'

export interface Booking {
  id: string
  petId: string
  petName: string
  status: BookingStatus
  joinedAt: string
}

export interface ClientBooking {
  bookingId: string
  bookingStatus: BookingStatus
  petId: string
  petName: string
  walkId: string
  walkStatus: WalkStatus
  startTime: string
  endTime: string
  meetingPoint: string
  pricePerPet: number
  zoneName: string
  walker: { id: string; name: string; ratingAvg: number; reviewCount: number }
}

export interface ClientBookingsResult { items: ClientBooking[]; page: number; size: number; hasNext: boolean }

function isClientBooking(value: unknown): value is ClientBooking {
  if (!value || typeof value !== 'object') return false
  const b = value as ClientBooking
  return uuid.test(b.bookingId) && (b.bookingStatus === 'JOINED' || b.bookingStatus === 'CANCELLED')
    && Object.hasOwn(walkStatusLabels, b.walkStatus)
    && uuid.test(b.petId) && typeof b.petName === 'string' && uuid.test(b.walkId)
    && typeof b.startTime === 'string' && typeof b.endTime === 'string' && typeof b.meetingPoint === 'string'
    && typeof b.pricePerPet === 'number' && Number.isFinite(b.pricePerPet) && b.pricePerPet >= 0
    && typeof b.zoneName === 'string'
    && !!b.walker && uuid.test(b.walker.id) && typeof b.walker.name === 'string'
    && typeof b.walker.ratingAvg === 'number' && typeof b.walker.reviewCount === 'number'
}

// RN-21/22/23/24/26: sumar una mascota propia a un paseo publicado
export async function joinWalk(walkId: string, petId: string): Promise<Booking> {
  const { data } = await apiClient.post<Booking>(`/walks/${walkId}/join/${petId}`)
  return data
}

// RN-27/28: cancelar hasta 2hs antes del horario del paseo
export async function cancelBooking(walkId: string, petId: string): Promise<void> {
  await apiClient.delete(`/walks/${walkId}/join/${petId}`)
}

// "mis reservas": simétrico a la agenda del paseador
export async function loadMyBookings(period: ClientBookingPeriod, page: number, signal?: AbortSignal): Promise<ClientBookingsResult> {
  const { data } = await apiClient.get<ClientBookingsResult>('/walks/bookings/mine', {
    params: { period, page, size: 12 }, signal,
  })
  if (!data || !Array.isArray(data.items) || !data.items.every(isClientBooking)
    || data.page !== page || data.size !== 12 || typeof data.hasNext !== 'boolean') {
    throw new Error('No pudimos cargar tus reservas.')
  }
  return data
}

// el backend ya devuelve el mensaje de negocio en español (RN-xx); no hace falta traducirlo
export function bookingError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 401) return 'Ingresá nuevamente para continuar.'
    return error.message
  }
  return error instanceof Error ? error.message : 'No pudimos completar la operación.'
}
