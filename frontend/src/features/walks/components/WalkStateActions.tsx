import { useState } from 'react'
import { apiClient } from '@/shared/api/client'
import { errorBox, panel, primary, secondary } from '@/shared/components/formStyles'
import type { WalkDetails } from '../api/walkReadContract'

type Action = 'cancel' | 'start' | 'complete'
const labels = { cancel: 'Cancelar paseo', start: 'Marcar en curso', complete: 'Marcar finalizado' }
const messages = { cancel: 'Paseo cancelado.', start: 'El paseo quedó en curso.', complete: 'El paseo quedó finalizado.' }

export function WalkStateActions({ walk, onChanged }: { walk: WalkDetails; onChanged: (message: string) => void }) {
  const [confirm, setConfirm] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [now] = useState(Date.now)
  const start = Date.parse(`${walk.startTime}-03:00`)
  const end = Date.parse(`${walk.endTime}-03:00`)
  const scheduled = walk.status === 'PUBLISHED' || walk.status === 'FULL'
  const action: Action | null = scheduled && now < start ? 'cancel'
    : scheduled && now < end ? 'start'
      : (scheduled || walk.status === 'IN_PROGRESS') && now >= end ? 'complete' : null

  async function run(operation: Action) {
    if (pending) return
    setPending(true)
    setError('')
    try {
      await apiClient.post(`/walks/${walk.id}/${operation}`)
      onChanged(messages[operation])
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos cambiar el estado del paseo.')
    } finally { setPending(false) }
  }

  if (!action) return null
  return <section className={`space-y-3 ${panel}`}>
    <h2 className="text-xl font-bold">Gestionar paseo</h2>
    {error && <p className={errorBox} role="alert">{error}</p>}
    {confirm ? <div className="space-y-2">
      <p className="text-sm">Al cancelar el paseo también se cancelan las reservas de las mascotas.</p>
      <div className="flex flex-wrap gap-2">
        <button className={primary} type="button" disabled={pending} onClick={() => void run('cancel')}>Confirmar cancelación</button>
        <button className={secondary} type="button" disabled={pending} onClick={() => setConfirm(false)}>Conservar paseo</button>
      </div>
    </div> : <button className={action === 'cancel' ? secondary : primary} type="button" disabled={pending}
      onClick={() => action === 'cancel' ? setConfirm(true) : void run(action)}>{pending ? 'Guardando…' : labels[action]}</button>}
  </section>
}
