import { useState, type SubmitEvent } from 'react'
import { ZoneCoverageMap, useZones, type Zone } from '@/features/map'
import { errorBox, input, panel, primary, successBox } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { getCoverage, saveCoverage, type Coverage } from '../api/coverage'

export function CoverageEditor() {
  const coverage = useResource(getCoverage)
  const catalog = useZones()
  return <section className={panel} id="cobertura">
    <h2 className="text-xl font-bold">Zonas de servicio</h2>
    <p className="mb-4 mt-1 text-sm text-slate-600">Elegí los barrios donde vas a publicar paseos. El precio y el cupo se pueden ajustar al publicar.</p>
    {(coverage.loading || catalog.loading) && <p role="status">Cargando cobertura…</p>}
    {(coverage.error || catalog.error) && <div role="alert" className={errorBox}>{coverage.error || catalog.error} <button type="button" className="underline" onClick={() => { coverage.reload(); catalog.reload() }}>Reintentar</button></div>}
    {coverage.data && !catalog.loading && !catalog.error && <CoverageForm coverage={coverage.data} zones={catalog.zones} />}
  </section>
}

function CoverageForm({ coverage, zones }: { coverage: Coverage; zones: Zone[] }) {
  const [selected, setSelected] = useState(() => coverage.zones.map(zone => zone.id))
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    const form = new FormData(event.currentTarget)
    const number = (name: string) => form.get(name) === '' ? null : Number(form.get(name))
    setSaving(true)
    setMessage('')
    setError('')
    try {
      await saveCoverage({ defaultPrice: number('price'), defaultCapacity: number('capacity'), zoneIds: selected })
      setMessage('Cobertura guardada.')
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos guardar la cobertura.')
    } finally { setSaving(false) }
  }

  return <form onSubmit={submit}>
    <fieldset disabled={saving} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="text-sm font-medium">Precio por defecto (ARS)<input name="price" type="number" min={0} max={99999999.99} step={0.01} defaultValue={coverage.defaultPrice ?? ''} className={input} /></label>
        <label className="text-sm font-medium">Cupo por defecto<input name="capacity" type="number" min={1} max={2147483647} step={1} defaultValue={coverage.defaultCapacity ?? ''} className={input} /></label>
      </div>
      <fieldset className="space-y-2">
        <legend className="mb-2 text-sm font-semibold">Barrios de cobertura · {selected.length} seleccionados</legend>
        <div className="grid max-h-52 gap-1 overflow-y-auto rounded-xl border border-slate-200 p-2 sm:grid-cols-2">
          {zones.map(zone => <label key={zone.id} className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-emerald-50">
            <input type="checkbox" checked={selected.includes(zone.id)} className="accent-emerald-700"
              onChange={e => { setMessage(''); setSelected(e.target.checked ? [...selected, zone.id] : selected.filter(id => id !== zone.id)) }} />
            {zone.name}
          </label>)}
        </div>
      </fieldset>
      {selected.length > 0 && <ZoneCoverageMap zones={zones.filter(zone => selected.includes(zone.id))} className="h-56" />}
      {error && <p className={errorBox} role="alert">{error}</p>}
      {message && <p className={successBox} role="status">{message}</p>}
      <button type="submit" className={primary}>{saving ? 'Guardando…' : 'Guardar cobertura'}</button>
    </fieldset>
  </form>
}
