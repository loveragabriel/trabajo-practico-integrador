import { useCallback, useState } from 'react'
import { generatePath, Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { formatWalkDate } from '@/features/walks/api/search'
import { walkStatusLabels } from '@/features/walks/api/walkReadContract'
import { PageHeader } from '@/shared/components/PageHeader'
import { errorBox, panel, primary, secondary, successBox } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { cancelBooking, listBookings, type Booking } from '../api/bookings'

export function MyBookingsPage() {
  const [now] = useState(Date.now)
  const [page, setPage] = useState(0)
  const [pending, setPending] = useState<string>()
  const [confirm, setConfirm] = useState<string>()
  const [notice, setNotice] = useState<{ text: string; error?: boolean }>()
  const request = useCallback((signal: AbortSignal) => listBookings(page, signal), [page])
  const { data, loading, error, reload } = useResource(request)

  async function cancel(booking: Booking) {
    if (pending) return
    setPending(booking.id)
    setNotice(undefined)
    try {
      await cancelBooking(booking)
      setConfirm(undefined)
      setNotice({ text: `Cancelaste la reserva de ${booking.petName}.` })
      reload()
    } catch (failure) {
      setNotice({ error: true, text: failure instanceof Error ? failure.message : 'No pudimos cancelar la reserva.' })
    } finally { setPending(undefined) }
  }

  const upcoming = data?.filter(b => b.bookingStatus === 'JOINED' && !['COMPLETED', 'CANCELLED'].includes(b.walkStatus)
    && Date.parse(`${b.endTime}-03:00`) > now) ?? []
  const history = data?.filter(b => !upcoming.includes(b)) ?? []

  function rows(bookings: Booking[], label: string) {
    if (!bookings.length) return null
    return <section className="space-y-3">
      <h2 className="text-xl font-bold">{label}</h2>
      {bookings.map(booking => <article key={booking.id} className={`flex flex-wrap items-start justify-between gap-4 ${panel}`}>
        <div>
          <h3 className="font-bold">{booking.petName} · {booking.zoneName}</h3>
          <p className="mt-1 text-sm text-slate-600">{formatWalkDate(booking.startTime)} · Con {booking.walkerName}</p>
          <p className="mt-2 text-sm font-semibold">{booking.bookingStatus === 'CANCELLED' ? 'Reserva cancelada' : walkStatusLabels[booking.walkStatus]}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {booking.bookingStatus === 'JOINED' && <Link className={secondary} to={generatePath(paths.walkDetail, { walkId: booking.walkId })}>
            {booking.walkStatus === 'COMPLETED' ? 'Ver paseo y reseña' : 'Ver paseo'}
          </Link>}
          {booking.canCancel && <button type="button" className={secondary} onClick={() => setConfirm(booking.id)} disabled={!!pending}>Cancelar reserva</button>}
        </div>
        {confirm === booking.id && <div className="w-full rounded-lg bg-amber-50 p-3">
          <p className="mb-2 text-sm">¿Querés liberar el lugar de {booking.petName}?</p>
          <div className="flex flex-wrap gap-2">
            <button type="button" className={primary} onClick={() => void cancel(booking)} disabled={!!pending}>Confirmar cancelación</button>
            <button type="button" className={secondary} onClick={() => setConfirm(undefined)} disabled={!!pending}>Conservar reserva</button>
          </div>
        </div>}
      </article>)}
    </section>
  }

  return <section className="space-y-5" aria-busy={loading}>
    <PageHeader title="Mis reservas" description="Tus próximas salidas y el historial de cada mascota." />
    {notice && <p role={notice.error ? 'alert' : 'status'} className={notice.error ? errorBox : successBox}>{notice.text}</p>}
    {loading && <p role="status">Cargando reservas…</p>}
    {error && <div role="alert" className={errorBox}>{error} <button type="button" className="underline" onClick={reload}>Reintentar</button></div>}
    {data?.length === 0 && <div className={panel}><p>{page === 0 ? 'Todavía no tenés reservas.' : 'No quedan más reservas.'}</p>
      {page === 0 && <Link className={`mt-3 ${primary}`} to={paths.walks}>Buscar paseos</Link>}</div>}
    {rows(upcoming, 'Próximas salidas')}
    {rows(history, 'Historial')}
    {data && (page > 0 || data.length === 25) && <nav aria-label="Páginas de reservas" className="flex gap-2">
      <button type="button" className={secondary} disabled={!page || !!pending} onClick={() => setPage(page - 1)}>Anterior</button>
      <button type="button" className={secondary} disabled={data.length < 25 || !!pending} onClick={() => setPage(page + 1)}>Siguiente</button>
    </nav>}
  </section>
}
