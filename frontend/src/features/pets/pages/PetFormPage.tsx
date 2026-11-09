import { useCallback, useState, type SubmitEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { PageHeader } from '@/shared/components/PageHeader'
import { errorBox, input, panel, primary } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { getPet, petSizeLabels, savePet, type Pet, type PetSize } from '../api/pets'

export function PetFormPage() {
  const { petId } = useParams<{ petId: string }>()
  const request = useCallback((signal: AbortSignal) => getPet(petId ?? '', signal), [petId])
  const { data, error, loading, reload } = useResource(request, !!petId)

  return <section className="mx-auto max-w-xl">
    <PageHeader title={petId ? 'Editar mascota' : 'Nueva mascota'} description="Agregá la información que el paseador necesita conocer." />
    {loading && <p role="status">Cargando mascota…</p>}
    {error && <div role="alert" className={errorBox}>{error} <button type="button" onClick={reload} className="underline">Reintentar</button></div>}
    {(!petId || data) && <PetForm key={petId ?? 'new'} pet={data} />}
    <Link to={paths.pets} className="mt-5 inline-block text-sm font-semibold text-emerald-800 underline">Volver a Mis mascotas</Link>
  </section>
}

function PetForm({ pet }: { pet?: Pet }) {
  const navigate = useNavigate()
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return
    const form = new FormData(event.currentTarget)
    const value = (name: string) => String(form.get(name) ?? '').trim()
    if (!value('name')) { setError('Ingresá el nombre de tu mascota.'); return }
    setPending(true)
    setError('')
    try {
      await savePet({ name: value('name'), breed: value('breed') || null,
        size: (value('size') || null) as PetSize | null, notes: value('notes') || null }, pet?.id)
      navigate(paths.pets)
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos guardar la mascota.')
    } finally { setPending(false) }
  }

  return <form onSubmit={submit} className={panel}>
    <fieldset disabled={pending} className="space-y-4">
      <label className="block text-sm font-medium">Nombre<input name="name" required maxLength={100} defaultValue={pet?.name} className={input} /></label>
      <label className="block text-sm font-medium">Raza (opcional)<input name="breed" maxLength={100} defaultValue={pet?.breed ?? ''} className={input} /></label>
      <label className="block text-sm font-medium">Tamaño (opcional)<select name="size" defaultValue={pet?.size ?? ''} className={input}>
        <option value="">Sin especificar</option>{Object.entries(petSizeLabels).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
      </select></label>
      <label className="block text-sm font-medium">Observaciones (opcional)<textarea name="notes" maxLength={1000} rows={3} defaultValue={pet?.notes ?? ''} className={input} /></label>
      {error && <p role="alert" className={errorBox}>{error}</p>}
      <button type="submit" className={primary}>{pending ? 'Guardando…' : 'Guardar mascota'}</button>
    </fieldset>
  </form>
}
