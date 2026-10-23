import { useState, type SubmitEvent } from 'react'
import { ZoneSelect, type Zone } from '@/features/map'
import { readWalkFilters, walkSearchParams } from '../api/search'

interface Props {
  params: URLSearchParams
  zones: Zone[]
  loadingZones: boolean
  onSearch: (params: URLSearchParams) => void
}

export function WalkFiltersForm({ params, zones, loadingZones, onSearch }: Props) {
  const [zoneId, setZoneId] = useState(params.get('zoneId') ?? '')
  const [startsFrom, setStartsFrom] = useState(params.get('startsFrom') ?? '')
  const [startsTo, setStartsTo] = useState(params.get('startsTo') ?? '')
  const [error, setError] = useState('')

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = walkSearchParams({ zoneId, startsFrom, startsTo, page: 0 })
    try { readWalkFilters(next); setError(''); onSearch(next) }
    catch (e) { setError(e instanceof Error ? e.message : 'Revisá los filtros.') }
  }

  function clear() {
    setZoneId(''); setStartsFrom(''); setStartsTo(''); setError('')
    onSearch(new URLSearchParams())
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" aria-label="Filtrar paseos">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ZoneSelect zones={zones} value={zoneId} onChange={setZoneId} disabled={loadingZones} label="Barrio (opcional)" />
        <label className="space-y-2 text-sm font-medium text-slate-800">
          <span className="block">Desde</span>
          <input name="startsFrom" type="datetime-local" value={startsFrom} onChange={(e) => setStartsFrom(e.target.value)} className="w-full rounded-md border border-slate-300 px-3 py-2 focus:outline-blue-600" />
        </label>
        <label className="space-y-2 text-sm font-medium text-slate-800">
          <span className="block">Hasta (sin incluir)</span>
          <input name="startsTo" type="datetime-local" value={startsTo} onChange={(e) => setStartsTo(e.target.value)} className="w-full rounded-md border border-slate-300 px-3 py-2 focus:outline-blue-600" />
        </label>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <button className="rounded-lg bg-blue-700 px-5 py-2.5 font-semibold text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700">Buscar paseos</button>
        <button type="button" onClick={clear} className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">Limpiar filtros</button>
        <p className="text-xs text-slate-500">Todos los horarios corresponden a CABA.</p>
      </div>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    </form>
  )
}
