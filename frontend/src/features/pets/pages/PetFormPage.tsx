import { useId, useState, type SubmitEvent } from 'react'
import { generatePath, Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { ApiRequestError } from '@/shared/api/client'
import { Icon } from '@/shared/components/Icon'
import { TextField } from '@/shared/components/TextField'
import { createPet, updatePet, type Pet, type PetSize } from '../api/pets'
import { usePet } from '../hooks/usePet'

export function PetFormPage() {
  const { petId } = useParams<{ petId: string }>()
  const [params] = useSearchParams()
  const { state } = useLocation()
  const { pet, error, loading, reload } = usePet(petId)
  const welcome = !petId && params.get('bienvenida') === '1'
  const walkId = (state as { walkId?: string } | null)?.walkId
  const destination = walkId
    ? generatePath(paths.walkDetail, { walkId })
    : welcome ? paths.walks : paths.pets

  return (
    <section className="mx-auto max-w-md space-y-5 pb-4">
      {!welcome && (
        <Link to={destination} className="inline-flex items-center gap-1 text-label-md font-semibold text-primary">
          <Icon name="arrow_back" className="text-lg" /> {walkId ? 'Volver al paseo' : 'Mis mascotas'}
        </Link>
      )}
      <header>
        {welcome && <p className="mb-2 text-label-sm font-semibold text-primary">Paso 2 de 2 · Perfil de tu mascota</p>}
        <h1 className="text-headline-md font-bold text-on-surface">{petId ? 'Editar mascota' : welcome ? 'Sumá a tu compañero peludo' : 'Registrar mascota'}</h1>
        <p className="mt-1 text-body-md text-on-surface-variant">Completá los datos que el paseador necesita conocer para cuidarla durante la salida.</p>
      </header>
      {loading && <p role="status" className="text-body-sm text-on-surface-variant">Cargando mascota…</p>}
      {error && (
        <div role="alert" className="rounded-2xl bg-error-container p-4 text-on-error-container">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {(!petId || pet) && <PetForm key={petId ?? 'new'} pet={pet} destination={destination} />}
      {welcome && (
        <Link to={paths.walks} replace className="block py-2 text-center text-label-md font-semibold text-primary">Hacerlo más tarde</Link>
      )}
    </section>
  )
}

const sizes: { value: PetSize; label: string; hint: string; icon: string }[] = [
  { value: 'SMALL', label: 'Chico', hint: 'Hasta 10 kg', icon: 'pets' },
  { value: 'MEDIUM', label: 'Mediano', hint: '10 a 25 kg', icon: 'pets' },
  { value: 'LARGE', label: 'Grande', hint: 'Más de 25 kg', icon: 'pets' },
]

function PetForm({ pet, destination }: { pet?: Pet; destination: string }) {
  const navigate = useNavigate()
  const nameId = useId()
  const breedId = useId()
  const notesId = useId()
  const [name, setName] = useState(pet?.name ?? '')
  const [breed, setBreed] = useState(pet?.breed ?? '')
  const [size, setSize] = useState<PetSize | ''>(pet?.size ?? '')
  const [notes, setNotes] = useState(pet?.notes ?? '')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting || !name.trim()) return
    setSubmitting(true)
    setError('')
    try {
      const payload = {
        name: name.trim(),
        breed: breed.trim() || undefined,
        size: size || undefined,
        notes: notes.trim() || undefined,
      }
      if (pet) await updatePet(pet.id, payload)
      else await createPet(payload)
      navigate(destination, { replace: true })
    } catch (failure) {
      setError(failure instanceof ApiRequestError
        ? (failure.fieldErrors?.[0]?.message ?? failure.message) : 'No pudimos guardar la mascota.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <fieldset disabled={submitting} className="space-y-4">
        <div className="flex justify-center">
          <div className="grid size-22 place-items-center rounded-full bg-primary-fixed text-primary shadow-sm">
            <Icon name="pets" filled className="text-[38px]" />
          </div>
        </div>
        <section className="space-y-4 rounded-2xl bg-surface-container-lowest p-5 shadow-sm">
          <h2 className="flex items-center gap-2 text-title-sm font-semibold text-primary"><Icon name="pets" className="text-xl" /> Datos básicos</h2>
          <TextField id={nameId} label="Nombre de la mascota" icon="pets" value={name} required maxLength={100} placeholder="Ej.: Rocky" onChange={(event) => setName(event.target.value)} />
          <TextField id={breedId} label="Raza o cruza (opcional)" icon="search" value={breed} maxLength={100} placeholder="Ej.: mestizo" onChange={(event) => setBreed(event.target.value)} />
        </section>
        <section className="rounded-2xl bg-surface-container-lowest p-5 shadow-sm">
          <h2 id="pet-size-label" className="flex items-center gap-2 text-title-sm font-semibold text-primary"><Icon name="straighten" className="text-xl" /> Tamaño</h2>
          <p className="mt-1 text-body-sm text-on-surface-variant">Ayuda al paseador a organizar el grupo.</p>
          <div role="group" aria-labelledby="pet-size-label" className="mt-3 grid grid-cols-3 gap-2">
            {sizes.map(({ value, label, hint, icon }) => (
              <button key={value} type="button" aria-pressed={size === value} onClick={() => setSize(size === value ? '' : value)}
                className={`flex min-w-0 flex-col items-center gap-1 rounded-xl px-1 py-3 text-center ${size === value ? 'bg-primary-fixed text-primary ring-2 ring-primary' : 'bg-surface-container-low text-on-surface-variant'}`}>
                <Icon name={icon} className="text-lg" />
                <span className="text-label-md font-semibold">{label}</span>
                <span className="text-label-sm">{hint}</span>
              </button>
            ))}
          </div>
        </section>
        <section className="rounded-2xl bg-secondary-fixed/45 p-5 text-on-secondary-fixed-variant">
          <h2 className="flex items-center gap-2 text-title-sm font-semibold"><Icon name="info" className="text-xl" /> Notas para el paseador</h2>
          <label htmlFor={notesId} className="mt-3 block text-label-md font-semibold">Cuidados específicos (opcional)</label>
          <textarea id={notesId} value={notes} onChange={(event) => setNotes(event.target.value)} maxLength={1000} rows={3}
            placeholder="Ej.: tira de la correa cuando ve palomas."
            className="mt-2 w-full rounded-xl bg-surface-container-lowest p-3 text-body-md text-on-surface outline-offset-2 focus-visible:outline-primary" />
        </section>
        {error && <p role="alert" className="rounded-xl bg-error-container p-3 text-body-sm text-on-error-container">{error}</p>}
        <button type="submit" disabled={submitting || !name.trim()} className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container px-4 text-label-lg font-semibold text-on-primary shadow-sm disabled:opacity-60">
          <Icon name="pets" className="text-xl" />
          {submitting ? 'Guardando…' : pet ? 'Guardar cambios' : 'Guardar mascota'}
        </button>
      </fieldset>
    </form>
  )
}
