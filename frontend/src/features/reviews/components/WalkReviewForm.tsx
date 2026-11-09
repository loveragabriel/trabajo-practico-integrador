import { useCallback, useState, type SubmitEvent } from 'react'
import { errorBox, input, panel, primary, successBox } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { createReview, getWalkReviews } from '../api/reviews'

export function WalkReviewForm({ walkId, userId, onChanged }: { walkId: string; userId: string; onChanged: () => void }) {
  const request = useCallback((signal: AbortSignal) => getWalkReviews(walkId, signal), [walkId])
  const { data, loading, error, reload } = useResource(request)
  const [pending, setPending] = useState(false)
  const [issue, setIssue] = useState('')
  const own = data?.find(review => review.authorId === userId)

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return
    const form = new FormData(event.currentTarget)
    setPending(true)
    setIssue('')
    try {
      await createReview(walkId, Number(form.get('rating')), String(form.get('comment') ?? ''))
      onChanged()
    } catch (failure) {
      setIssue(failure instanceof Error ? failure.message : 'No pudimos publicar tu reseña.')
    } finally { setPending(false) }
  }

  return <section className={panel}>
    <h2 className="mb-3 text-xl font-bold">Tu reseña</h2>
    {loading && <p role="status">Cargando reseña…</p>}
    {error && <div className={errorBox} role="alert">{error} <button type="button" className="underline" onClick={reload}>Reintentar</button></div>}
    {own && <div className={successBox}><p className="font-semibold">Ya calificaste este paseo: {own.rating} de 5.</p>{own.comment && <p className="mt-1 break-words">{own.comment}</p>}</div>}
    {data && !own && <form onSubmit={submit}>
      <fieldset disabled={pending} className="space-y-3">
        <label className="block text-sm font-medium">Puntuación<select name="rating" className={input} defaultValue="" required>
          <option value="" disabled>Elegí una puntuación</option>
          {[5, 4, 3, 2, 1].map(rating => <option key={rating} value={rating}>{rating} de 5</option>)}
        </select></label>
        <label className="block text-sm font-medium">Comentario (opcional)<textarea name="comment" className={input} maxLength={120} rows={3} /></label>
        <p className="text-xs text-slate-600">Hasta 120 caracteres. La puntuación y el comentario se muestran en el perfil del paseador.</p>
        {issue && <p className={errorBox} role="alert">{issue}</p>}
        <button className={primary} type="submit">{pending ? 'Publicando…' : 'Publicar reseña'}</button>
      </fieldset>
    </form>}
  </section>
}
