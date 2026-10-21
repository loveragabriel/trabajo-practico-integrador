// Fixture de desarrollo aislado: no lo importa el router ni el build de la aplicación.
// Abrir /tests/coverage-map.html con `pnpm dev` para revisar el mapa a ojo.
import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ZoneCoverageMap, type Zone } from '@/features/map'
import { CABA_ZONE_NAMES } from './cabaZones'
import '../src/index.css'

const allZones: Zone[] = CABA_ZONE_NAMES.map((name, index) => ({
  id: `00000000-0000-4000-8000-${String(index).padStart(12, '0')}`, name, city: 'Buenos Aires',
}))
const unknownZone: Zone = { id: '99999999-9999-4999-8999-999999999999', name: 'Avellaneda', city: 'Buenos Aires' }

const scenarios: Record<string, Zone[]> = {
  'Los 48 barrios': allZones,
  'Un barrio (Palermo)': allZones.filter((zone) => zone.name === 'Palermo'),
  'Cobertura de un paseador': allZones.filter((zone) => ['Palermo', 'Villa Crespo', 'Colegiales'].includes(zone.name)),
  'Barrio desconocido': [unknownZone],
  'Sin zonas': [],
}

export function CoverageMapFixture() {
  const [scenario, setScenario] = useState('Los 48 barrios')

  return (
    <main className="mx-auto max-w-3xl space-y-4 p-4">
      <h1 className="text-xl font-semibold">Prueba del mapa de cobertura</h1>
      <div className="flex flex-wrap gap-3">
        {Object.keys(scenarios).map((name) => (
          <button key={name} type="button" onClick={() => setScenario(name)} aria-pressed={scenario === name}>{name}</button>
        ))}
      </div>
      <ZoneCoverageMap zones={scenarios[scenario]} className="h-[32rem]" />
    </main>
  )
}

createRoot(document.getElementById('root')!).render(<CoverageMapFixture />)
