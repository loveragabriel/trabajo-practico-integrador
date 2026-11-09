import { useCallback, useState, type SubmitEvent } from 'react'
import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { listPets } from '@/features/pets/api/pets'
import { WalkReviewForm } from '@/features/reviews/components/WalkReviewForm'
import type { WalkDetails } from '@/features/walks/api/walkReadContract'
import { errorBox, panel, primary, secondary } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { cancelBooking, listWalkBookings, reserveWalk, type Booking } from '../api/bookings'

export function WalkBookingPanel({ walk, userId, onChanged }: {
  walk: WalkDetails; userId: string; onChanged: (message: string) => void
}) {
  const [now] = useState(Date.now)
  const isOpen = walk.status === 'PUBLISHED' && Date.parse(`${walk.startTime}-03:00`) > now && walk.availableSpots > 0
  const request = useCallback((signal: AbortSignal) => listWalkBookings(walk.id, signal), [walk.id])
  const bookings = useResource(request)
  const pets = useResource(listPets, isOpen)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [confirmation, setConfirmation] = useState<Booking>()
  const active = bookings.data?.filter(item => item.bookingStatus === 'JOINED') ?? []
  const availablePets = pets.data?.filter(pet => !active.some(booking => booking.petId === pet.id)) ?? []

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return
    const petIds = new FormData(event.currentTarget).getAll('pet').map(String)
    if (!petIds.length || petIds.length > walk.availableSpots) {
      setError('Elegí al menos una mascota, sin superar los lugares disponibles.')
      return
    }
    setPending(true)
    setError('')
    try {
      await reserveWalk(walk.id, petIds)
      onChanged(petIds.length === 1 ? 'Reserva confirmada.' : 'Reservas confirmadas.')
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos confirmar la reserva.')
    } finally { setPending(false) }
  }

  async function cancel(booking: Booking) {
    if (pending) return
    setPending(true)
    setError('')
    try {
      await cancelBooking(booking)
      onChanged(`Cancelaste la reserva de ${booking.petName}.`)
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos cancelar la reserva.')
    } finally { setPending(false) }
  }

  return <>
    <section className={`space-y-3 ${panel}`}>
      <h2 className="text-xl font-bold">Reservas de este paseo</h2>
      {(bookings.loading || pets.loading) && <p role="status">Consultando disponibilidad…</p>}
      {bookings.error && <div className={errorBox} role="alert">{bookings.error} <button className="underline" type="button" onClick={bookings.reload}>Reintentar reservas</button></div>}
      {pets.error && <div className={errorBox} role="alert">{pets.error} <button className="underline" type="button" onClick={pets.reload}>Reintentar mascotas</button></div>}
      {active.length > 0 && <ul className="space-y-2">
        {active.map(booking => <li key={booking.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-emerald-50 p-3">
          <span><strong>{booking.petName}</strong> · Reserva confirmada</span>
          {booking.canCancel && <button type="button" className={secondary} onClick={() => setConfirmation(booking)} disabled={pending}>Cancelar reserva</button>}
        </li>)}
      </ul>}
      {confirmation && <div className="rounded-lg bg-amber-50 p-3">
        <p className="mb-2 text-sm">¿Querés liberar el lugar de {confirmation.petName}?</p>
        <div className="flex flex-wrap gap-2">
          <button type="button" className={primary} onClick={() => void cancel(confirmation)} disabled={pending}>Confirmar cancelación</button>
          <button type="button" className={secondary} onClick={() => setConfirmation(undefined)} disabled={pending}>Conservar reserva</button>
        </div>
      </div>}
      {error && <p className={errorBox} role="alert">{error}</p>}
      {!isOpen && <p className="text-sm text-slate-600">Este paseo no admite nuevas reservas.</p>}
      {isOpen && bookings.data && pets.data && (availablePets.length
        ? <form onSubmit={submit} className="space-y-3">
          <fieldset disabled={pending || !!confirmation} className="space-y-2">
            <legend className="mb-2 font-semibold">¿Qué mascotas llevás?</legend>
            {availablePets.map(pet => <label key={pet.id} className="flex items-center gap-3 rounded-lg border border-slate-200 p-3 text-sm">
              <input className="size-4 accent-emerald-700" type="checkbox" name="pet" value={pet.id} />{pet.name}
            </label>)}
            <button className={primary} type="submit">{pending ? 'Guardando…' : 'Confirmar reserva'}</button>
          </fieldset>
          <p className="text-xs text-slate-600">Cada mascota ocupa un lugar. Podés cancelar hasta dos horas antes del inicio.</p>
        </form>
        : <div className="space-y-2 text-sm"><p>{pets.data.length ? 'Tus mascotas ya tienen reserva en este paseo.' : 'Primero registrá una mascota para poder reservar.'}</p>
          <Link to={pets.data.length ? paths.pets : paths.newPet} className="font-semibold text-emerald-800 underline">{pets.data.length ? 'Ver mis mascotas' : 'Agregar mascota'}</Link></div>)}
      <Link to={paths.bookings} className="inline-block text-sm font-semibold text-emerald-800 underline">Ver mis reservas</Link>
    </section>
    {walk.status === 'COMPLETED' && active.length > 0 && <WalkReviewForm walkId={walk.id} userId={userId} onChanged={() => onChanged('Tu reseña se publicó.')} />}
  </>
}
