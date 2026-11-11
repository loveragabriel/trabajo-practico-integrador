import { useCallback, useState } from 'react'
import { generatePath, Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { formatWalkDate, walkTimeOnly } from '@/features/walks/api/search'
import { useWalkResource } from '@/features/walks/hooks/useWalkResource'
import { Icon } from '@/shared/components/Icon'
import { bookingError, cancelBooking, loadMyBookings, type ClientBookingPeriod, type ClientBooking } from '../api/bookings'

const tabs: { value: ClientBookingPeriod; label: string; empty: string }[] = [
  { value: 'UPCOMING', label: 'Próximas', empty: 'No tenés reservas próximas.' },
  { value: 'IN_PROGRESS', label: 'En curso', empty: 'No tenés paseos en curso.' },
  { value: 'HISTORY', label: 'Historial', empty: 'Todavía no tenés reservas en tu historial.' },
]

function bookingPresentation(booking: ClientBooking, period: ClientBookingPeriod) {
  if (booking.bookingStatus === 'CANCELLED' || booking.walkStatus === 'CANCELLED') {
    return { label: 'Cancelada', color: 'bg-error-container text-on-error-container', cancelled: true }
  }
  if (booking.walkStatus === 'IN_PROGRESS') {
    return { label: 'En curso', color: 'bg-primary-container text-on-primary', cancelled: false }
  }
  if (booking.walkStatus === 'COMPLETED') {
    return { label: 'Finalizado', color: 'bg-surface-container-high text-on-surface-variant', cancelled: false }
  }
  if (period === 'HISTORY') {
    return { label: 'Horario cumplido', color: 'bg-surface-container-high text-on-surface-variant', cancelled: false }
  }
  return { label: 'Confirmada', color: 'bg-primary-fixed text-on-primary-fixed-variant', cancelled: false }
}

function BookingCard({ booking, period, onCancelled }: { booking: ClientBooking; period: ClientBookingPeriod; onCancelled: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const presentation = bookingPresentation(booking, period)
  const [now] = useState(Date.now)
  const completed = booking.walkStatus === 'COMPLETED'
  const remainingMs = Date.parse(`${booking.startTime}-03:00`) - now
  const canCancel = period === 'UPCOMING' && !presentation.cancelled && remainingMs > 2 * 60 * 60 * 1000
  const minutes = Math.ceil(remainingMs / 60000)

  async function handleCancel() {
    if (cancelling || !window.confirm(`¿Cancelar la reserva de ${booking.petName} para este paseo?`)) return
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
    <li className="space-y-3 rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className={`rounded-full px-3 py-1 text-label-sm font-semibold ${presentation.color}`}>{presentation.label}</span>
        {period === 'UPCOMING' && minutes > 0 && minutes <= 24 * 60 && (
          <span className="rounded-full bg-secondary-fixed px-2.5 py-1 text-label-sm font-semibold text-on-secondary-fixed-variant">{minutes < 60 ? `Faltan ${minutes} min` : `Faltan ${Math.ceil(minutes / 60)} h`}</span>
        )}
      </div>
      <div className="flex items-center gap-3">
        <span className="grid size-11 shrink-0 place-items-center rounded-full bg-primary-fixed/60 text-primary"><Icon name="person" className="text-xl" /></span>
        <div className="min-w-0">
          <h2 className="break-words text-title-sm font-semibold text-on-surface">{booking.walker.name}</h2>
          <p className="break-words text-caption text-on-surface-variant">{booking.zoneName}{booking.walker.reviewCount > 0 ? ` · ★ ${booking.walker.ratingAvg.toLocaleString('es-AR')}` : ''}</p>
        </div>
      </div>
      <div className="space-y-2 rounded-xl bg-surface-container-low p-3 text-body-sm text-on-surface-variant">
        <p className="flex gap-2"><Icon name="calendar_today" className="shrink-0 text-lg text-primary" /> <span>{formatWalkDate(booking.startTime)} a {walkTimeOnly(booking.endTime)}</span></p>
        <p className="flex gap-2"><Icon name="pets" className="shrink-0 text-lg text-primary" /> <span className="min-w-0 break-words">{booking.petName}</span></p>
        <p className="flex gap-2"><Icon name="location_on" className="shrink-0 text-lg text-primary" /> <span className="min-w-0 break-words">{booking.meetingPoint}</span></p>
      </div>
      {!presentation.cancelled && (
        <p className="text-label-md font-semibold text-primary">{booking.pricePerPet.toLocaleString('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 0 })} por mascota</p>
      )}
      {error && <p role="alert" className="rounded-xl bg-error-container p-3 text-body-sm text-on-error-container">{error}</p>}
      {!presentation.cancelled && (
        <Link to={{ pathname: generatePath(paths.walkDetail, { walkId: booking.walkId }), hash: completed ? '#resenas' : '' }}
          state={{ fromBookings: true, bookingsSearch: `?period=${period}` }}
          className="flex min-h-12 items-center justify-center gap-2 rounded-full bg-primary-container px-4 text-label-md font-semibold text-on-primary">
          {completed ? 'Calificar el paseo' : 'Ver detalle'} <Icon name="arrow_forward" className="text-lg" />
        </Link>
      )}
      {canCancel && (
        <button type="button" onClick={handleCancel} disabled={cancelling} className="block min-h-10 w-full text-center text-label-md font-semibold text-error disabled:opacity-50">
          {cancelling ? 'Cancelando…' : 'Cancelar reserva'}
        </button>
      )}
    </li>
  )
}

export function MyBookingsPage() {
  const { session } = useAuth()
  const [params, setParams] = useSearchParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const confirmedPet = (state as { confirmedPet?: string } | null)?.confirmedPet
  const tab = tabs.find((item) => item.value === params.get('period')) ?? tabs[0]
  const pageParam = params.get('page') ?? '0'
  const page = /^\d+$/.test(pageParam) && Number(pageParam) <= 2147483647 ? Number(pageParam) : 0
  const request = useCallback((signal: AbortSignal) => loadMyBookings(tab.value, page, signal), [tab.value, page])
  const { data, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${tab.value}:${page}`, !!session, request)
  const [cancelled, setCancelled] = useState(false)

  function choosePeriod(period: ClientBookingPeriod, nextPage = 0) {
    setCancelled(false)
    setParams(nextPage ? { period, page: String(nextPage) } : { period })
  }

  return (
    <section className="mx-auto max-w-2xl space-y-4">
      <header>
        <h1 className="text-headline-md font-bold text-on-surface">Mis reservas</h1>
        <p className="mt-1 text-body-md text-on-surface-variant">Consultá las salidas de tus mascotas.</p>
      </header>
      {confirmedPet && !cancelled && (
        <div role="status" className="flex items-center gap-2 rounded-2xl bg-primary-fixed p-3 text-body-sm font-semibold text-on-primary-fixed-variant">
          <Icon name="check_circle" filled className="shrink-0 text-xl" /> <p className="min-w-0 flex-1 break-words">Reserva confirmada para {confirmedPet}.</p>
          <button type="button" onClick={() => navigate({ search: params.toString() }, { replace: true, state: null })} aria-label="Cerrar confirmación" className="grid size-10 shrink-0 place-items-center rounded-full"><Icon name="close" className="text-xl" /></button>
        </div>
      )}
      {cancelled && <p role="status" className="rounded-2xl bg-surface-container-low p-3 text-body-sm text-on-surface-variant">Cancelaste la reserva. Podés verla en Historial.</p>}
      <div role="group" aria-label="Estado de las reservas" className="grid grid-cols-3 gap-1 rounded-full bg-surface-container-low p-1">
        {tabs.map((item) => (
          <button key={item.value} type="button" aria-pressed={tab.value === item.value} onClick={() => choosePeriod(item.value)}
            className={`min-h-11 rounded-full px-2 text-label-md font-semibold ${tab.value === item.value ? 'bg-primary-container text-on-primary shadow-sm' : 'text-on-surface-variant'}`}>
            {item.label}
          </button>
        ))}
      </div>
      {loading && <p role="status" className="rounded-2xl bg-surface-container-lowest p-4 text-body-sm text-on-surface-variant">Cargando tus reservas…</p>}
      {error && (
        <div role="alert" className="rounded-2xl bg-error-container p-4 text-on-error-container">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {data && (data.items.length
        ? <ul className="space-y-3">{data.items.map((booking) => <BookingCard key={booking.bookingId} booking={booking} period={tab.value} onCancelled={() => { setCancelled(true); reload() }} />)}</ul>
        : <div className="rounded-2xl bg-surface-container-lowest p-6 text-center shadow-sm">
            <Icon name="calendar_today" className="text-3xl text-primary" />
            <p className="mt-2 text-title-sm font-semibold">{tab.empty}</p>
            {tab.value === 'UPCOMING' && <Link to={paths.walks} className="mt-3 inline-flex min-h-11 items-center rounded-full bg-primary-container px-4 text-label-md font-semibold text-on-primary">Buscar paseos</Link>}
          </div>)}
      {data && (page > 0 || data.hasNext) && (
        <nav aria-label="Páginas de reservas" className="flex items-center justify-between text-label-md">
          <button type="button" disabled={!page} onClick={() => choosePeriod(tab.value, page - 1)} className="rounded-full bg-surface-container-low px-4 py-2 text-primary disabled:opacity-40">Anterior</button>
          <span>Página {page + 1}</span>
          <button type="button" disabled={!data.hasNext} onClick={() => choosePeriod(tab.value, page + 1)} className="rounded-full bg-surface-container-low px-4 py-2 text-primary disabled:opacity-40">Siguiente</button>
        </nav>
      )}
    </section>
  )
}
