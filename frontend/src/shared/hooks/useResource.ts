import { useCallback, useEffect, useState } from 'react'

export function useResource<T>(request: (signal: AbortSignal) => Promise<T>, enabled = true) {
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<{ attempt: number; request: typeof request; data?: T; error?: string }>()
  const reload = useCallback(() => setAttempt(value => value + 1), [])

  useEffect(() => {
    if (!enabled) return
    const controller = new AbortController()
    request(controller.signal).then(data => {
      if (!controller.signal.aborted) setResult({ attempt, request, data })
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setResult({ attempt, request, error: error instanceof Error ? error.message : 'No pudimos cargar los datos.' })
    })
    return () => controller.abort()
  }, [request, enabled, attempt])

  const current = enabled && result?.attempt === attempt && result.request === request ? result : undefined
  return { ...current, loading: enabled && !current, reload }
}
