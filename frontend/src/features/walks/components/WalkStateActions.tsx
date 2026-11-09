import { useRef, useState } from 'react'
import { updateWalkStatus, walkActionError } from '../api/walks'
import type { WalkStatus } from '../api/walkReadContract'

type ForcedStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

// RN-29: transiciones válidas que el paseador puede forzar manualmente.
// PUBLISHED <-> FULL no está acá: es automático (RN-30).
const nextStatuses: Record<WalkStatus, ForcedStatus[]> = {
  PUBLISHED: ['IN_PROGRESS', 'CANCELLED'],
  FULL: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

const actionLabels: Record<ForcedStatus, string> = {
  IN_PROGRESS: 'Iniciar paseo',
  COMPLETED: 'Marcar como finalizado',
  CANCELLED: 'Cancelar paseo',
}

interface Props {
  walkId: string
  status: WalkStatus
  onChanged: () => void
}

export function WalkStateActions({ walkId, status, onChanged }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const posting = useRef(false)

  async function changeTo(next: ForcedStatus) {
    if (posting.current) return
    if (next === 'CANCELLED' && !window.confirm('¿Cancelar este paseo? Se cancelan también las reservas activas.')) return

    posting.current = true
    setSubmitting(true)
    setError(null)
    try {
      await updateWalkStatus(walkId, next)
      onChanged()
    } catch (failure) {
      setError(walkActionError(failure))
    } finally {
      posting.current = false
      setSubmitting(false)
    }
  }

  const options = nextStatuses[status]
  if (!options.length) return null

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 font-bold">Gestionar paseo</h2>
      <div className="flex flex-wrap gap-2">
        {options.map((next) => (
          <button key={next} type="button" disabled={submitting} onClick={() => changeTo(next)}
            className="rounded-lg bg-blue-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
            {submitting ? 'Guardando…' : actionLabels[next]}
          </button>
        ))}
      </div>
      {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
    </div>
  )
}
