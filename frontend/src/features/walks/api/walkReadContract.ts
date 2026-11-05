import type { WalkSummary } from './search'

export const walkStatusLabels = {
  PUBLISHED: 'Publicado', FULL: 'Completo', IN_PROGRESS: 'En curso',
  COMPLETED: 'Finalizado', CANCELLED: 'Cancelado',
} as const

export type WalkStatus = keyof typeof walkStatusLabels
export type AgendaPeriod = 'UPCOMING' | 'HISTORY'
export interface AgendaFilters { period: AgendaPeriod; page: number }
export interface WalkDetails extends WalkSummary { endTime: string; status: WalkStatus; bookedSpots: number }
export interface WalkAgenda { items: WalkDetails[]; page: number; size: number; hasNext: boolean }

export function readAgendaFilters(params: URLSearchParams): AgendaFilters {
  const period = params.get('period') ?? 'UPCOMING'
  const page = params.get('page') ?? '0'
  if (period !== 'UPCOMING' && period !== 'HISTORY') throw new Error('Elegí Próximos o Historial para consultar tus paseos.')
  if (!/^\d+$/.test(page) || !Number.isSafeInteger(Number(page)) || Number(page) > 2147483647) {
    throw new Error('La página indicada no es válida.')
  }
  return { period, page: Number(page) }
}

export function agendaParams(filters: AgendaFilters) {
  const params = new URLSearchParams({ period: filters.period })
  if (filters.page) params.set('page', String(filters.page))
  return params
}

export function validWalkId(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)
}

export function isWalkDetails(value: unknown): value is WalkDetails {
  if (!value || typeof value !== 'object') return false
  const w = value as WalkDetails
  return validWalkId(w.id) && validWalkId(w.zoneId) && typeof w.zoneName === 'string'
    && typeof w.startTime === 'string' && Number.isFinite(Date.parse(`${w.startTime}-03:00`))
    && typeof w.endTime === 'string' && Number.isFinite(Date.parse(`${w.endTime}-03:00`))
    && Number.isInteger(w.durationMinutes) && w.durationMinutes > 0 && typeof w.meetingPoint === 'string'
    && typeof w.pricePerPet === 'number' && Number.isFinite(w.pricePerPet) && w.pricePerPet >= 0
    && Number.isInteger(w.capacity) && w.capacity > 0 && Number.isInteger(w.bookedSpots) && w.bookedSpots >= 0
    && Number.isInteger(w.availableSpots) && w.availableSpots >= 0 && w.availableSpots <= w.capacity
    && Object.hasOwn(walkStatusLabels, w.status)
    && !!w.walker && validWalkId(w.walker.id) && typeof w.walker.name === 'string'
    && typeof w.walker.ratingAvg === 'number' && w.walker.ratingAvg >= 0 && w.walker.ratingAvg <= 5
    && Number.isInteger(w.walker.reviewCount) && w.walker.reviewCount >= 0
}
