import { useCallback } from 'react'
import { useAuth } from '@/features/auth'
import { useWalkResource } from '@/features/walks/hooks/useWalkResource'
import { loadWalkReviews } from '../api/reviews'
import { WalkReviewForm } from './WalkReviewForm'

interface Props {
  walkId: string
  // true cuando el que mira es un dueño distinto del paseador y el paseo ya está finalizado
  canReview: boolean
}

export function WalkReviewsSection({ walkId, canReview }: Props) {
  const { session } = useAuth()
  const request = useCallback((signal: AbortSignal) => loadWalkReviews(walkId, signal), [walkId])
  const { data: reviews, loading, error, reload } =
    useWalkResource(`${session?.token ?? ''}:${walkId}:reviews`, true, request)
  const ownReview = reviews?.find((review) => review.authorId === session?.user.id)

  return (
    <div className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6">
      <h2 className="text-xl font-bold">Reseñas</h2>
      {loading && <p role="status" className="text-sm text-slate-600">Cargando reseñas…</p>}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {reviews && (
        reviews.length
          ? <ul className="space-y-3">
              {reviews.map((review) => (
                <li key={review.id} className="rounded-lg border border-slate-100 bg-slate-50 p-3 text-sm">
                  <p className="font-semibold">{review.rating} / 5</p>
                  {review.comment && <p className="mt-1 text-slate-700">{review.comment}</p>}
                </li>
              ))}
            </ul>
          : <p className="text-sm text-slate-600">Todavía no tiene reseñas.</p>
      )}
      {canReview && (
        ownReview
          ? <p className="text-sm text-emerald-700">Ya calificaste este paseo: {ownReview.rating} / 5.</p>
          : <WalkReviewForm walkId={walkId} onSubmitted={reload} />
      )}
    </div>
  )
}
