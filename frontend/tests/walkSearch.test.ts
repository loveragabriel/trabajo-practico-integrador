import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { apiClient, ApiRequestError } from '../src/shared/api/client.ts'
import { formatWalkDate, loadWalks, readWalkFilters, walkBrowseDays, walkSearchParams } from '../src/features/walks/api/search.ts'

const originalGet = apiClient.get
afterEach(() => { apiClient.get = originalGet })
const id = '11111111-1111-4111-8111-111111111111'
const filters = { zoneId: id, startsFrom: '2030-01-01T10:00', startsTo: '2030-01-02T10:00', page: 0 }
const walk = { id, zoneId: id, zoneName: 'Palermo', startTime: '2030-01-01T11:00:00', durationMinutes: 60,
  meetingPoint: 'Entrada de la plaza', pricePerPet: 2500, capacity: 3, availableSpots: 2,
  walker: { id, name: 'Paseador de prueba', ratingAvg: 0, reviewCount: 0 } }

test('conserva barrio y horario al compartir el enlace y reinicia la página', () => {
  assert.deepEqual(readWalkFilters(walkSearchParams(filters)), filters)
  assert.equal(walkSearchParams(filters).has('page'), false)
})

for (const query of ['page=-1', 'page=1.5', 'page=2147483648', 'zoneId=Palermo',
  'startsFrom=2030-02-30T10:00', 'startsFrom=2030-01-01T10:00&startsTo=2030-01-01T10:00:00',
  'startsFrom=2030-01-02T10:00&startsTo=2030-01-01T10:00']) {
  test(`rechaza filtros inválidos: ${query}`, () => {
    assert.throws(() => readWalkFilters(new URLSearchParams(query)))
  })
}

test('consulta la API pública con filtros codificados, límite y cancelación', async () => {
  const controller = new AbortController()
  apiClient.get = (async (url: string, config?: { signal?: AbortSignal }) => {
    const parsed = new URL(String(url), 'http://localhost')
    assert.equal(parsed.pathname, '/walks')
    assert.equal(parsed.searchParams.get('zoneId'), id)
    assert.equal(parsed.searchParams.get('startsFrom'), filters.startsFrom)
    assert.equal(parsed.searchParams.get('size'), '12')
    assert.equal(config?.signal, controller.signal)
    return { data: { items: [walk], page: 0, size: 12, hasNext: false } }
  }) as typeof apiClient.get
  assert.equal((await loadWalks(filters, controller.signal)).items[0]?.availableSpots, 2)
})

test('acepta búsquedas vacías sin fabricar resultados', async () => {
  apiClient.get = (async () => ({ data: { items: [], page: 0, size: 12, hasNext: false } })) as typeof apiClient.get
  assert.deepEqual((await loadWalks(filters)).items, [])
})

for (const items of [[{ ...walk, availableSpots: 0 }], [{ ...walk, availableSpots: 4 }], [walk, walk],
  [{ ...walk, pricePerPet: -1 }], [{ ...walk, startTime: '2030-02-30T10:00' }]]) {
  test('rechaza una respuesta incompatible de paseos', async () => {
    apiClient.get = (async () => ({ data: { items, page: 0, size: 12, hasNext: false } })) as typeof apiClient.get
    await assert.rejects(loadWalks(filters), /formato válido/)
  })
}

test('acepta timestamps con fracciones generados por PostgreSQL', async () => {
  apiClient.get = (async () => ({
    data: { items: [{ ...walk, startTime: '2030-01-01T11:00:00.123456' }], page: 0, size: 12, hasNext: false },
  })) as typeof apiClient.get
  assert.equal((await loadWalks(filters)).items.length, 1)
})

for (const status of [400, 500]) {
  test(`no muestra detalles internos del error HTTP ${status}`, async () => {
    // simula que el server filtrara algo sensible en el mensaje: loadWalks nunca debe repetirlo
    apiClient.get = (async () => { throw new ApiRequestError('password_hash=secreto', status) }) as typeof apiClient.get
    await assert.rejects(loadWalks(filters), (error: unknown) => error instanceof Error && !error.message.includes('password_hash'))
  })
}

test('muestra la hora publicada en CABA y no la convierte al huso del dispositivo', () => {
  assert.match(formatWalkDate('2030-01-01T11:00:00'), /11:00/)
})

test('explica un problema de conexión sin mostrar el error técnico', async () => {
  apiClient.get = (async () => { throw new TypeError('detalle técnico de red') }) as typeof apiClient.get
  await assert.rejects(loadWalks(filters), /No pudimos conectar con el servidor/)
})

test('conserva la cancelación para descartar una búsqueda anterior', async () => {
  const controller = new AbortController()
  controller.abort()
  apiClient.get = (async (_url: string, config?: { signal?: AbortSignal }) => {
    if (config?.signal?.aborted) throw new DOMException('aborted', 'AbortError')
    return { data: { items: [], page: 0, size: 12, hasNext: false } }
  }) as typeof apiClient.get
  await assert.rejects(loadWalks(filters, controller.signal), (error: unknown) => error instanceof DOMException && error.name === 'AbortError')
})

test('los días rápidos cruzan fin de mes en el horario de Buenos Aires', () => {
  const [today, tomorrow] = walkBrowseDays(new Date('2030-02-01T01:20:00Z'))
  assert.deepEqual(today, { from: '2030-01-31T00:00', to: '2030-02-01T00:00', label: 'Hoy', date: 31 })
  assert.equal(tomorrow.from, today.to)
  assert.equal(tomorrow.to, '2030-02-02T00:00')
})
