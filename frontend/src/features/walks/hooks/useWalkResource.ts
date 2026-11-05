import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError } from '@/shared/api/client'

interface ResourceState<T> { key: string; attempt: number; data?: T; error?: string }

export function useWalkResource<T>(key: string, enabled: boolean, request: (signal: AbortSignal) => Promise<T>) {
  const [state, setState] = useState<ResourceState<T>>({ key: '', attempt: -1 })
  const [attempt, setAttempt] = useState(0)
  const reload = useCallback(() => setAttempt(value => value + 1), [])

  useEffect(() => {
    if (!enabled) return
    const controller = new AbortController()
    let active = true
    Promise.resolve().then(() => request(controller.signal)).then(data => {
      if (active) setState({ key, attempt, data })
    }).catch((error: unknown) => {
      if (!active || controller.signal.aborted) return
      const message = error instanceof ApiRequestError
        ? error.status === 404 ? 'Ese paseo no está disponible o no podés consultar su historial.'
          : error.status === 403 ? 'Tu cuenta no tiene permiso para consultar estos paseos.'
          : error.status === 401 ? 'Ingresá nuevamente para consultar tus paseos.'
          : 'No pudimos cargar los paseos. Intentá nuevamente.'
        : error instanceof Error ? error.message : 'No pudimos cargar los paseos. Intentá nuevamente.'
      setState({ key, attempt, error: message })
    })
    return () => { active = false; controller.abort() }
  }, [key, enabled, request, attempt])

  const current = enabled && state.key === key && state.attempt === attempt ? state : undefined
  return { data: current?.data, error: current?.error, loading: enabled && !current, reload }
}
