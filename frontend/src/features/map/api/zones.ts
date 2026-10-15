import type { Zone } from '../types'

export class ZoneRequestError extends Error {
  readonly status: number | null

  constructor(message: string, status: number | null = null) {
    super(message)
    this.name = 'ZoneRequestError'
    this.status = status
  }
}

function isZone(value: unknown): value is Zone {
  if (typeof value !== 'object' || value === null) return false
  const zone = value as Record<string, unknown>
  return typeof zone.id === 'string'
    && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(zone.id)
    && typeof zone.name === 'string' && zone.name.trim().length > 0
    && typeof zone.city === 'string' && zone.city.trim().length > 0
}

/** Uses the existing Vite /api proxy; authentication remains owned by the auth module. */
export async function loadZones(token: string, signal?: AbortSignal): Promise<Zone[]> {
  if (!token.trim()) throw new ZoneRequestError('Ingresá para consultar los barrios.', 401)

  const response = await fetch('/api/zones', {
    headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    credentials: 'omit',
    signal,
  })

  if (!response.ok) {
    const message = response.status === 401
      ? 'Tu sesión no es válida. Volvé a ingresar para consultar los barrios.'
      : response.status === 403
        ? 'No tenés permiso para consultar los barrios.'
        : 'No pudimos cargar los barrios. Intentá nuevamente.'
    // Do not surface raw server responses or internal error details in the UI.
    throw new ZoneRequestError(message, response.status)
  }

  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new ZoneRequestError('El catálogo recibido no tiene un formato válido.')
  }
  if (!Array.isArray(body) || !body.every(isZone)
      || new Set(body.map((zone: Zone) => zone.id)).size !== body.length) {
    throw new ZoneRequestError('El catálogo recibido no tiene un formato válido.')
  }

  return body.map(({ id, name, city }: Zone) => ({ id, name, city }))
    .sort((a, b) => a.name.localeCompare(b.name, 'es'))
}
