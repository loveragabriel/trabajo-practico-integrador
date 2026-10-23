export interface WalkSummary {
  id: string
  zoneId: string
  zoneName: string
  startTime: string
  durationMinutes: number
  meetingPoint: string
  pricePerPet: number
  capacity: number
  availableSpots: number
  walker: { id: string; name: string; ratingAvg: number; reviewCount: number }
}

export interface WalkSearchResult {
  items: WalkSummary[]
  page: number
  size: number
  hasNext: boolean
}

export interface WalkFilters {
  zoneId: string
  startsFrom: string
  startsTo: string
  page: number
}

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const localDate = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?$/

function validDate(value: string) {
  if (!localDate.test(value)) return false
  const date = new Date(`${value}Z`)
  const precision = value.length === 16 ? 16 : 19
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, precision) === value.slice(0, precision)
}

export function readWalkFilters(params: URLSearchParams): WalkFilters {
  const page = params.get('page') ?? '0'
  if (!/^\d+$/.test(page) || !Number.isSafeInteger(Number(page)) || Number(page) > 2147483647) {
    throw new Error('La página indicada no es válida.')
  }
  const filters = {
    zoneId: params.get('zoneId') ?? '', startsFrom: params.get('startsFrom') ?? '',
    startsTo: params.get('startsTo') ?? '', page: Number(page),
  }
  if (filters.zoneId && !uuid.test(filters.zoneId)) throw new Error('El barrio del enlace no es válido. Elegí otro barrio.')
  if ([filters.startsFrom, filters.startsTo].some((date) => date && !validDate(date))) {
    throw new Error('Revisá las fechas del enlace o limpiá los filtros.')
  }
  if (filters.startsFrom && filters.startsTo && new Date(`${filters.startsFrom}Z`) >= new Date(`${filters.startsTo}Z`)) {
    throw new Error('Hasta debe ser posterior a Desde.')
  }
  return filters
}

export function walkSearchParams(filters: WalkFilters) {
  const params = new URLSearchParams()
  if (filters.zoneId) params.set('zoneId', filters.zoneId)
  if (filters.startsFrom) params.set('startsFrom', filters.startsFrom)
  if (filters.startsTo) params.set('startsTo', filters.startsTo)
  if (filters.page) params.set('page', String(filters.page))
  return params
}

function isWalk(value: unknown): value is WalkSummary {
  if (typeof value !== 'object' || value === null) return false
  const w = value as WalkSummary
  return uuid.test(w.id) && uuid.test(w.zoneId) && typeof w.zoneName === 'string'
    && typeof w.startTime === 'string' && validDate(w.startTime)
    && Number.isInteger(w.durationMinutes) && w.durationMinutes > 0
    && typeof w.meetingPoint === 'string' && typeof w.pricePerPet === 'number'
    && Number.isFinite(w.pricePerPet) && w.pricePerPet >= 0
    && Number.isInteger(w.capacity) && w.capacity > 0
    && Number.isInteger(w.availableSpots) && w.availableSpots > 0 && w.availableSpots <= w.capacity
    && !!w.walker && uuid.test(w.walker.id) && typeof w.walker.name === 'string'
    && typeof w.walker.ratingAvg === 'number' && w.walker.ratingAvg >= 0 && w.walker.ratingAvg <= 5
    && Number.isInteger(w.walker.reviewCount) && w.walker.reviewCount >= 0
}

export async function loadWalks(filters: WalkFilters, signal?: AbortSignal): Promise<WalkSearchResult> {
  const params = walkSearchParams(filters)
  params.set('size', '12')
  let response: Response
  try {
    response = await fetch(`/api/walks?${params}`, {
      headers: { Accept: 'application/json' }, credentials: 'omit', signal,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('No pudimos conectar con el servidor. Intentá nuevamente.')
  }
  if (!response.ok) throw new Error(response.status === 400
    ? 'Los filtros no son válidos. Revisá el barrio y las fechas.'
    : 'No pudimos cargar los paseos. Intentá nuevamente.')
  let data: unknown
  try { data = await response.json() } catch { throw new Error('La respuesta de paseos no tiene un formato válido.') }
  const body = data as WalkSearchResult | null
  if (!body || !Array.isArray(body.items) || !body.items.every(isWalk)
    || body.page !== filters.page || body.size !== 12 || body.items.length > 12
    || typeof body.hasNext !== 'boolean'
    || new Set(body.items.map((walk) => walk.id)).size !== body.items.length) {
    throw new Error('La respuesta de paseos no tiene un formato válido.')
  }
  return body
}

export function formatWalkDate(value: string) {
  // La base guarda la hora local de CABA; la mostramos igual desde cualquier dispositivo.
  return new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires', dateStyle: 'medium', timeStyle: 'short',
  }).format(new Date(`${value}-03:00`))
}
