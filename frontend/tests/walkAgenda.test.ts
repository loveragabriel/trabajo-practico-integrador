import assert from 'node:assert/strict'
import { test } from 'node:test'
import { agendaParams, isWalkDetails, readAgendaFilters, validWalkId, walkStatusLabels } from '../src/features/walks/api/walkReadContract.ts'

const id = '11111111-1111-4111-8111-111111111111'
const walk = { id, zoneId: id, zoneName: 'Palermo', startTime: '2030-01-01T11:00:00', endTime: '2030-01-01T12:00:00',
  durationMinutes: 60, meetingPoint: 'Plaza', pricePerPet: 2500, capacity: 3, bookedSpots: 3, availableSpots: 0,
  status: 'FULL', walker: { id, name: 'Paseador', ratingAvg: 0, reviewCount: 0 } }

test('la agenda empieza en próximos y página cero', () => {
  assert.deepEqual(readAgendaFilters(new URLSearchParams()), { period: 'UPCOMING', page: 0 })
})
test('conserva el período y la página en el enlace', () => {
  assert.deepEqual(readAgendaFilters(agendaParams({ period: 'HISTORY', page: 2 })), { period: 'HISTORY', page: 2 })
})
for (const query of ['period=ALL', 'page=-1', 'page=1.5', 'page=2147483648']) {
  test(`rechaza un filtro de agenda inválido: ${query}`, () => assert.throws(() => readAgendaFilters(new URLSearchParams(query))))
}
test('un paseo completo sigue teniendo un detalle válido', () => assert.equal(isWalkDetails(walk), true))
test('reconoce los cinco estados de la entidad compartida', () => {
  for (const status of Object.keys(walkStatusLabels)) assert.equal(isWalkDetails({ ...walk, status }), true)
})
for (const extra of [{ status: 'UNKNOWN' }, { bookedSpots: -1 }, { availableSpots: 4 }, { endTime: 'sin fecha' }]) {
  test(`rechaza un detalle inválido: ${JSON.stringify(extra)}`, () => assert.equal(isWalkDetails({ ...walk, ...extra }), false))
}
test('rechaza identificadores que no son UUID', () => {
  assert.equal(validWalkId('mine'), false)
  assert.equal(validWalkId(id), true)
})
