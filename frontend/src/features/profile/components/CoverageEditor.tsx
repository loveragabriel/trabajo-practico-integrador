import { useId, useState, type SubmitEvent } from 'react'
import { useAuth } from '@/features/auth'
import { useZones, ZoneCoverageMap, type Zone } from '@/features/map'
import { ApiRequestError } from '@/shared/api/client'
import { updateCoverage, type Coverage } from '../api/coverage'
import { useCoverage } from '../hooks/useCoverage'

const inputClass =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100'
const labelClass = 'block text-sm font-medium text-slate-800'

export function CoverageEditor() {
  const { session } = useAuth()
  const { coverage, error, loading, reload } = useCoverage()
  const zones = useZones(session?.token ?? null)

  return (
    <section className="mt-8 space-y-4 border-t border-slate-200 pt-6">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">Zonas de servicio</h2>
        <p className="text-sm text-slate-600">Elegí los barrios donde vas a publicar paseos. El precio y el cupo se pueden ajustar al publicar.</p>
      </div>

      {(loading || zones.loading) && <p role="status" className="text-slate-600">Cargando cobertura…</p>}
      {(error || zones.error) && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <p>{error || zones.error}</p>
          <button type="button" onClick={() => { reload(); zones.reload() }} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {coverage && !zones.loading && !zones.error && <CoverageForm coverage={coverage} zones={zones.zones} />}
    </section>
  )
}

interface CoverageFormProps {
  coverage: Coverage
  zones: Zone[]
}

function CoverageForm({ coverage, zones }: CoverageFormProps) {
  const priceId = useId()
  const capacityId = useId()

  const [selected, setSelected] = useState<string[]>(() => coverage.zones.map((zone) => zone.id))
  const [price, setPrice] = useState(coverage.defaultPrice != null ? String(coverage.defaultPrice) : '')
  const [capacity, setCapacity] = useState(coverage.defaultCapacity != null ? String(coverage.defaultCapacity) : '')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  function toggleZone(zoneId: string, checked: boolean) {
    setMessage('')
    setSelected((current) => (checked ? [...current, zoneId] : current.filter((id) => id !== zoneId)))
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving || selected.length === 0) return
    setSaving(true)
    setMessage('')
    setError('')
    try {
      await updateCoverage({
        defaultPrice: price === '' ? null : Number(price),
        defaultCapacity: capacity === '' ? null : Number(capacity),
        zoneIds: selected,
      })
      setMessage('Cobertura guardada.')
    } catch (failure) {
      setError(
        failure instanceof ApiRequestError
          ? (failure.fieldErrors?.[0]?.message ?? failure.message)
          : 'No pudimos guardar la cobertura.',
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <fieldset disabled={saving} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <label htmlFor={priceId} className={labelClass}>Precio por defecto (ARS)</label>
            <input
              id={priceId}
              type="number"
              min={0}
              max={99999999.99}
              step={0.01}
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              className={inputClass}
            />
          </div>
          <div className="space-y-2">
            <label htmlFor={capacityId} className={labelClass}>Cupo por defecto</label>
            <input
              id={capacityId}
              type="number"
              min={1}
              step={1}
              value={capacity}
              onChange={(event) => setCapacity(event.target.value)}
              className={inputClass}
            />
          </div>
        </div>

        <div className="space-y-2">
          <span className={labelClass}>Barrios de cobertura · {selected.length} seleccionados</span>
          <div className="grid max-h-52 gap-1 overflow-y-auto rounded-md border border-slate-300 p-2 sm:grid-cols-2">
            {zones.map((zone) => (
              <label key={zone.id} className="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-slate-800 hover:bg-emerald-50">
                <input
                  type="checkbox"
                  checked={selected.includes(zone.id)}
                  onChange={(event) => toggleZone(zone.id, event.target.checked)}
                  className="accent-emerald-600"
                />
                {zone.name}
              </label>
            ))}
          </div>
          {selected.length === 0 && (
            <p className="text-sm text-red-600">Elegí al menos un barrio de cobertura.</p>
          )}
        </div>

        {selected.length > 0 && (
          <ZoneCoverageMap zones={zones.filter((zone) => selected.includes(zone.id))} className="h-56" />
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}
        {message && <p className="text-sm text-emerald-700">{message}</p>}

        <button
          type="submit"
          disabled={saving || selected.length === 0}
          className="w-full rounded-md bg-emerald-600 px-3 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
        >
          {saving ? 'Guardando…' : 'Guardar cobertura'}
        </button>
      </fieldset>
    </form>
  )
}
