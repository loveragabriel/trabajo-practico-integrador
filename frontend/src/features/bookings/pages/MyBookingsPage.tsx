import { useCallback, useState } from 'react'
import { useAuth } from '@/features/auth'
import { formatWalkDate } from '@/features/walks/api/search'
import { useWalkResource } from '@/features/walks/hooks/useWalkResource'
import { PageHeader } from '@/shared/components/PageHeader'
import { bookingError, cancelBooking, loadMyBookings, type AgendaPeriod, type ClientBooking } from '../api/bookings'

const tabs: { value: AgendaPeriod; label: string }[] = [
  { value: 'UPCOMING', label: 'Próximas' },
  { value: 'HISTORY', label: 'Historial' },
]

// RN-27: el cliente puede cancelar hasta 2hs antes de scheduled_at; esto solo evita
// un viaje de ida y vuelta innecesario, el backend valida lo mismo de todas formas
function canCancel(booking: ClientBooking) {
  return booking.bookingStatus === 'JOINED' && new Date(`${booking.startTime}-03:00`).getTime() > Date.now() + 2 * 60 * 60 * 1000
}

function BookingCard({ booking, onCancelled }: { booking: ClientBooking; onCancelled: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState(false)

  async function handleCancel() {
    if (cancelling || !window.confirm('¿Cancelar la reserva de ' + booking.petName + ' para este paseo?')) return
    setCancelling(true)
    setError(null)
    try {
      await cancelBooking(booking.walkId, booking.petId)
      onCancelled()
    } catch (failure) {
      setError(bookingError(failure))
    } finally {
      setCancelling(false)
    }
  }

  return (
    <li className="rounded-xl border border-slate-200 bg-white p-4">
      <p className="text-sm font-semibold text-blue-800">{booking.zoneName} · con {booking.walker.name}</p>
      <p className="mt-1 font-semibold">{formatWalkDate(booking.startTime)}</p>
      <p className="mt-1 text-sm text-slate-600">Mascota: {booking.petName}</p>
      <p className="mt-1 text-sm text-slate-600">Punto de encuentro: {booking.meetingPoint}</p>
      <p className="mt-1 text-sm text-slate-600">
        {booking.bookingStatus === 'CANCELLED' ? 'Reserva cancelada' : `${booking.pricePerPet.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })} por mascota`}
      </p>
      {error && <p role="alert" className="mt-2 rounded-lg bg-red-50 p-2 text-sm text-red-800">{error}</p>}
      {canCancel(booking) && (
        <button type="button" onClick={handleCancel} disabled={cancelling}
          className="mt-3 rounded-lg border border-red-300 px-3 py-1.5 text-sm font-semibold text-red-700 disabled:opacity-50">
          {cancelling ? 'Cancelando…' : 'Cancelar reserva'}
        </button>
      )}
    </li>
  )
}

export function MyBookingsPage() {
  const { session } = useAuth()
  const [period, setPeriod] = useState<AgendaPeriod>('UPCOMING')
  const request = useCallback((signal: AbortSignal) => loadMyBookings(period, 0, signal), [period])
  const { data, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${period}`, !!session, request)

  return (
    <section className="space-y-4">
      <PageHeader title="Mis reservas" description="Tus próximos paseos y el historial." />
      <div className="flex gap-2">
        {tabs.map((tab) => (
          <button key={tab.value} type="button" onClick={() => setPeriod(tab.value)}
            className={`rounded-lg px-4 py-2 text-sm font-semibold ${period === tab.value ? 'bg-blue-700 text-white' : 'bg-white text-slate-700 border border-slate-200'}`}>
            {tab.label}
          </button>
        ))}
      </div>
      {loading && <p role="status">Cargando tus reservas…</p>}
      {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5">
        <p>{error}</p>
        <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
      </div>}
      {data && (data.items.length
        ? <ul className="space-y-3">
            {data.items.map((booking) => <BookingCard key={booking.bookingId} booking={booking} onCancelled={reload} />)}
          </ul>
        : <p className="rounded-xl border border-slate-200 bg-white p-4 text-sm text-slate-600">
            {period === 'UPCOMING' ? 'No tenés reservas próximas.' : 'Todavía no tenés reservas en tu historial.'}
          </p>)}
    </section>
  )
}
