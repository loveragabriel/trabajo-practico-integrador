import { PageHeader } from '@/shared/components/PageHeader'
import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { agendaParams, readAgendaFilters, type AgendaPeriod } from '../api/walkReadContract'
import { loadWalkAgenda } from '../api/walks'
import { WalkAgendaCard } from '../components/WalkAgendaCard'
import { useWalkResource } from '../hooks/useWalkResource'

export function MyWalksPage() {
  const { session } = useAuth()
  const [params, setParams] = useSearchParams()
  const query = params.toString()
  const request = useCallback((signal: AbortSignal) => loadWalkAgenda(readAgendaFilters(new URLSearchParams(query)), signal), [query])
  const { data, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${query}`, !!session, request)
  const period = params.get('period') ?? 'UPCOMING'

  function changePeriod(next: AgendaPeriod) { setParams(agendaParams({ period: next, page: 0 })) }
  function changePage(page: number) {
    setParams(agendaParams({ period: period === 'HISTORY' ? 'HISTORY' : 'UPCOMING', page }))
  }

  return (
    <section className="space-y-6">
      <PageHeader title="Mis paseos" description="Consultá tus próximas salidas, los lugares ocupados y el historial." />
      <div className="flex flex-wrap gap-2" aria-label="Período de la agenda">
        <button type="button" aria-pressed={period === 'UPCOMING'} onClick={() => changePeriod('UPCOMING')} className={`rounded-lg px-4 py-2 font-medium ${period === 'UPCOMING' ? 'bg-blue-700 text-white' : 'border border-slate-300 bg-white text-slate-700'}`}>Próximos y en curso</button>
        <button type="button" aria-pressed={period === 'HISTORY'} onClick={() => changePeriod('HISTORY')} className={`rounded-lg px-4 py-2 font-medium ${period === 'HISTORY' ? 'bg-blue-700 text-white' : 'border border-slate-300 bg-white text-slate-700'}`}>Historial</button>
      </div>
      <div aria-busy={loading}>
        {loading && <p role="status">Cargando tus paseos…</p>}
        {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4"><p>{error}</p><button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar agenda</button></div>}
        {data && data.items.length === 0 && <div role="status" className="rounded-2xl border border-slate-200 bg-white p-6"><h2 className="font-semibold">{period === 'HISTORY' ? 'Todavía no hay paseos en tu historial' : 'No tenés próximas salidas publicadas'}</h2><p className="mt-2 text-sm text-slate-600">Las salidas que publiques aparecerán acá con su estado y los lugares ocupados.</p></div>}
        {data && <div className="grid gap-4 sm:grid-cols-2">{data.items.map(walk => <WalkAgendaCard key={walk.id} walk={walk} />)}</div>}
      </div>
      {data && (data.page > 0 || data.hasNext) && <nav aria-label="Páginas de la agenda" className="flex items-center justify-between gap-3"><button type="button" disabled={data.page === 0} onClick={() => changePage(data.page - 1)} className="rounded-lg border border-slate-300 px-3 py-2 disabled:opacity-40">Anterior</button><span>Página {data.page + 1}</span><button type="button" disabled={!data.hasNext} onClick={() => changePage(data.page + 1)} className="rounded-lg border border-slate-300 px-3 py-2 disabled:opacity-40">Siguiente</button></nav>}
    </section>
  )
}
