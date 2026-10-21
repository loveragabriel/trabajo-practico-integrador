import assert from 'node:assert/strict'
import { test } from 'node:test'
import { locateZone, normalizeZoneName } from '../src/features/map/utils/zoneLocations.ts'
import { CABA_ZONE_NAMES } from './cabaZones.ts'

test('locates every neighborhood returned by /api/zones', () => {
  assert.equal(CABA_ZONE_NAMES.length, 48)
  const missing = CABA_ZONE_NAMES.filter((name) => locateZone({ name }) === null)
  assert.deepEqual(missing, [])
})

test('every location falls inside CABA', () => {
  for (const name of CABA_ZONE_NAMES) {
    const position = locateZone({ name })!
    assert.ok(position.lat > -34.71 && position.lat < -34.52, `${name} lat ${position.lat}`)
    assert.ok(position.lng > -58.54 && position.lng < -58.33, `${name} lng ${position.lng}`)
  }
})

test('normalizes accents, case and spacing', () => {
  assert.equal(normalizeZoneName('  Villa   Ortúzar '), 'villa ortuzar')
  assert.equal(normalizeZoneName('NÚÑEZ'), 'nunez')
  assert.deepEqual(locateZone({ name: 'velez sarsfield' }), locateZone({ name: 'Vélez Sarsfield' }))
})

test('returns null for an unknown neighborhood instead of inventing a location', () => {
  assert.equal(locateZone({ name: 'Avellaneda' }), null)
})
