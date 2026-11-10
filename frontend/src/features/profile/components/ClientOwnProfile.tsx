import { useId, useState, type SubmitEvent } from 'react'
import { useZones, ZoneSelect } from '@/features/map'
import { ApiRequestError } from '@/shared/api/client'
import { updateMyProfile } from '../api/profile'
import type { UserProfile } from '../types'

// TODO: todavía no tiene mockup propio (a diferencia de WalkerOwnProfile) — sigue con el
// estilo viejo hasta que le llegue su turno en el rediseño
const inputClass =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100'
const labelClass = 'block text-sm font-medium text-slate-800'

export function ClientOwnProfile({ profile, onSaved }: { profile: UserProfile; onSaved: (profile: UserProfile) => void }) {
  const zones = useZones()
  const fullNameId = useId()
  const phoneId = useId()

  const [fullName, setFullName] = useState(profile.fullName)
  const [phone, setPhone] = useState(profile.phone ?? '')
  const [zoneId, setZoneId] = useState(profile.zoneId)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSuccess(false)
    setSubmitting(true)
    try {
      const updated = await updateMyProfile({ fullName, phone: phone || undefined, zoneId })
      onSaved(updated)
      setSuccess(true)
    } catch (err) {
      setError(
        err instanceof ApiRequestError && err.status === 404
          ? 'El barrio seleccionado ya no existe. Elegí otro.'
          : 'No pudimos guardar los cambios. Revisá los datos e intentá nuevamente.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto mt-6 max-w-sm space-y-4">
      <fieldset disabled={submitting} className="space-y-4">
        <div className="space-y-2">
          <span className={labelClass}>Email</span>
          <p className="text-slate-600">{profile.email}</p>
        </div>

        <div className="space-y-2">
          <label htmlFor={fullNameId} className={labelClass}>Nombre completo</label>
          <input
            id={fullNameId}
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            required
            autoComplete="name"
            className={inputClass}
          />
        </div>

        <div className="space-y-2">
          <label htmlFor={phoneId} className={labelClass}>Teléfono (opcional)</label>
          <input
            id={phoneId}
            type="tel"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            autoComplete="tel"
            className={inputClass}
          />
        </div>

        <ZoneSelect zones={zones.zones} value={zoneId} onChange={setZoneId} label="Barrio" disabled={zones.loading} required />

        {error && <p className="text-sm text-red-600">{error}</p>}
        {success && <p className="text-sm text-emerald-700">Perfil actualizado.</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-emerald-600 px-3 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
        >
          {submitting ? 'Guardando…' : 'Guardar cambios'}
        </button>
      </fieldset>
    </form>
  )
}
