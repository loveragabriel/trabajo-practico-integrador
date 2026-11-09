import { useId, useState, type SubmitEvent } from 'react'
import { useAuth } from '@/features/auth'
import { useZones, ZoneSelect, type Zone } from '@/features/map'
import { ApiRequestError } from '@/shared/api/client'
import { PageHeader } from '@/shared/components/PageHeader'
import { ROLE_LABELS } from '@/shared/types/user'
import { updateMyProfile } from '../api/profile'
import { CoverageEditor } from '../components/CoverageEditor'
import { useProfile } from '../hooks/useProfile'
import type { UserProfile } from '../types'

const inputClass =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100'
const labelClass = 'block text-sm font-medium text-slate-800'

export function ProfilePage() {
  const { session, signIn } = useAuth()
  const { profile, error, loading, reload } = useProfile()
  const zones = useZones(session?.token ?? null)

  return (
    <section className="mx-auto max-w-5xl">
      <PageHeader
        title="Mi perfil"
        description={profile?.role === 'WALKER' ? 'Tus datos, zonas de servicio y valores por defecto.' : 'Tus datos de contacto y zona.'}
      />

      {loading && <p role="status" className="mt-6 text-slate-600">Cargando tu perfil…</p>}
      {error && (
        <div role="alert" className="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {profile && (
        <div className={profile.role === 'WALKER' ? 'grid items-start gap-6 lg:grid-cols-2' : 'max-w-xl'}>
        <ProfileForm
          profile={profile}
          zones={zones.zones}
          zonesLoading={zones.loading}
          onSaved={(updated) => {
            if (session) signIn({ ...session, user: { ...session.user, name: updated.fullName } })
          }}
        />
        {profile.role === 'WALKER' && <CoverageEditor />}
        </div>
      )}
    </section>
  )
}

interface ProfileFormProps {
  profile: UserProfile
  zones: Zone[]
  zonesLoading: boolean
  onSaved: (profile: UserProfile) => void
}

function ProfileForm({ profile, zones, zonesLoading, onSaved }: ProfileFormProps) {
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
    <form onSubmit={handleSubmit} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="space-y-2">
        <span className={labelClass}>Email</span>
        <p className="text-slate-600">{profile.email}</p>
      </div>

      <div className="space-y-2">
        <span className={labelClass}>Tipo de cuenta</span>
        <p className="text-slate-600">{ROLE_LABELS[profile.role]}</p>
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

      <ZoneSelect zones={zones} value={zoneId} onChange={setZoneId} label="Barrio" disabled={zonesLoading} required />

      {error && <p className="text-sm text-red-600">{error}</p>}
      {success && <p className="text-sm text-emerald-700">Perfil actualizado.</p>}

      <button
        type="submit"
        disabled={submitting}
        className="w-full rounded-md bg-emerald-600 px-3 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
      >
        {submitting ? 'Guardando…' : 'Guardar cambios'}
      </button>
    </form>
  )
}
