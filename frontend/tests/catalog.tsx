// Isolated development fixture. It is not imported by the application's router/build.
// Browser tests intercept /api/zones; these tokens are deliberately not real JWTs.
import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ZoneCatalog } from '@/features/map'
import '../src/index.css'

export function CatalogFixture() {
  const [token, setToken] = useState<string | null>(null)
  const [zone, setZone] = useState('')

  return (
    <main className="mx-auto max-w-lg space-y-4 p-4">
      <h1 className="text-xl font-semibold">Prueba del selector de barrios</h1>
      <div className="flex flex-wrap gap-3">
        <button type="button" onClick={() => setToken('test-a')}>Sesión A</button>
        <button type="button" onClick={() => setToken('test-b')}>Sesión B</button>
        <button type="button" onClick={() => setToken(null)}>Cerrar sesión</button>
      </div>
      <ZoneCatalog token={token} value={zone} onChange={setZone} />
      <output aria-label="ID elegido">{zone}</output>
    </main>
  )
}

createRoot(document.getElementById('root')!).render(<CatalogFixture />)
