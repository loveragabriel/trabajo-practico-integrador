import { useSearchParams } from 'react-router-dom'
import { locateZone, useZones, ZoneCoverageMap } from '@/features/map'
import { WalkCard } from '../components/WalkCard'
import { WalkFiltersForm } from '../components/WalkFiltersForm'
import { useWalkSearch } from '../hooks/useWalkSearch'

export function WalkSearchPage() {
  const [params, setParams] = useSearchParams()
  const query = params.toString()
  const catalog = useZones()
  const { result, loading, error, reload } = useWalkSearch(query)
  const zoneId = params.get('zoneId')
  const selectedZone = catalog.zones.find((zone) => zone.id === zoneId)
  const mapZones = selectedZone ? [selectedZone] : zoneId ? [] : catalog.zones
  const unknownZone = !!zoneId && !catalog.loading && !catalog.error && !selectedZone

  function goToPage(page: number) {
    const next = new URLSearchParams(params)
    if (page) next.set('page', String(page)); else next.delete('page')
    setParams(next)
  }

  return (
    <section className="space-y-6">
      <div className="rounded-3xl border border-blue-100 bg-blue-50 px-6 py-8 sm:px-8">
        <p className="text-sm font-semibold tracking-wide text-blue-700">PASEADORES · CABA</p>
        <h1 className="mt-3 max-w-2xl text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Un paseo cerca, una salida para compartir.</h1>
        <p className="mt-4 max-w-xl text-slate-600">Encontrá paseos grupales por barrio y horario. Compará el punto de encuentro, el precio y los lugares disponibles.</p>
      </div>
      <WalkFiltersForm key={query} params={params} zones={catalog.zones} loadingZones={catalog.loading} onSearch={setParams} />
      {catalog.loading && <p role="status" className="text-sm text-slate-600">Cargando barrios…</p>}
      {catalog.error && <div role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm"><p>{catalog.error}</p><button type="button" onClick={catalog.reload} className="mt-2 font-semibold underline">Reintentar barrios</button></div>}
      {!catalog.loading && !catalog.error && catalog.zones.length === 0 && <p role="status">Todavía no hay barrios cargados.</p>}
      {unknownZone && <p role="alert" className="text-sm text-amber-800">El barrio del enlace no está en el catálogo. Elegí otro o limpiá los filtros.</p>}
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_19rem]">
        <div className="min-w-0 space-y-4" aria-busy={loading}>
          <div><h2 className="text-xl font-bold">Paseos disponibles</h2><p className="mt-1 text-sm text-slate-500">Salidas futuras con al menos un lugar libre.</p></div>
          {loading && <p role="status" className="rounded-2xl border border-slate-200 bg-white p-6 text-slate-600">Buscando paseos…</p>}
          {error && <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-5"><p>{error}</p><button type="button" onClick={reload} className="mt-3 font-semibold underline">Reintentar búsqueda</button></div>}
          {result && result.items.length === 0 && <div role="status" className="rounded-2xl border border-slate-200 bg-white p-7"><h3 className="font-semibold">No encontramos paseos con estos filtros</h3><p className="mt-2 text-sm text-slate-600">Probá con otro barrio u horario. Los paseos completos o que ya comenzaron no aparecen en la búsqueda.</p></div>}
          {result && result.items.length > 0 && <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">{result.items.map((walk) => <WalkCard key={walk.id} walk={walk} />)}</div>}
          {result && (result.page > 0 || result.hasNext) && <nav aria-label="Páginas de resultados" className="flex items-center justify-between gap-3"><button type="button" disabled={result.page === 0} onClick={() => goToPage(result.page - 1)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:opacity-40">Anterior</button><span className="text-sm">Página {result.page + 1}</span><button type="button" disabled={!result.hasNext} onClick={() => goToPage(result.page + 1)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:opacity-40">Siguiente</button></nav>}
        </div>
        <aside className="overflow-hidden rounded-2xl border border-slate-200 bg-white p-4">
          <h2 className="font-bold">{selectedZone ? selectedZone.name : 'Barrios de CABA'}</h2>
          <p className="mb-4 mt-2 text-sm text-slate-600">Ubicación orientativa de los barrios. Consultá el punto de encuentro publicado en cada paseo.</p>
          <ZoneCoverageMap zones={mapZones} />
          {selectedZone && !locateZone(selectedZone) && <p role="status" className="mt-3 text-sm text-amber-800">Este barrio todavía no tiene una referencia en el mapa.</p>}
          <p className="mt-3 text-xs text-slate-500">El mapa no muestra recorridos ni la ubicación de personas o mascotas.</p>
        </aside>
      </div>
    </section>
  )
}
