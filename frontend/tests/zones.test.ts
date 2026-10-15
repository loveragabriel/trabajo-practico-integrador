import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { loadZones, ZoneRequestError } from '../src/features/map/api/zones.ts'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch })
const palermo = { id: '11111111-1111-4111-8111-111111111111', name: 'Palermo', city: 'Buenos Aires' }
const almagro = { id: '22222222-2222-4222-8222-222222222222', name: 'Almagro', city: 'Buenos Aires' }

test('requests the real endpoint with the supplied JWT and returns a sorted minimal DTO', async () => {
  let calls = 0
  globalThis.fetch = async (url, options) => {
    calls++
    assert.equal(url, '/api/zones')
    assert.deepEqual(options?.headers, { Accept: 'application/json', Authorization: 'Bearer token' })
    assert.equal(options?.credentials, 'omit')
    return Response.json([{ ...palermo, createdAt: 'internal' }, almagro])
  }
  assert.deepEqual(await loadZones('token'), [almagro, palermo])
  assert.equal(calls, 1)
})

test('never requests the endpoint without a token', async () => {
  globalThis.fetch = async () => { throw new Error('Must not request') }
  await assert.rejects(loadZones('  '), (error: unknown) => error instanceof ZoneRequestError && error.status === 401)
})

test('handles an empty catalog without creating fake neighborhoods', async () => {
  globalThis.fetch = async () => Response.json([])
  assert.deepEqual(await loadZones('token'), [])
})

for (const status of [401, 403, 500]) {
  test(`handles HTTP ${status} without displaying internal server details`, async () => {
    globalThis.fetch = async () => new Response('password_hash=secret stack trace', { status })
    await assert.rejects(loadZones('token'), (error: unknown) => {
      assert.ok(error instanceof ZoneRequestError)
      assert.equal(error.status, status)
      assert.ok(!error.message.includes('password_hash'))
      return true
    })
  })
}

for (const body of [{ zones: [palermo] }, [palermo, palermo], [{ ...palermo, id: 'Palermo' }], [{ ...palermo, name: '' }]]) {
  test(`rejects invalid catalog shape: ${JSON.stringify(body)}`, async () => {
    globalThis.fetch = async () => Response.json(body)
    await assert.rejects(loadZones('token'), ZoneRequestError)
  })
}

test('handles invalid JSON', async () => {
  globalThis.fetch = async () => new Response('<html>proxy error</html>')
  await assert.rejects(loadZones('token'), ZoneRequestError)
})

test('propagates abort to cancel obsolete requests', async () => {
  const controller = new AbortController()
  controller.abort()
  globalThis.fetch = async (_url, options) => {
    assert.equal(options?.signal, controller.signal)
    options?.signal?.throwIfAborted()
    return Response.json([])
  }
  await assert.rejects(loadZones('token', controller.signal), (error: unknown) => error instanceof DOMException && error.name === 'AbortError')
})
