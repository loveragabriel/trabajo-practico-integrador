import { useState, type FormEvent } from 'react'
import { createReview, reviewError } from '../api/reviews'

interface Props {
  walkId: string
  onSubmitted: () => void
}

export function WalkReviewForm({ walkId, onSubmitted }: Props) {
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await createReview(walkId, { rating, comment: comment.trim() || undefined })
      onSubmitted()
    } catch (failure) {
      setError(reviewError(failure))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
      <h2 className="font-bold">Calificar este paseo</h2>
      <label className="block text-sm font-medium">
        Puntuación
        <select value={rating} onChange={(event) => setRating(Number(event.target.value))}
          className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2">
          {[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} {value === 1 ? 'estrella' : 'estrellas'}</option>)}
        </select>
      </label>
      <label className="block text-sm font-medium">
        Comentario (opcional)
        <textarea value={comment} onChange={(event) => setComment(event.target.value)} maxLength={120} rows={3}
          className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2" />
      </label>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      <button type="submit" disabled={submitting}
        className="rounded-lg bg-emerald-700 px-4 py-2 font-semibold text-white disabled:opacity-50">
        {submitting ? 'Enviando…' : 'Enviar calificación'}
      </button>
    </form>
  )
}
