import { useRef, useState } from 'react'
import { Icon } from '@/shared/components/Icon'
import { updateWalkStatus, walkActionError } from '../api/walks'
import type { WalkStatus } from '../api/walkReadContract'

type ForcedStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
type PrimaryAction = 'IN_PROGRESS' | 'COMPLETED'

// RN-29: transiciones válidas que el paseador puede forzar manualmente.
// PUBLISHED <-> FULL no está acá: es automático (RN-30).
const nextStatuses: Record<WalkStatus, ForcedStatus[]> = {
  PUBLISHED: ['IN_PROGRESS', 'CANCELLED'],
  FULL: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

const primaryIcon: Record<PrimaryAction, string> = { IN_PROGRESS: 'play_circle', COMPLETED: 'task_alt' }

function primaryLabel(action: PrimaryAction, bookedSpots: number) {
  if (action === 'COMPLETED') return 'Marcar como finalizado'
  return `Iniciar paseo (${bookedSpots} ${bookedSpots === 1 ? 'perro listo' : 'perros listos'})`
}

interface Props {
  walkId: string
  status: WalkStatus
  bookedSpots: number
  onChanged: () => void
}

export function WalkStateActions({ walkId, status, bookedSpots, onChanged }: Props) {
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
  const primary = options.find((option): option is PrimaryAction => option !== 'CANCELLED')
  const canCancel = options.includes('CANCELLED')
  if (!options.length) return null

  return (
    <div className="fixed inset-x-0 bottom-16 z-40 bg-surface/95 pb-3 pt-3 shadow-[0_-8px_24px_rgba(47,93,80,0.08)] backdrop-blur-md md:bottom-0 md:pb-safe">
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-2 px-5">
        {error && <p role="alert" className="w-full rounded-lg bg-error-container p-3 text-body-sm text-on-error-container">{error}</p>}
        {primary && (
          <button type="button" disabled={submitting} onClick={() => changeTo(primary)}
            className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container text-label-lg font-semibold text-on-primary shadow-md transition-all active:scale-[0.98] disabled:opacity-60">
            <Icon name={primaryIcon[primary]} filled className="text-[20px]" />
            {submitting ? 'Guardando…' : primaryLabel(primary, bookedSpots)}
          </button>
        )}
        {canCancel && (
          <button type="button" disabled={submitting} onClick={() => changeTo('CANCELLED')}
            className="py-1 text-label-md text-tertiary transition-colors hover:text-tertiary-container">
            Cancelar esta salida
          </button>
        )}
      </div>
    </div>
  )
}
