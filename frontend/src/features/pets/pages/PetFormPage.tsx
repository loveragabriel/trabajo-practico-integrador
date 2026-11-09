import { useId, useState, type SubmitEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { ApiRequestError } from '@/shared/api/client'
import { PageHeader } from '@/shared/components/PageHeader'
import { createPet, petSizeLabels, updatePet, type Pet, type PetSize } from '../api/pets'
import { usePet } from '../hooks/usePet'

const inputClass =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100'
const labelClass = 'block text-sm font-medium text-slate-800'

// alta y edición comparten formulario: si la URL trae petId es edición
export function PetFormPage() {
  const { petId } = useParams<{ petId: string }>()
  const { pet, error, loading, reload } = usePet(petId)

  return (
    <section className="mx-auto max-w-lg">
      <PageHeader title={petId ? 'Editar mascota' : 'Nueva mascota'} description="Agregá la información que el paseador necesita conocer." />

      {loading && <p role="status" className="mt-6 text-slate-600">Cargando mascota…</p>}
      {error && (
        <div role="alert" className="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {(!petId || pet) && <PetForm petId={petId} pet={pet} />}

      <Link to={paths.pets} className="mt-5 inline-block text-sm font-semibold text-blue-700 underline">Volver a Mis mascotas</Link>
    </section>
  )
}

interface PetFormProps {
  petId?: string
  pet?: Pet
}

function PetForm({ petId, pet }: PetFormProps) {
  const navigate = useNavigate()
  const nameId = useId()
  const breedId = useId()
  const sizeId = useId()
  const notesId = useId()

  const [name, setName] = useState(pet?.name ?? '')
  const [breed, setBreed] = useState(pet?.breed ?? '')
  const [size, setSize] = useState<PetSize | ''>(pet?.size ?? '')
  const [notes, setNotes] = useState(pet?.notes ?? '')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    setSubmitting(true)
    setError('')
    try {
      const payload = {
        name: name.trim(),
        breed: breed.trim() || undefined,
        size: size || undefined,
        notes: notes.trim() || undefined,
      }
      if (petId) await updatePet(petId, payload)
      else await createPet(payload)
      navigate(paths.pets)
    } catch (failure) {
      setError(
        failure instanceof ApiRequestError
          ? (failure.fieldErrors?.[0]?.message ?? failure.message)
          : 'No pudimos guardar la mascota.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-6 space-y-4">
      <fieldset disabled={submitting} className="space-y-4">
        <div className="space-y-2">
          <label htmlFor={nameId} className={labelClass}>Nombre</label>
          <input id={nameId} value={name} onChange={(event) => setName(event.target.value)} required maxLength={100} className={inputClass} />
        </div>

        <div className="space-y-2">
          <label htmlFor={breedId} className={labelClass}>Raza (opcional)</label>
          <input id={breedId} value={breed} onChange={(event) => setBreed(event.target.value)} maxLength={100} className={inputClass} />
        </div>

        <div className="space-y-2">
          <label htmlFor={sizeId} className={labelClass}>Tamaño (opcional)</label>
          <select id={sizeId} value={size} onChange={(event) => setSize(event.target.value as PetSize | '')} className={inputClass}>
            <option value="">Sin especificar</option>
            {Object.entries(petSizeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>

        <div className="space-y-2">
          <label htmlFor={notesId} className={labelClass}>Observaciones (opcional)</label>
          <textarea id={notesId} value={notes} onChange={(event) => setNotes(event.target.value)} maxLength={1000} rows={3} className={inputClass} />
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button type="submit" disabled={submitting} className="w-full rounded-md bg-emerald-600 px-3 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60">
          {submitting ? 'Guardando…' : 'Guardar mascota'}
        </button>
      </fieldset>
    </form>
  )
}
